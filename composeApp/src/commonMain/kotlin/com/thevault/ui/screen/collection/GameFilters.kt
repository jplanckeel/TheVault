package com.thevault.ui.screen.collection

import com.thevault.ui.component.FormatGroup
import com.thevault.ui.component.formatGroup
import kotlinx.datetime.LocalDate
import kotlin.math.abs

/**
 * Sort order of a game grid.
 *
 * @property label Toggle label.
 */
enum class GameSort(
    val label: String,
) {
    /** Highest value first. */
    Value("Valeur"),

    /** Most recently acquired first. */
    Recent("Récents"),

    /** Alphabetical. */
    Title("A → Z"),

    /** Biggest 30-day change of the quote first (up or down). */
    Change("Variation"),
}

/**
 * Filters of a game grid.
 *
 * @property platform Platform, or null for every platform.
 * @property query Free-text search.
 * @property format Format filter, or null for all.
 * @property sort Sort order.
 */
data class GameFilters(
    val platform: String? = null,
    val query: String = "",
    val format: FormatGroup? = null,
    val sort: GameSort = GameSort.Value,
)

/** Applies [filters] to games. Pure function, unit-tested. */
fun filterGames(
    games: List<CollectionEntry>,
    filters: GameFilters,
): List<CollectionEntry> {
    val query = filters.query.trim()
    return games
        .filter { filters.platform == null || it.platform == filters.platform }
        .filter { filters.format == null || it.condition.formatGroup == filters.format }
        .filter { query.isEmpty() || it.title.contains(query, ignoreCase = true) }
        .sortedWith(sortComparator(filters.sort))
}

/** Comparator of a [GameSort]. */
fun sortComparator(sort: GameSort): Comparator<CollectionEntry> =
    when (sort) {
        GameSort.Value -> compareByDescending<CollectionEntry> { it.value ?: -1.0 }.thenBy { it.title.lowercase() }
        GameSort.Recent ->
            compareByDescending<CollectionEntry, LocalDate?>(nullsFirst()) { it.acquiredAt }.thenBy { it.title.lowercase() }
        GameSort.Title -> compareBy { it.title.lowercase() }
        GameSort.Change -> compareByDescending<CollectionEntry> { it.changePercent?.let(::abs) ?: -1.0 }.thenBy { it.title.lowercase() }
    }
