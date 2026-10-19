package com.thevault.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.VaultStats
import com.thevault.domain.usecase.ObserveAccessoriesUseCase
import com.thevault.domain.usecase.ObserveConsolesUseCase
import com.thevault.domain.usecase.ObserveGamesUseCase
import com.thevault.domain.usecase.ObserveVaultStatsUseCase
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.toEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

/**
 * Share of the collection owned on one platform.
 *
 * @property platform Platform name.
 * @property count Number of owned games.
 * @property fraction Share of all owned games, between 0 and 1.
 */
data class PlatformShare(
    val platform: String,
    val count: Int,
    val fraction: Float,
)

/**
 * State of the Dashboard screen.
 *
 * @property stats Vault statistics.
 * @property latest Most recently acquired items, all kinds.
 * @property platforms Top platforms by number of owned games.
 * @property topRated Highest rated games.
 * @property isLoading Whether data is still loading.
 */
data class DashboardUiState(
    val stats: VaultStats = VaultStats(0, 0, 0, 0, 0.0, 0.0),
    val latest: List<CollectionEntry> = emptyList(),
    val platforms: List<PlatformShare> = emptyList(),
    val topRated: List<Game> = emptyList(),
    val isLoading: Boolean = false,
)

/**
 * ViewModel of the Dashboard screen.
 */
class DashboardViewModel(
    observeVaultStats: ObserveVaultStatsUseCase,
    observeGames: ObserveGamesUseCase,
    observeConsoles: ObserveConsolesUseCase,
    observeAccessories: ObserveAccessoriesUseCase,
) : ViewModel() {
    /** Observable UI state. */
    val uiState: StateFlow<DashboardUiState> =
        combine(observeVaultStats(), observeGames(), observeConsoles(), observeAccessories()) { stats, games, consoles, accessories ->
            val ownedGames = games.filter { it.status != ItemStatus.WISHLIST }
            val owned =
                (ownedGames.map { it.toEntry() } + consoles.map { it.toEntry() } + accessories.map { it.toEntry() })
                    .filter { it.status != ItemStatus.WISHLIST }
            DashboardUiState(
                stats = stats,
                latest = latestEntries(owned),
                platforms = platformShares(ownedGames),
                topRated = topRatedGames(ownedGames),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = DashboardUiState(isLoading = true),
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private const val LATEST_COUNT = 10
private const val TOP_RATED_COUNT = 3
private const val PLATFORM_COUNT = 5

/** Most recent acquisitions first; items without date come last. */
internal fun latestEntries(entries: List<CollectionEntry>): List<CollectionEntry> =
    entries
        .sortedWith(compareByDescending<CollectionEntry, LocalDate?>(nullsFirst()) { it.acquiredAt })
        .take(LATEST_COUNT)

/** Highest rated games first, unrated games excluded. */
internal fun topRatedGames(games: List<Game>): List<Game> {
    val rated = games.filter { game -> game.rating?.let { it > 0 } == true }
    return rated.sortedByDescending { it.rating }.take(TOP_RATED_COUNT)
}

/** Top platforms by number of games, with their share of the total. */
internal fun platformShares(games: List<Game>): List<PlatformShare> {
    if (games.isEmpty()) return emptyList()
    return games
        .groupingBy { it.platform }
        .eachCount()
        .filterKeys { it.isNotBlank() }
        .entries
        .sortedByDescending { it.value }
        .take(PLATFORM_COUNT)
        .map { PlatformShare(it.key, it.value, it.value.toFloat() / games.size) }
}
