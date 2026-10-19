package com.thevault.ui.screen.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.ui.component.Manufacturer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Accessory filter of the collection.
 *
 * @property label Chip label.
 */
enum class AccessoryFilter(
    val label: String,
) {
    /** Every accessory. */
    All("Tous"),

    /** Controllers. */
    Controllers("Manettes"),

    /** Everything else. */
    Others("Autres"),
}

/**
 * Filters of the collection screen.
 *
 * @property kind Selected category.
 * @property query Search among games.
 * @property consoleMaker Console manufacturer filter, or null for all.
 * @property accessoryFilter Accessory filter.
 */
data class CollectionFilters(
    val kind: ItemKind = ItemKind.Game,
    val query: String = "",
    val consoleMaker: Manufacturer? = null,
    val accessoryFilter: AccessoryFilter = AccessoryFilter.All,
)

/**
 * UI state of the collection screen.
 *
 * @property filters Current filters.
 * @property totals Count and value per category.
 * @property groups Game platforms grouped by manufacturer.
 * @property platformCount Number of game platforms.
 * @property gameResults Games matching the search (empty without query).
 * @property consoles Consoles matching the filter, most valuable first.
 * @property consoleMakers Manufacturers present among consoles.
 * @property accessories Accessories matching the filter, most valuable first.
 * @property isLoading Whether data is still loading.
 */
data class CollectionUiState(
    val filters: CollectionFilters = CollectionFilters(),
    val totals: Map<ItemKind, KindTotal> = emptyMap(),
    val groups: List<ManufacturerGroup> = emptyList(),
    val platformCount: Int = 0,
    val gameResults: List<CollectionEntry> = emptyList(),
    val consoles: List<CollectionEntry> = emptyList(),
    val consoleMakers: List<Manufacturer> = emptyList(),
    val accessories: List<CollectionEntry> = emptyList(),
    val isLoading: Boolean = false,
)

/** ViewModel of the collection screen (games by platform, consoles, accessories). */
class CollectionViewModel(
    source: VaultEntriesSource,
) : ViewModel() {
    private val filters = MutableStateFlow(CollectionFilters())

    /** Observable UI state. */
    val uiState: StateFlow<CollectionUiState> =
        combine(source.entries(), filters) { all, f -> buildCollectionState(all.owned(), f) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CollectionUiState(isLoading = true))

    /** Selects a category. */
    fun onKindSelected(kind: ItemKind) = filters.update { it.copy(kind = kind) }

    /** Updates the game search. */
    fun onQueryChanged(query: String) = filters.update { it.copy(query = query) }

    /** Filters consoles by manufacturer (null = all). */
    fun onConsoleMakerSelected(maker: Manufacturer?) = filters.update { it.copy(consoleMaker = maker) }

    /** Filters accessories. */
    fun onAccessoryFilterSelected(filter: AccessoryFilter) = filters.update { it.copy(accessoryFilter = filter) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Builds the collection state from owned entries. Pure function, unit-tested. */
fun buildCollectionState(
    owned: List<CollectionEntry>,
    filters: CollectionFilters,
): CollectionUiState {
    val games = owned.filter { it.kind == ItemKind.Game }
    val consoles = owned.filter { it.kind == ItemKind.Console }
    val accessories = owned.filter { it.kind == ItemKind.Accessory }
    val byValue = sortComparator(GameSort.Value)
    return CollectionUiState(
        filters = filters,
        totals = kindTotals(owned),
        groups = manufacturerGroups(games),
        platformCount = platformSummaries(games).size,
        gameResults = if (filters.query.isBlank()) emptyList() else filterGames(games, GameFilters(query = filters.query)),
        consoles = consoles.filter { filters.consoleMaker == null || it.manufacturer == filters.consoleMaker }.sortedWith(byValue),
        consoleMakers = consoles.map { it.manufacturer }.distinct().sortedBy { it.order },
        accessories =
            accessories
                .filter { it.matches(filters.accessoryFilter) }
                .sortedWith(byValue),
    )
}

private fun CollectionEntry.matches(filter: AccessoryFilter): Boolean =
    when (filter) {
        AccessoryFilter.All -> true
        AccessoryFilter.Controllers -> isController()
        AccessoryFilter.Others -> !isController()
    }
