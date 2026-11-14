package com.thevault.domain

import com.thevault.domain.model.Completeness
import com.thevault.domain.model.Components
import com.thevault.domain.model.CopyDetails
import com.thevault.domain.model.Grade
import com.thevault.domain.model.PriceGrade
import com.thevault.domain.model.PricePoint
import com.thevault.domain.model.PriceQuote
import com.thevault.domain.model.componentsOfLegacy
import com.thevault.domain.model.legacyCode
import com.thevault.domain.model.priceGrade
import com.thevault.domain.repository.PriceRepository
import com.thevault.domain.repository.SettingsRepository
import com.thevault.domain.usecase.RefreshQuotesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class InsightsTest {
    @Test
    fun mapsConditionsToPriceGrades() {
        assertEquals(PriceGrade.LOOSE, Components.LOOSE.priceGrade)
        assertEquals(PriceGrade.CIB, Components(box = true).priceGrade)
        assertEquals(PriceGrade.NEW, Components.SEALED.priceGrade)
        assertNull(Components.DIGITAL.priceGrade)
        assertEquals(PriceGrade.CIB, PriceGrade.fromApi("CIB"))
        assertNull(PriceGrade.fromApi("graded"))
    }

    @Test
    fun derivesCompletenessAndLegacyCodes() {
        assertEquals(Completeness.LOOSE, Components(extras = true).completeness)
        assertEquals(Completeness.CIB, Components(box = true).completeness)
        assertEquals(Completeness.NEW, Components.SEALED.completeness)
        assertTrue(Components.CIB.copy(mediaGrade = Grade.MINT, boxGrade = Grade.MINT).isMintComplete)
        assertFalse(Components.CIB.isMintComplete)
        listOf("LOOSE", "CIB_NO_MANUAL", "CIB", "NEW_SEALED").forEach { assertEquals(it, componentsOfLegacy(it).legacyCode) }
        assertEquals(Grade.FAIR, componentsOfLegacy("FAIR").mediaGrade)
        assertEquals(15.0, CopyDetails(shippingCost = 5.0).totalCost(10.0))
        assertNull(CopyDetails().totalCost(null))
    }

    @Test
    fun quoteNeedsEnoughListings() {
        val q = PriceQuote("a", PriceGrade.CIB, 100.0, 80.0, 120.0, 2, Instant.fromEpochSeconds(0))
        assertFalse(q.isReliable)
        assertTrue(q.copy(samples = 3).isReliable)
    }

    private class FakePrices : PriceRepository {
        var refreshes = 0

        override fun observeQuotes(): Flow<Map<String, List<PriceQuote>>> = MutableStateFlow(emptyMap())

        override suspend fun refreshQuotes(itemIds: List<String>): Int = (++refreshes).let { itemIds.size }

        override suspend fun history(itemId: String): List<PricePoint> = emptyList()

        override fun observeSnapshots(): Flow<Map<String, List<com.thevault.domain.model.PriceSnapshot>>> = MutableStateFlow(emptyMap())

        override suspend fun replaceImportedPrices(snapshots: List<com.thevault.domain.model.PriceSnapshot>) = Unit
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
