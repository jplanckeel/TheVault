package com.thevault.ui.screen.collection

import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.ValueSnapshot
import com.thevault.ui.component.FormatGroup
import com.thevault.ui.component.formatGroup
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.time.Instant

private const val HOME_PLATFORMS = 4
private const val TOP_VALUE = 3
private const val RISERS = 3
private const val STATS_PLATFORMS = 5
private const val STATS_TOP = 10

/**
 * Count and value of one category.
 *
 * @property count Number of items.
 * @property value Estimated value.
 */
data class KindTotal(
    val count: Int = 0,
    val value: Double = 0.0,
)

/**
 * Purchase price coverage.
 *
 * @property filled Items with a purchase price.
 * @property missing Items without a purchase price.
 * @property invested Sum of the known purchase prices.
 */
data class PurchaseCoverage(
    val filled: Int = 0,
    val missing: Int = 0,
    val invested: Double = 0.0,
)

/**
 * Data of the home screen.
 *
 * @property totalValue Estimated value of owned items.
 * @property totalCount Number of owned items.
 * @property platformCount Number of distinct game platforms.
 * @property byKind Totals per category.
 * @property purchases Purchase price coverage.
 * @property risers Owned items whose quote rose the most over 30 days.
 * @property platforms Top game platforms by value.
 * @property topValue Most valuable items.
 * @property quotedAt Time of the most recent eBay quote, null when none.
 * @property indicativeValue Part of [totalValue] that only comes from imported US prices.
 */
data class HomeSummary(
    val totalValue: Double = 0.0,
    val totalCount: Int = 0,
    val platformCount: Int = 0,
    val byKind: Map<ItemKind, KindTotal> = emptyMap(),
    val purchases: PurchaseCoverage = PurchaseCoverage(),
    val risers: List<CollectionEntry> = emptyList(),
    val platforms: List<PlatformSummary> = emptyList(),
    val topValue: List<CollectionEntry> = emptyList(),
    val quotedAt: Instant? = null,
    val indicativeValue: Double = 0.0,
)

/** Items that count as part of the collection (not wishlist, sold or loaned). */
fun List<CollectionEntry>.owned(): List<CollectionEntry> = filter { it.status == ItemStatus.OWNED }

/** Builds the home screen data. Pure function, unit-tested. */
fun buildHomeSummary(all: List<CollectionEntry>): HomeSummary {
    val owned = all.owned()
    val games = owned.filter { it.kind == ItemKind.Game }
    return HomeSummary(
        totalValue = owned.totalValue(),
        totalCount = owned.size,
        platformCount = platformSummaries(games).size,
        byKind = kindTotals(owned),
        purchases = purchaseCoverage(owned),
        risers = risers(owned, RISERS),
        platforms = platformSummaries(games).sortedByDescending { it.value }.take(HOME_PLATFORMS),
        topValue = owned.filter { it.value != null }.sortedByDescending { it.value }.take(TOP_VALUE),
        quotedAt = latestQuote(all),
        indicativeValue = owned.filter { it.valueSource == ValueSource.UsIndicative }.sumOf { it.value ?: 0.0 },
    )
}

/** Items whose quote rose the most (in euros) over 30 days. */
fun risers(
    entries: List<CollectionEntry>,
    count: Int,
): List<CollectionEntry> =
    entries
        .filter { it.changeAmount ?: 0.0 > 0.0 }
        .sortedByDescending { it.changeAmount }
        .take(count)

/** Time of the most recent quote among [entries]. */
fun latestQuote(entries: List<CollectionEntry>): Instant? = entries.flatMap { e -> e.quotes.map { it.scrapedAt } }.maxOrNull()

/** Totals per category. */
fun kindTotals(entries: List<CollectionEntry>): Map<ItemKind, KindTotal> =
    ItemKind.entries.associateWith { kind ->
        entries.filter { it.kind == kind }.let { KindTotal(it.size, it.totalValue()) }
    }

/** Purchase price coverage. */
fun purchaseCoverage(entries: List<CollectionEntry>): PurchaseCoverage {
    val filled = entries.filter { it.purchasePrice != null }
    return PurchaseCoverage(filled.size, entries.size - filled.size, filled.sumOf { it.purchasePrice ?: 0.0 })
}

