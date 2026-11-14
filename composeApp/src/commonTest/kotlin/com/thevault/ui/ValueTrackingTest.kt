package com.thevault.ui

import com.thevault.domain.model.CopyDetails
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.PriceGrade
import com.thevault.domain.model.PricePoint
import com.thevault.domain.model.PriceQuote
import com.thevault.domain.model.PriceSnapshot
import com.thevault.domain.model.PriceSource
import com.thevault.domain.model.Region
import com.thevault.ui.component.manufacturerOf
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.GameSort
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.ValueSource
import com.thevault.ui.screen.collection.mergeHistory
import com.thevault.ui.screen.collection.platformGains
import com.thevault.ui.screen.collection.purchasesByMonth
import com.thevault.ui.screen.collection.realisedProfit
import com.thevault.ui.screen.collection.regionSplit
import com.thevault.ui.screen.collection.salesSummary
import com.thevault.ui.screen.collection.sortComparator
import com.thevault.ui.screen.collection.withMarket
import com.thevault.ui.screen.report.buildQuoteReport
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValueTrackingTest {
    private val d1 = LocalDate(2026, 11, 1)
    private val d2 = LocalDate(2026, 11, 8)

    private fun entry(
        id: String,
        value: Double? = null,
        paid: Double? = null,
        details: CopyDetails = CopyDetails(),
        status: ItemStatus = ItemStatus.OWNED,
        acquired: LocalDate? = null,
    ) = CollectionEntry(
        id,
        ItemKind.Game,
        "T$id",
        "GameCube",
        manufacturerOf("GameCube"),
        null,
        details.components,
        Region.PAL,
        status,
        value,
        paid,
        acquired,
        details = details,
    )

    private fun pal(
        id: String,
        day: LocalDate,
        cib: Double,
    ) = PriceSnapshot(id, day, PriceSource.EBAY_PAL, null, cib, null, 5)

    private fun us(
        id: String,
        cib: Double,
    ) = PriceSnapshot(id, d1, PriceSource.GAMEYE_US, 10.0, cib, 90.0)

    private fun quote(
        id: String,
        median: Double,
    ) = PriceQuote(id, PriceGrade.CIB, median, median, median, 5, kotlin.time.Instant.fromEpochSeconds(0))

    @Test
    fun valueFallsBackFromPalToManualToUs() {
        val entries =
            listOf(entry("pal", 10.0), entry("manual", 20.0), entry("us"), entry("none"))
                .withMarket(
                    mapOf("pal" to listOf(quote("pal", 50.0))),
                    mapOf(
                        "us" to listOf(us("us", 33.0)),
                        "pal" to listOf(us("pal", 99.0)),
                    ),
                ).associateBy { it.id }
        assertEquals(50.0 to ValueSource.Ebay, entries.getValue("pal").let { it.value to it.valueSource })
        assertEquals(20.0 to ValueSource.Manual, entries.getValue("manual").let { it.value to it.valueSource })
        assertEquals(33.0 to ValueSource.UsIndicative, entries.getValue("us").let { it.value to it.valueSource })
        assertEquals(null to ValueSource.None, entries.getValue("none").let { it.value to it.valueSource })
        assertEquals(99.0, entries.getValue("pal").usPrice)
    }

    @Test
    fun reportComparesTheLastTwoRefreshes() {
        val entries = listOf(entry("up"), entry("down"), entry("same"), entry("new"), entry("sold", status = ItemStatus.SOLD))
        val snaps =
            mapOf(
                "up" to listOf(pal("up", d1, 10.0), pal("up", d2, 15.0)),
                "down" to listOf(pal("down", d1, 30.0), pal("down", d2, 22.0)),
                "same" to listOf(pal("same", d1, 5.0), pal("same", d2, 5.0)),
                "new" to listOf(pal("new", d2, 40.0)),
                "sold" to listOf(pal("sold", d1, 1.0), pal("sold", d2, 100.0)),
            )
        val report = buildQuoteReport(entries, snaps)
        requireNotNull(report)
        assertEquals(d2, report.day)
        assertEquals(listOf("up"), report.ups.map { it.entry.id })
        assertEquals(listOf("down"), report.downs.map { it.entry.id })
        assertEquals(-3.0, report.total)
        assertNull(buildQuoteReport(entries, mapOf("new" to listOf(pal("new", d2, 40.0)))))
    }

    @Test
    fun localHistoryOnlyFillsMissingDays() {
        val t1 = d1.atStartOfDayIn(TimeZone.UTC)
        val t2 = d2.atStartOfDayIn(TimeZone.UTC)
        val remote = listOf(PricePoint(t1, PriceGrade.CIB, 10.0))
        val local = listOf(PricePoint(t1, PriceGrade.CIB, 11.0), PricePoint(t2, PriceGrade.CIB, 12.0))
        assertEquals(listOf(10.0, 12.0), mergeHistory(remote, local).map { it.median })
    }

    @Test
    fun salesAndGainsSubtractShipping() {
        val sold = entry("s", paid = 20.0, details = CopyDetails(shippingCost = 5.0, soldPrice = 40.0), status = ItemStatus.SOLD)
        assertEquals(15.0, sold.realisedProfit)
        assertNull(entry("x", details = CopyDetails(soldPrice = 40.0), status = ItemStatus.SOLD).realisedProfit)
        assertEquals(15.0, salesSummary(listOf(sold)).profit)
        val gains = platformGains(listOf(entry("a", 50.0, 20.0, CopyDetails(shippingCost = 5.0)), entry("b", 10.0)))
        assertEquals(25.0, gains.single().gain)
        assertEquals(25.0, gains.single().cost)
    }

    @Test
    fun monthlyPurchasesAndRegions() {
        val owned =
            listOf(
                entry("a", paid = 10.0, acquired = LocalDate(2026, 11, 2)),
                entry("b", acquired = LocalDate(2026, 11, 3), details = CopyDetails(regionVariant = "FAH")),
                entry("c", paid = 7.0, acquired = LocalDate(2025, 12, 3)),
                entry("d", paid = 99.0, acquired = LocalDate(2025, 11, 3)),
            )
        val months = purchasesByMonth(owned, LocalDate(2026, 11, 14))
        assertEquals(12, months.size)
        assertEquals(2025 to 12, months.first().year to months.first().month)
        assertEquals(7.0, months.first().spent)
        assertEquals(2, months.last().count)
        assertEquals(10.0, months.last().spent)
        assertEquals(listOf("PAL" to 3, "PAL · FAH" to 1), regionSplit(owned))
    }

    @Test
    fun sortsByBiggestChange() {
        val entries =
            listOf(entry("a"), entry("b"), entry("c")).withMarket(
                mapOf(
                    "a" to listOf(quote("a", 11.0).copy(medianPast = 10.0)),
                    "b" to listOf(quote("b", 5.0).copy(medianPast = 10.0)),
                ),
            )
        assertEquals(listOf("b", "a", "c"), entries.sortedWith(sortComparator(GameSort.Change)).map { it.id })
    }
}
