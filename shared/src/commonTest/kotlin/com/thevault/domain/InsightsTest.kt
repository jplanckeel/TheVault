package com.thevault.domain

import com.thevault.domain.model.ItemCondition
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.PriceGrade
import com.thevault.domain.model.PricePoint
import com.thevault.domain.model.PriceQuote
import com.thevault.domain.model.Region
import com.thevault.domain.model.priceGrade
import com.thevault.domain.repository.PriceRepository
import com.thevault.domain.repository.SettingsRepository
import com.thevault.domain.usecase.CsvRow
import com.thevault.domain.usecase.ExistingIds
import com.thevault.domain.usecase.RefreshQuotesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class InsightsTest {
    @Test
    fun mapsConditionsToPriceGrades() {
        assertEquals(PriceGrade.LOOSE, ItemCondition.LOOSE.priceGrade)
        assertEquals(PriceGrade.CIB, ItemCondition.CIB_NO_MANUAL.priceGrade)
        assertEquals(PriceGrade.NEW, ItemCondition.NEW_SEALED.priceGrade)
        assertNull(ItemCondition.GOOD.priceGrade)
        assertEquals(PriceGrade.CIB, PriceGrade.fromApi("CIB"))
        assertNull(PriceGrade.fromApi("graded"))
    }

    @Test
    fun quoteNeedsEnoughListings() {
        val q = PriceQuote("a", PriceGrade.CIB, 100.0, 80.0, 120.0, 2, Instant.fromEpochSeconds(0))
        assertFalse(q.isReliable)
        assertTrue(q.copy(samples = 3).isReliable)
    }

    private fun row(title: String = "Zelda") =
        CsvRow(
            "SNES",
            "Games",
            title,
            "",
            "",
            "",
            LocalDate(2021, 3, 24),
            "",
            "",
            null,
            ItemStatus.OWNED,
            ItemCondition.CIB,
            Region.PAL,
            null,
            30.0,
        )

    @Test
    fun csvIdsAreStableAndCopiesDistinct() {
        val first = ExistingIds(emptyList(), emptyList(), emptyList())
        val again = ExistingIds(emptyList(), emptyList(), emptyList())
        val id1 = first.newId("game", row())
        val id2 = first.newId("game", row())
        assertNotEquals(id1, id2)
        assertEquals(id1, again.newId("game", row()))
    }

    @Test
    fun csvReimportReusesExistingItemAndKeepsCover() {
        val existing = row().newGame("legacy-1").copy(coverUrl = "https://c", releaseYear = 1991)
        val ids = ExistingIds(listOf(existing), emptyList(), emptyList())
        assertEquals("legacy-1", ids.take("game", row()))
        assertNull(ids.take("game", row()))
        val merged = row().copy(price = 45.0).toGame(existing)
        assertEquals("https://c", merged.coverUrl)
        assertEquals(1991, merged.releaseYear)
        assertEquals(45.0, merged.purchasePrice)
    }

    @Test
    fun parsesGameEyeDates() {
        assertEquals(LocalDate(2021, 3, 24), CsvRow.parseDate("2021-03-24 10:12:00"))
        assertNull(CsvRow.parseDate("n/a"))
    }

    private class FakePrices : PriceRepository {
        var refreshes = 0

        override fun observeQuotes(): Flow<Map<String, List<PriceQuote>>> = MutableStateFlow(emptyMap())

        override suspend fun refreshQuotes(itemIds: List<String>): Int = (++refreshes).let { itemIds.size }

        override suspend fun history(itemId: String): List<PricePoint> = emptyList()
    }

    private class FakeSettings : SettingsRepository {
        val values = MutableStateFlow<Map<String, String>>(emptyMap())

        override fun observe(key: String) = values.map { it[key] }

        override suspend fun get(key: String) = values.value[key]

        override suspend fun set(
            key: String,
            value: String,
        ) {
            values.value = values.value + (key to value)
        }
    }

    private class FixedClock(
        var now: Instant,
    ) : Clock {
        override fun now() = now
    }

    @Test
    fun refreshIsThrottledUnlessForced() =
        runTest {
            val prices = FakePrices()
            val clock = FixedClock(Instant.fromEpochSeconds(1_000_000))
            val refresh = RefreshQuotesUseCase(prices, FakeSettings(), clock)
            assertEquals(2, refresh(listOf("a", "b")))
            assertNull(refresh(listOf("a", "b")))
            assertEquals(2, refresh(listOf("a", "b"), force = true))
            clock.now += 7.hours
            assertEquals(2, refresh(listOf("a", "b")))
            assertEquals(3, prices.refreshes)
            assertNull(refresh(emptyList(), force = true))
        }
}
