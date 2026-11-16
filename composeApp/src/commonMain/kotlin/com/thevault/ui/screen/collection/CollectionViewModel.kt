package com.thevault.ui.screen.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.ItemStatus
import com.thevault.ui.component.Manufacturer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

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
 * Order of the platform list.
 *
 * @property label Toggle label.
 * @property comparator Sort order.
 */
enum class PlatformSort(
    val label: String,
    val comparator: Comparator<PlatformSummary>,
) {
    /** Highest value first. */
    Value("Valeur", compareByDescending { it.value }),

    /** Most games first. */
    Count("Nombre", compareByDescending { it.count }),

    /** Alphabetical. */
    Name("A → Z", compareBy { it.platform.lowercase() }),

    /** Biggest 30-day change first. */
    Change("Variation", compareByDescending { it.changePercent?.let { c -> kotlin.math.abs(c) } ?: -1.0 }),
}

/**
 * Filters of the collection screen.
 *
 * @property kind Selected category.
 * @property platformSort Order of the platform list.
 * @property query Search among games.
 * @property consoleMaker Console manufacturer filter, or null for all.
 * @property accessoryFilter Accessory filter.
 * @property showSold Shows the sold items instead of a category.
 */
data class CollectionFilters(
    val kind: ItemKind = ItemKind.Game,
    val platformSort: PlatformSort = PlatformSort.Value,
    val query: String = "",
    val consoleMaker: Manufacturer? = null,
    val accessoryFilter: AccessoryFilter = AccessoryFilter.All,
    val showSold: Boolean = false,
)

/**
 * UI state of the collection screen.
 *
 * @property filters Current filters.
 * @property totals Count and value per category.
 * @property groups Game platforms grouped by manufacturer.
 * @property platforms Game platforms in the chosen order.
 * @property platformCount Number of game platforms.
 * @property gameResults Games matching the search (empty without query).
 * @property consoles Consoles matching the filter, most valuable first.
 * @property consoleMakers Manufacturers present among consoles.
 * @property accessories Accessories matching the filter, most valuable first.
 * @property sold Sold items, most recent sale first.
 * @property isLoading Whether data is still loading.
 */
data class CollectionUiState(
    val filters: CollectionFilters = CollectionFilters(),
    val totals: Map<ItemKind, KindTotal> = emptyMap(),
    val groups: List<ManufacturerGroup> = emptyList(),
    val platforms: List<PlatformSummary> = emptyList(),
    val platformCount: Int = 0,
    val gameResults: List<CollectionEntry> = emptyList(),
    val consoles: List<CollectionEntry> = emptyList(),
    val consoleMakers: List<Manufacturer> = emptyList(),
    val accessories: List<CollectionEntry> = emptyList(),
    val sold: List<CollectionEntry> = emptyList(),
    val isLoading: Boolean = false,
)

/** ViewModel of the collection screen (games by platform, consoles, accessories). */
class CollectionViewModel(
    source: VaultEntriesSource,
    private val refresher: CollectionRefresher,
) : ViewModel() {
    private val filters = MutableStateFlow(CollectionFilters())

    /** Pull to refresh state. */
    val refresh: StateFlow<RefreshUiState> = refresher.state

    /** Observable UI state. */
    val uiState: StateFlow<CollectionUiState> =
        combine(source.entries(), filters) { all, f -> buildCollectionState(all.owned(), f, all.filter { it.status == ItemStatus.SOLD }) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CollectionUiState(isLoading = true))

    /** Downloads the covers, sheets and quotes from the backend (pull to refresh). */
    fun onRefresh() = refresher.refresh()

    /** Selects a category. */
    fun onKindSelected(kind: ItemKind) = filters.update { it.copy(kind = kind, showSold = false) }

    /** Shows the sold items. */
    fun onSoldSelected() = filters.update { it.copy(showSold = true) }

    /** Updates the game search. */
    fun onQueryChanged(query: String) = filters.update { it.copy(query = query) }

    /** Filters consoles by manufacturer (null = all). */
    fun onConsoleMakerSelected(maker: Manufacturer?) = filters.update { it.copy(consoleMaker = maker) }

    /** Cycles the order of the platform list. */
    fun onPlatformSortToggled() =
        filters.update { it.copy(platformSort = PlatformSort.entries[(it.platformSort.ordinal + 1) % PlatformSort.entries.size]) }

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
    sold: List<CollectionEntry> = emptyList(),
): CollectionUiState {
    val games = owned.filter { it.kind == ItemKind.Game }
    val consoles = owned.filter { it.kind == ItemKind.Console }
    val accessories = owned.filter { it.kind == ItemKind.Accessory }
    val byValue = sortComparator(GameSort.Value)
    return CollectionUiState(
        filters = filters,
        totals = kindTotals(owned),
        groups = manufacturerGroups(games),
        platforms = platformSummaries(games).sortedWith(filters.platformSort.comparator),
        platformCount = platformSummaries(games).size,
        gameResults = if (filters.query.isBlank()) emptyList() else filterGames(games, GameFilters(query = filters.query)),
        consoles = consoles.filter { filters.consoleMaker == null || it.manufacturer == filters.consoleMaker }.sortedWith(byValue),
        consoleMakers = consoles.map { it.manufacturer }.distinct().sortedBy { it.order },
        accessories =
            accessories
                .filter { it.matches(filters.accessoryFilter) }
                .sortedWith(byValue),
        sold = sold.sortedWith(compareByDescending<CollectionEntry, LocalDate?>(nullsFirst()) { it.details.soldAt }),
    )
}

private fun CollectionEntry.matches(filter: AccessoryFilter): Boolean =
    when (filter) {
        AccessoryFilter.All -> true
        AccessoryFilter.Controllers -> isController()
        AccessoryFilter.Others -> !isController()
    }
