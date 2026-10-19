package com.thevault.ui.screen.collection

import com.thevault.domain.model.Accessory
import com.thevault.domain.model.Console
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemCondition
import com.thevault.domain.model.ItemStatus
import kotlinx.datetime.LocalDate

/**
 * Item category, used for the segmented selector of the Collection screen.
 *
 * @property label French plural label.
 */
enum class ItemKind(
    val label: String,
) {
    /** Video games. */
    Game("Jeux"),

    /** Consoles. */
    Console("Consoles"),

    /** Accessories. */
    Accessory("Accessoires"),
}

/**
 * Sort order of the collection grid.
 *
 * @property label French label.
 */
enum class CollectionSort(
    val label: String,
) {
    /** Most recently acquired first. */
    Recent("Récents"),

    /** Highest estimated value first. */
    Value("Valeur"),

    /** Alphabetical. */
    Title("A → Z"),
}

/**
 * Flattened view of a game, console or accessory for grids and lists.
 *
 * @property id Domain identifier.
 * @property kind Item category.
 * @property title Display title.
 * @property platform Platform (games), manufacturer (consoles) or compatible console (accessories).
 * @property coverUrl Cover or photo URL.
 * @property condition Physical condition.
 * @property status Ownership status.
 * @property value Estimated current value, if known.
 * @property acquiredAt Acquisition date, if known.
 */
data class CollectionEntry(
    val id: String,
    val kind: ItemKind,
    val title: String,
    val platform: String,
    val coverUrl: String?,
    val condition: ItemCondition,
    val status: ItemStatus,
    val value: Double?,
    val acquiredAt: LocalDate?,
)

/** Maps a game to a [CollectionEntry]. */
fun Game.toEntry(): CollectionEntry =
    CollectionEntry(id, ItemKind.Game, title, platform, coverUrl, condition, status, currentValue, acquiredAt)

/** Maps a console to a [CollectionEntry]. */
fun Console.toEntry(): CollectionEntry =
    CollectionEntry(id, ItemKind.Console, name, manufacturer, imageUrl, condition, status, currentValue, acquiredAt)

/** Maps an accessory to a [CollectionEntry]. */
fun Accessory.toEntry(): CollectionEntry =
    CollectionEntry(id, ItemKind.Accessory, name, compatibleConsole, imageUrl, condition, status, currentValue, acquiredAt)

/**
 * User-controlled filters of a collection grid.
 *
 * @property kind Selected category.
 * @property query Free-text search on title and platform.
 * @property platform Selected platform chip, or null for all.
 * @property sort Sort order.
 */
data class CollectionFilters(
    val kind: ItemKind = ItemKind.Game,
    val query: String = "",
    val platform: String? = null,
    val sort: CollectionSort = CollectionSort.Recent,
)

/**
 * Result of applying [CollectionFilters] to a set of entries.
 *
 * @property entries Visible entries, filtered and sorted.
 * @property platforms Platforms available for the selected kind, most frequent first.
 * @property countByKind Number of entries per kind (before text/platform filtering).
 * @property totalCount Total number of entries across all kinds.
 * @property totalValue Sum of known values across all kinds.
 */
data class CollectionView(
    val entries: List<CollectionEntry> = emptyList(),
    val platforms: List<String> = emptyList(),
    val countByKind: Map<ItemKind, Int> = emptyMap(),
    val totalCount: Int = 0,
    val totalValue: Double = 0.0,
)

/**
 * Applies [filters] to [all] entries. Pure function, unit-tested.
 *
 * @param all Every entry in scope (already restricted to owned or wishlist items).
 * @param filters Filters chosen by the user.
 */
fun buildCollectionView(
    all: List<CollectionEntry>,
    filters: CollectionFilters,
): CollectionView {
    val ofKind = all.filter { it.kind == filters.kind }
    val platforms =
        ofKind
            .groupingBy { it.platform }
            .eachCount()
            .filterKeys { it.isNotBlank() }
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
    val query = filters.query.trim()
    val visible =
        ofKind
            .filter { filters.platform == null || it.platform.equals(filters.platform, ignoreCase = true) }
            .filter { query.isEmpty() || it.title.contains(query, true) || it.platform.contains(query, true) }
    return CollectionView(
        entries = visible.sortedWith(comparatorFor(filters.sort)),
        platforms = platforms,
        countByKind = ItemKind.entries.associateWith { kind -> all.count { it.kind == kind } },
        totalCount = all.size,
        totalValue = all.sumOf { it.value ?: 0.0 },
    )
}

private fun comparatorFor(sort: CollectionSort): Comparator<CollectionEntry> =
    when (sort) {
        CollectionSort.Recent ->
            compareByDescending<CollectionEntry, LocalDate?>(nullsFirst()) { it.acquiredAt }
                .thenBy { it.title.lowercase() }
        CollectionSort.Value -> compareByDescending<CollectionEntry> { it.value ?: -1.0 }.thenBy { it.title.lowercase() }
        CollectionSort.Title -> compareBy { it.title.lowercase() }
    }