/**
 * Data of the stats screen.
 *
 * @property totalValue Estimated value of owned items.
 * @property byKind Totals per category.
 * @property topPlatforms Game platforms with the highest value.
 * @property formats Number of games per condition family.
 * @property purchases Purchase price coverage.
 * @property totalCount Number of owned items.
 * @property topValue Most valuable items.
 * @property gainOnPriced Value minus price paid, on the items whose price is known.
 * @property valueOfPriced Value of the items whose price is known.
 * @property since Earliest acquisition date, null when unknown.
 * @property platformGains Latent gain (value − price − shipping) per game platform, biggest first.
 */
data class StatsSummary(
    val totalValue: Double = 0.0,
    val byKind: Map<ItemKind, KindTotal> = emptyMap(),
    val topPlatforms: List<PlatformSummary> = emptyList(),
    val formats: Map<FormatGroup, Int> = emptyMap(),
    val purchases: PurchaseCoverage = PurchaseCoverage(),
    val totalCount: Int = 0,
    val topValue: List<CollectionEntry> = emptyList(),
    val gainOnPriced: Double = 0.0,
    val valueOfPriced: Double = 0.0,
    val since: LocalDate? = null,
    val platformGains: List<PlatformGain> = emptyList(),
)

/**
 * Latent gain of the priced items of a platform.
 *
 * @property platform Platform.
 * @property manufacturer Manufacturer.
 * @property gain Value − price − shipping.
 * @property cost Price + shipping.
 */
data class PlatformGain(
    val platform: String,
    val manufacturer: com.thevault.ui.component.Manufacturer,
    val gain: Double,
    val cost: Double,
)

/** Price + shipping of an entry, null when the price is unknown. */
val CollectionEntry.cost: Double? get() = details.totalCost(purchasePrice)

/** Latent gain per game platform, on the items whose value and price are known. */
fun platformGains(owned: List<CollectionEntry>): List<PlatformGain> =
    owned
        .filter { it.kind == ItemKind.Game && it.cost != null && it.value != null }
        .groupBy { it.platform }
        .map { (p, list) ->
            val cost = list.sumOf { it.cost ?: 0.0 }
            PlatformGain(p, list.first().manufacturer, list.sumOf { it.value ?: 0.0 } - cost, cost)
        }.sortedByDescending { kotlin.math.abs(it.gain) }

/** Builds the stats screen data. Pure function, unit-tested. */
fun buildStatsSummary(all: List<CollectionEntry>): StatsSummary {
    val owned = all.owned()
    val priced = owned.filter { it.cost != null && it.value != null }
    return StatsSummary(
        totalValue = owned.totalValue(),
        byKind = kindTotals(owned),
        topPlatforms = platformSummaries(owned.filter { it.kind == ItemKind.Game }).sortedByDescending { it.value }.take(STATS_PLATFORMS),
        formats = FormatGroup.entries.associateWith { group -> owned.count { it.condition.formatGroup == group } },
        purchases = purchaseCoverage(owned),
        totalCount = owned.size,
        topValue = owned.filter { it.value != null }.sortedByDescending { it.value }.take(STATS_TOP),
        gainOnPriced = priced.sumOf { (it.value ?: 0.0) - (it.cost ?: 0.0) },
        valueOfPriced = priced.sumOf { it.value ?: 0.0 },
        since = owned.mapNotNull { it.acquiredAt }.minOrNull(),
        platformGains = platformGains(owned).take(STATS_PLATFORMS),
    )
}

/**
 * Period of the value chart.
 *
 * @property label Segment label.
 * @property days Length in days, null for the whole history.
 * @property suffix Muted period after the change ("30 j").
 */
enum class ChartPeriod(
    val label: String,
    val days: Int?,
    val suffix: String,
) {
    /** One month. */
    Month("1M", 30, "30 j"),

    /** Six months. */
    HalfYear("6M", 182, "6 mois"),

    /** One year. */
    Year("1A", 365, "1 an"),

    /** Everything recorded. */
    All("TOUT", null, "total"),
}

/** Snapshots of [period] ending [today], oldest first. */
fun ChartPeriod.window(
    snapshots: List<ValueSnapshot>,
    today: LocalDate,
): List<ValueSnapshot> {
    val from = days?.let { today.minus(DatePeriod(days = it)) }
    return snapshots.filter { from == null || it.day >= from }.sortedBy { it.day }
}
