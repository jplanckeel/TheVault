package com.thevault.ui.screen.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.usecase.ObserveAccessoriesUseCase
import com.thevault.domain.usecase.ObserveConsolesUseCase
import com.thevault.domain.usecase.ObserveGamesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * UI state shared by the Collection and Wishlist screens.
 *
 * @property filters Current filters.
 * @property view Filtered and sorted entries.
 * @property isLoading Whether data is still loading.
 */
data class CollectionUiState(
    val filters: CollectionFilters = CollectionFilters(),
    val view: CollectionView = CollectionView(),
    val isLoading: Boolean = false,
)

/**
 * Combines games, consoles and accessories into a filterable grid.
 *
 * @param observeGames Games source.
 * @param observeConsoles Consoles source.
 * @param observeAccessories Accessories source.
 * @param includeStatus Keeps only the entries whose status matches.
 */
abstract class BaseCollectionViewModel(
    observeGames: ObserveGamesUseCase,
    observeConsoles: ObserveConsolesUseCase,
    observeAccessories: ObserveAccessoriesUseCase,
    includeStatus: (ItemStatus) -> Boolean,
) : ViewModel() {
    private val filters = MutableStateFlow(CollectionFilters())

    /** Observable UI state. */
    val uiState: StateFlow<CollectionUiState> =
        combine(observeGames(), observeConsoles(), observeAccessories(), filters) { games, consoles, accessories, f ->
            val entries =
                (games.map { it.toEntry() } + consoles.map { it.toEntry() } + accessories.map { it.toEntry() })
                    .filter { includeStatus(it.status) }
            CollectionUiState(filters = f, view = buildCollectionView(entries, f))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = CollectionUiState(isLoading = true),
        )

    /** Selects a category and resets the platform filter. */
    fun onKindSelected(kind: ItemKind) = filters.update { it.copy(kind = kind, platform = null) }

    /** Updates the free-text search. */
    fun onQueryChanged(query: String) = filters.update { it.copy(query = query) }

    /** Selects a platform chip, or null for all platforms. */
    fun onPlatformSelected(platform: String?) = filters.update { it.copy(platform = platform) }

    /** Changes the sort order. */
    fun onSortSelected(sort: CollectionSort) = filters.update { it.copy(sort = sort) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Owned items (everything except the wishlist). */
class CollectionViewModel(
    observeGames: ObserveGamesUseCase,
    observeConsoles: ObserveConsolesUseCase,
    observeAccessories: ObserveAccessoriesUseCase,
) : BaseCollectionViewModel(observeGames, observeConsoles, observeAccessories, { it != ItemStatus.WISHLIST })

/** Wishlist items only. */
class WishlistViewModel(
    observeGames: ObserveGamesUseCase,
    observeConsoles: ObserveConsolesUseCase,
    observeAccessories: ObserveAccessoriesUseCase,
) : BaseCollectionViewModel(observeGames, observeConsoles, observeAccessories, { it == ItemStatus.WISHLIST })
