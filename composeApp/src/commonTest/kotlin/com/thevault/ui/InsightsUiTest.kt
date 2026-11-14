package com.thevault.ui

import com.thevault.domain.model.Components
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.PriceGrade
import com.thevault.domain.model.PriceQuote
import com.thevault.domain.model.Region
import com.thevault.domain.model.ValueSnapshot
import com.thevault.ui.component.formatAge
import com.thevault.ui.component.formatPercent
import com.thevault.ui.component.manufacturerOf
import com.thevault.ui.screen.collection.ChartPeriod
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.risers
import com.thevault.ui.screen.collection.window
import com.thevault.ui.screen.collection.withQuotes
import com.thevault.ui.screen.profile.achievementProgress
import com.thevault.ui.screen.profile.levelOf
import com.thevault.ui.screen.wishlist.WishSort
import com.thevault.ui.screen.wishlist.buildWishlist
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class InsightsUiTest {
    private fun entry(
        id: String,
        value: Double? = null,
        condition: Components = Components.CIB,
        status: ItemStatus = ItemStatus.OWNED,
        platform: String = "Super Nintendo",
    ) = CollectionEntry(
        id,
        ItemKind.Game,
        "T$id",
        platform,
        manufacturerOf(platform),
        null,
        condition,
        Region.PAL,
        status,
        value,
        null,
        null,
    )

    private fun quote(
        id: String,
        median: Double,
        past: Double? = null,
        grade: PriceGrade = PriceGrade.CIB,
        samples: Int = 5,
    ) = PriceQuote(id, grade, median, median, median, samples, Instant.fromEpochSeconds(0), past)

    @Test
    fun reliableQuoteOfTheConditionReplacesTheManualValue() {
        val entries =
            listOf(entry("a", 10.0), entry("b", 10.0), entry("c", 10.0, Components.LOOSE)).withQuotes(
                mapOf(
                    "a" to listOf(quote("a", 50.0, past = 40.0)),
                    "b" to listOf(quote("b", 99.0, samples = 1)),
                    "c" to listOf(quote("c", 70.0)),
                ),
            )
        assertEquals(listOf(50.0, 10.0, 10.0), entries.map { it.value })
        assertEquals(25.0, entries[0].changePercent)
        assertNull(entries[1].changePercent)
    }

    @Test
    fun risersAreSortedByAmountAndIgnoreDrops() {
        val entries =
            listOf(entry("a"), entry("b"), entry("c")).withQuotes(
                mapOf(
                    "a" to listOf(quote("a", 60.0, past = 50.0)),
                    "b" to listOf(quote("b", 200.0, past = 150.0)),
                    "c" to listOf(quote("c", 20.0, past = 30.0)),
                ),
            )
        assertEquals(listOf("b", "a"), risers(entries, 3).map { it.id })
    }

    @Test
    fun chartWindowKeepsThePeriod() {
        val today = LocalDate(2026, 11, 14)
        val snaps = listOf(LocalDate(2025, 1, 1), LocalDate(2026, 10, 20), LocalDate(2026, 11, 13)).map { ValueSnapshot(it, 1.0, 0.0) }
        assertEquals(2, ChartPeriod.Month.window(snaps, today).size)
        assertEquals(3, ChartPeriod.All.window(snaps, today).size)
    }

    @Test
    fun levelsGrowBy100xpPerLevel() {
        assertEquals(1, levelOf(0).level)
        assertEquals(2, levelOf(100).level)
        assertEquals(3, levelOf(300).level)
        assertEquals(50, levelOf(350).xpInLevel)
        assertEquals(300, levelOf(350).xpForNext)
    }

    @Test
    fun achievementsCountOwnedItemsOnly() {
        val all = List(40) { entry("s$it") } + entry("w", status = ItemStatus.WISHLIST)
        val progress = achievementProgress(all).associateBy { it.def.id }
        assertTrue(progress.getValue("bits16").unlocked)
        assertEquals(40, progress.getValue("first").progress)
    }

    @Test
    fun wishlistListsDrops() {
        val all =
            listOf(entry("a", status = ItemStatus.WISHLIST), entry("b", status = ItemStatus.WISHLIST), entry("c"))
                .withQuotes(mapOf("a" to listOf(quote("a", 30.0, past = 40.0)), "b" to listOf(quote("b", 90.0))))
        val state = buildWishlist(all, WishSort.Price)
        assertEquals(listOf("a"), state.dropping.map { it.id })
        assertEquals(listOf("b", "a"), state.watched.map { it.id })
        assertEquals(120.0, state.total)
    }

    @Test
    fun formatsPercentAndAge() {
        assertEquals("6,8 %", formatPercent(6.8))
        assertEquals("il y a 2 h", formatAge(2.hours + 5.minutes))
    }
}
