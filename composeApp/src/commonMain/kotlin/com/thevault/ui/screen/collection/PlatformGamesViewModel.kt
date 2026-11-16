package com.thevault.ui.screen.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import com.thevault.ui.component.FormatGroup
import com.thevault.ui.component.Manufacturer
import com.thevault.ui.component.formatGroup
import com.thevault.ui.component.manufacturerOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which games of the platform are shown. */
enum class Ownership {
    /** Owned games. */
    Owned,

    /** Wishlist. */
    Wishlist,

    /** Both. */
    All,
}

/**
 * UI state of the games of one platform (or of every platform).
 *
 * @property platform Platform, null for every game.
 * @property ownership Shown games.
 * @property filters Format and sort filters.
 * @property games Visible games.
 * @property ownedCount Owned games of the platform.
 * @property wishCount Wishlist games of the platform.
 * @property value Value of the owned games.
 * @property formats Owned games per condition family (non-empty ones).
 * @property manufacturer Manufacturer of the platform.
 * @property region Most frequent region, null when mixed or unknown.
 * @property isLoading Whether data is still loading.
 */
data class PlatformGamesUiState(
    val platform: String? = null,
    val ownership: Ownership = Ownership.Owned,
    val filters: GameFilters = GameFilters(),
    val games: List<CollectionEntry> = emptyList(),
    val ownedCount: Int = 0,
    val wishCount: Int = 0,
    val value: Double = 0.0,
    val formats: Map<FormatGroup, Int> = emptyMap(),
    val manufacturer: Manufacturer = Manufacturer.Other,
    val region: Region? = null,
    val isLoading: Boolean = false,
)

/** ViewModel of the platform games grid. */
class PlatformGamesViewModel(
    source: VaultEntriesSource,
    private val actions: ItemActions,
    private val refresher: CollectionRefresher,
) : ViewModel() {
    /** Pull to refresh state. */
    val refresh: StateFlow<RefreshUiState> = refresher.state

    /** Identifiers selected for a bulk edit (empty outside of selection mode). */
    val selection = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())

    /** Downloads the covers, sheets and quotes from the backend (pull to refresh). */
    fun onRefresh() = refresher.refresh()

    /** Adds or removes a game from the selection. */
    fun toggleSelection(id: String) = selection.update { if (id in it) it - id else it + id }

    /** Leaves the selection mode. */
    fun clearSelection() {
        selection.value = emptySet()
    }

    /** Applies [change] to the selected games and leaves the selection mode. */
    fun applyBulk(change: BulkChange) {
        val ids = selection.value
        viewModelScope.launch {
            actions.editDetails(ItemKind.Game, ids, change::applyTo)
            clearSelection()
        }
    }

    private val filters = MutableStateFlow(GameFilters())
    private val ownership = MutableStateFlow(Ownership.Owned)

    /** Observable UI state. */
    val uiState: StateFlow<PlatformGamesUiState> =
        combine(source.entries(), filters, ownership) { all, f, o -> buildPlatformState(all, f, o) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PlatformGamesUiState(isLoading = true))

    /** Shows the games of [platform] (null = every platform). */
    fun onPlatformSelected(platform: String?) = filters.update { GameFilters(platform = platform, sort = it.sort) }

    /** Filters by format (null = all). */
    fun onFormatSelected(format: FormatGroup?) = filters.update { it.copy(format = format) }

    /** Owned, wishlist or both. */
    fun onOwnershipSelected(value: Ownership) {
        ownership.value = value
    }

    /** Cycles through the sort orders. */
    fun onSortToggled() = filters.update { it.copy(sort = GameSort.entries[(it.sort.ordinal + 1) % GameSort.entries.size]) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Builds the platform state. Pure function. */
fun buildPlatformState(
    all: List<CollectionEntry>,
    f: GameFilters,
    o: Ownership,
): PlatformGamesUiState {
    val games = all.filter { it.kind == ItemKind.Game && (f.platform == null || it.platform == f.platform) }
    val owned = games.filter { it.status == ItemStatus.OWNED }
    val wished = games.filter { it.status == ItemStatus.WISHLIST }
    val shown =
        when (o) {
            Ownership.Owned -> owned
            Ownership.Wishlist -> wished
            Ownership.All -> owned + wished
        }
    val regions = owned.groupingBy { it.region }.eachCount()
    return PlatformGamesUiState(
        platform = f.platform,
        ownership = o,
        filters = f,
        games = filterGames(shown, f),
        ownedCount = owned.size,
        wishCount = wished.size,
        value = owned.totalValue(),
        formats =
            owned
                .groupingBy { it.condition.formatGroup }
                .eachCount()
                .toList()
                .sortedBy { it.first.ordinal }
                .toMap(),
        manufacturer = f.platform?.let { manufacturerOf(it) } ?: Manufacturer.Other,
        region = regions.maxByOrNull { it.value }?.key?.takeIf { it != Region.OTHER },
    )
}
