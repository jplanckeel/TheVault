package com.thevault.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.thevault.data.remote.ItemSyncRemoteDataSource
import com.thevault.data.remote.PriceRemoteDataSource
import com.thevault.data.remote.dto.PriceSnapshotDto
import com.thevault.data.remote.dto.QuoteDto
import com.thevault.data.repository.AchievementRepositoryImpl
import com.thevault.data.repository.CatalogSyncRepositoryImpl
import com.thevault.data.repository.PriceRepositoryImpl
import com.thevault.data.repository.SettingsRepositoryImpl
import com.thevault.data.repository.ValueHistoryRepositoryImpl
import com.thevault.database.TheVaultDatabase
import com.thevault.domain.model.AchievementUnlock
import com.thevault.domain.model.PriceGrade
import com.thevault.domain.model.PriceSource
import com.thevault.domain.model.SyncOutcome
import com.thevault.domain.model.SyncPayload
import com.thevault.domain.model.ValueSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** Local storage of quotes, value history, achievements, settings and the sync queue. */
class InsightsStorageTest {
    private fun db() = TheVaultDatabase(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { TheVaultDatabase.Schema.create(it) })

    private val fixed =
        object : Clock {
            override fun now() = Instant.parse("2026-11-14T10:00:00Z")
        }

    private val remote =
        object : PriceRemoteDataSource {
            override suspend fun quotes(externalIds: List<String>) =
                listOf(
                    QuoteDto("g", "cib", 20.0, 40.0, 30.0, 6, "2026-11-13T00:00:00Z", 25.0),
                    QuoteDto("g", "loose", 5.0, 12.0, 9.0, 4, "2026-11-13T00:00:00Z", null),
                    QuoteDto("g", "graded", 1.0, 1.0, 1.0, 1, "2026-11-13T00:00:00Z"),
                    QuoteDto("h", "new", 1.0, 1.0, 1.0, 1, "not a date"),
                )

            override suspend fun history(externalId: String) =
                listOf(
                    PriceSnapshotDto("cib", 30.0, "2026-11-13T00:00:00Z"),
                    PriceSnapshotDto("cib", 28.0, "2026-10-13T00:00:00Z"),
                    PriceSnapshotDto("??", 1.0, "2026-10-13T00:00:00Z"),
                )
        }

    @Test
    fun quotesAreCachedAndRecordedAsPalSnapshots() =
        runTest {
            val repo = PriceRepositoryImpl(db(), remote, fixed)
            assertEquals(2, repo.refreshQuotes(listOf("g", "h")))
            val quotes = repo.observeQuotes().first().getValue("g")
            assertEquals(30.0, quotes.single { it.grade == PriceGrade.CIB }.median)
            assertEquals(25.0, quotes.single { it.grade == PriceGrade.CIB }.medianPast)
            val snap =
                repo
                    .observeSnapshots()
                    .first()
                    .getValue("g")
                    .single()
            assertEquals(PriceSource.EBAY_PAL, snap.source)
            assertEquals(LocalDate(2026, 11, 14), snap.day)
            assertEquals(9.0, snap.loose)
            assertEquals(listOf(28.0, 30.0), repo.history("g").map { it.median })
        }

    @Test
    fun valueHistoryKeepsOneSnapshotPerDay() =
        runTest {
            val repo = ValueHistoryRepositoryImpl(db())
            repo.record(ValueSnapshot(LocalDate(2026, 11, 14), 100.0, 50.0))
            repo.record(ValueSnapshot(LocalDate(2026, 11, 14), 120.0, 50.0))
            repo.record(ValueSnapshot(LocalDate(2026, 11, 13), 90.0, 50.0))
            assertEquals(listOf(90.0, 120.0), repo.observe().first().map { it.value })
        }

    @Test
    fun achievementsAreRecordedOnceAndMarkedSeen() =
        runTest {
            val repo = AchievementRepositoryImpl(db())
            val at = Instant.parse("2026-11-14T10:00:00Z")
            repo.record(listOf(AchievementUnlock("a", at, false)))
            repo.record(listOf(AchievementUnlock("a", at, true), AchievementUnlock("b", at, true)))
            repo.markSeen("a")
            assertEquals(listOf(true, true), repo.observe().first().map { it.seen })
        }

    @Test
    fun settingsStoreValues() =
        runTest {
            val repo = SettingsRepositoryImpl(db())
            assertNull(repo.get("k"))
            repo.set("k", "1")
            repo.set("k", "2")
            assertEquals("2", repo.get("k"))
            assertEquals("2", repo.observe("k").first())
        }

    @Test
    fun syncQueueTracksPushedItems() =
        runTest {
            val outcomes = mutableListOf(SyncOutcome.Failed, SyncOutcome.Synced, SyncOutcome.Rejected)
            val remote =
                object : ItemSyncRemoteDataSource {
                    override suspend fun push(payload: SyncPayload) = outcomes.removeAt(0)
                }
            val repo = CatalogSyncRepositoryImpl(db(), remote, fixed)

            fun p(id: String) = SyncPayload(id, "game", "T", "P", "PAL", "CIB", "OWNED")
            assertEquals(SyncOutcome.Failed, repo.push(p("a")))
            assertEquals(SyncOutcome.Synced, repo.push(p("b")))
            assertEquals(SyncOutcome.Rejected, repo.push(p("c")))
            assertEquals(setOf("b", "c"), repo.observeSyncedIds().first())
            repo.markDirty("b")
            assertTrue("b" !in repo.observeSyncedIds().first())
        }
}
