package com.thevault.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.thevault.data.repository.AccessoryRepositoryImpl
import com.thevault.data.repository.ConsoleRepositoryImpl
import com.thevault.data.repository.GameRepositoryImpl
import com.thevault.database.TheVaultDatabase
import com.thevault.domain.model.Accessory
import com.thevault.domain.model.Components
import com.thevault.domain.model.Console
import com.thevault.domain.model.CopyDetails
import com.thevault.domain.model.Edition
import com.thevault.domain.model.Game
import com.thevault.domain.model.Grade
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Round trips through SQLite: every copy detail and the tags survive a save. */
class RepositoryTest {
    private fun database() = TheVaultDatabase(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { TheVaultDatabase.Schema.create(it) })

    private val details =
        CopyDetails(
            components = Components(box = true, manual = false, extras = true, mediaGrade = Grade.VERY_GOOD, boxGrade = Grade.FAIR),
            regionVariant = "FAH",
            edition = Edition.BUDGET,
            barcode = "045496390031",
            productCode = "DOL-GZLP-FAH",
            shippingCost = 4.5,
            acquiredFrom = "Leboncoin",
            location = "Étagère 2",
            tags = listOf("zelda", "à revendre"),
            loanedTo = "Paul",
            loanDueAt = LocalDate(2026, 12, 1),
            soldPrice = null,
            soldAt = null,
        )

    @Test
    fun gameRoundTripKeepsDetailsAndTags() =
        runTest {
            val repo = GameRepositoryImpl(database())
            val game = Game("g", "Zelda", "GameCube", "Action", 2003, Region.PAL, details, status = ItemStatus.LOANED, purchasePrice = 30.0)
            repo.save(game)
            assertEquals(game, repo.getById("g"))
            assertEquals(listOf(game), repo.observeAll().first())
            repo.save(game.copy(details = details.copy(tags = listOf("zelda"))))
            assertEquals(listOf("zelda"), repo.getById("g")?.details?.tags)
            repo.delete("g")
            assertNull(repo.getById("g"))
        }

    @Test
    fun consolesAndAccessoriesArePersisted() =
        runTest {
            val db = database()
            val console = Console("c", "GameCube", "Nintendo", Region.PAL, "Indigo", details, 2001, ItemStatus.OWNED)
            val accessory = Accessory("a", "Manette", "Controller", "GameCube", details, ItemStatus.SOLD)
            ConsoleRepositoryImpl(db).save(console)
            AccessoryRepositoryImpl(db).save(accessory)
            assertEquals(console, ConsoleRepositoryImpl(db).getById("c"))
            assertEquals(accessory, AccessoryRepositoryImpl(db).observeAll().first().single())
        }
}

/** Price snapshots: PAL history and imported US prices. */
class PriceSnapshotTest {
    @Test
    fun importedPricesAreReplacedAndRead() =
        kotlinx.coroutines.test.runTest {
            val db = TheVaultDatabase(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { TheVaultDatabase.Schema.create(it) })
            val repo =
                com.thevault.data.repository
                    .PriceRepositoryImpl(db, NoRemote)
            val record =
                com.thevault.domain.usecase
                    .RecordImportedPricesUseCase(repo)
            record(
                mapOf(
                    "a" to
                        com.thevault.domain.usecase
                            .UsPrices(1.0, 2.0, 3.0),
                    "b" to
                        com.thevault.domain.usecase
                            .UsPrices(null, 5.0, null),
                ),
            )
            record(
                mapOf(
                    "a" to
                        com.thevault.domain.usecase
                            .UsPrices(4.0, 5.0, 6.0),
                ),
            )
            val prices =
                com.thevault.domain.usecase
                    .GetImportedPricesUseCase(repo)()
            assertEquals(
                mapOf(
                    "a" to
                        com.thevault.domain.usecase
                            .UsPrices(4.0, 5.0, 6.0),
                ),
                prices,
            )
        }

    @Test
    fun palSnapshotPoolsTheGradesOfAnItem() {
        val at = kotlin.time.Instant.fromEpochSeconds(0)
        val q =
            listOf(
                com.thevault.domain.model
                    .PriceQuote("a", com.thevault.domain.model.PriceGrade.LOOSE, 10.0, 8.0, 12.0, 4, at),
                com.thevault.domain.model
                    .PriceQuote("a", com.thevault.domain.model.PriceGrade.CIB, 25.0, 20.0, 30.0, 6, at),
            )
        val s =
            com.thevault.data.repository
                .palSnapshot("a", LocalDate(2026, 11, 14), q)
        assertEquals(10.0, s.loose)
        assertEquals(25.0, s.cib)
        assertNull(s.new)
        assertEquals(10, s.sampleSize)
    }

    private object NoRemote : com.thevault.data.remote.PriceRemoteDataSource {
        override suspend fun quotes(externalIds: List<String>) = emptyList<com.thevault.data.remote.dto.QuoteDto>()

        override suspend fun history(externalId: String) = emptyList<com.thevault.data.remote.dto.PriceSnapshotDto>()
    }
}
