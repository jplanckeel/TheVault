package com.thevault.ui.screen.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.ui.component.FormatGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * UI state of the games of one platform (or of every platform).
 *
 * @property filters Current filters.
 * @property games Visible games.
 * @property count Number of games of the platform (before search / format filters).
 * @property value Value of the games of the platform.
 * @property platforms Every game platform, for the « Changer » menu.
 * @property isLoading Whether data is still loading.
 */
data class PlatformGamesUiState(
    val filters: GameFilters = GameFilters(),
    val games: List<CollectionEntry> = emptyList(),
    val count: Int = 0,
    val value: Double = 0.0,
    val platforms: List<PlatformSummary> = emptyList(),
    val isLoading: Boolean = false,
)

/** ViewModel of the platform games grid. */
class PlatformGamesViewModel(
    source: VaultEntriesSource,
) : ViewModel() {
    private val filters = MutableStateFlow(GameFilters())

    /** Observable UI state. */
    val uiState: StateFlow<PlatformGamesUiState> =
        combine(source.entries(), filters) { all, f ->
            val games = all.owned().filter { it.kind == ItemKind.Game }
            val ofPlatform = games.filter { f.platform == null || it.platform == f.platform }
            PlatformGamesUiState(f, filterGames(games, f), ofPlatform.size, ofPlatform.totalValue(), platformSummaries(games))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PlatformGamesUiState(isLoading = true))

    /** Shows the games of [platform] (null = every platform) and resets the filters. */
    fun onPlatformSelected(platform: String?) = filters.update { GameFilters(platform = platform, sort = it.sort) }

    /** Updates the search. */
    fun onQueryChanged(query: String) = filters.update { it.copy(query = query) }

    /** Filters by format (null = all). */
    fun onFormatSelected(format: FormatGroup?) = filters.update { it.copy(format = format) }

    /** Cycles through the sort orders. */
    fun onSortToggled() = filters.update { it.copy(sort = GameSort.entries[(it.sort.ordinal + 1) % GameSort.entries.size]) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
