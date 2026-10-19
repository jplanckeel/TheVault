package com.thevault.ui.screen.collection

import com.thevault.domain.model.ItemStatus
import com.thevault.ui.component.FormatGroup
import com.thevault.ui.component.formatGroup
import kotlinx.datetime.LocalDate

private const val RECENT_COUNT = 8
private const val HOME_PLATFORMS = 4
private const val MASTERPIECES = 3
private const val STATS_PLATFORMS = 5

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
 * @property recent Most recently added items.
 * @property platforms Top game platforms by value.
 * @property masterpieces Most valuable items.
 */
data class HomeSummary(
    val totalValue: Double = 0.0,
    val totalCount: Int = 0,
    val platformCount: Int = 0,
    val byKind: Map<ItemKind, KindTotal> = emptyMap(),
    val purchases: PurchaseCoverage = PurchaseCoverage(),
    val recent: List<CollectionEntry> = emptyList(),
    val platforms: List<PlatformSummary> = emptyList(),
    val masterpieces: List<CollectionEntry> = emptyList(),
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
        platformCount =
            games
                .map { it.platform }
                .filter { it.isNotBlank() }
                .distinct()
                .size,
        byKind = kindTotals(owned),
        purchases = purchaseCoverage(owned),
        recent =
            owned
                .sortedWith(compareByDescending<CollectionEntry, LocalDate?>(nullsFirst()) { it.acquiredAt })
                .take(RECENT_COUNT),
        platforms = platformSummaries(games).sortedByDescending { it.value }.take(HOME_PLATFORMS),
        masterpieces = owned.filter { it.value != null }.sortedByDescending { it.value }.take(MASTERPIECES),
    )
}

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
 * @property formats Number of items per format family.
 * @property purchases Purchase price coverage.
 * @property totalCount Number of owned items.
 */
data class StatsSummary(
    val totalValue: Double = 0.0,
    val byKind: Map<ItemKind, KindTotal> = emptyMap(),
    val topPlatforms: List<PlatformSummary> = emptyList(),
    val formats: Map<FormatGroup, Int> = emptyMap(),
    val purchases: PurchaseCoverage = PurchaseCoverage(),
    val totalCount: Int = 0,
)

/** Builds the stats screen data. Pure function, unit-tested. */
fun buildStatsSummary(all: List<CollectionEntry>): StatsSummary {
    val owned = all.owned()
    return StatsSummary(
        totalValue = owned.totalValue(),
        byKind = kindTotals(owned),
        topPlatforms = platformSummaries(owned.filter { it.kind == ItemKind.Game }).sortedByDescending { it.value }.take(STATS_PLATFORMS),
        formats = FormatGroup.entries.associateWith { group -> owned.count { it.condition.formatGroup == group } },
        purchases = purchaseCoverage(owned),
        totalCount = owned.size,
    )
}
