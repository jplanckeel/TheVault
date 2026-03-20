package com.thevault.ui.screen.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.Accessory
import com.thevault.domain.model.Console
import com.thevault.domain.model.Game
import com.thevault.domain.usecase.ObserveAccessoriesUseCase
import com.thevault.domain.usecase.ObserveConsolesUseCase
import com.thevault.domain.model.IgdbPlatformCatalog
import com.thevault.domain.usecase.ObserveGamesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * A platform entry shown in the collection overview.
 *
 * @property name Platform name (e.g. "PlayStation 5").
 * @property manufacturer Console manufacturer (e.g. "Sony").
 * @property productionYear Year used for sorting within a manufacturer group.
 * @property gameCount Number of games owned on this platform.
 * @property coverUrls Up to 4 cover URLs used as a mosaic background.
 * @property console Matching console entity, if any (provides image).
 */
data class PlatformEntry(
    val name: String,
    val manufacturer: String,
    val productionYear: Int,
    val gameCount: Int,
    val coverUrls: List<String>,
    val console: Console?,
)

/**
 * A manufacturer group containing its platforms sorted newest-first.
 *
 * @property manufacturer Manufacturer name (e.g. "Sony").
 * @property platforms Platforms sorted by [PlatformEntry.productionYear] descending.
 */
data class ManufacturerGroup(
    val manufacturer: String,
    val platforms: List<PlatformEntry>,
)

/**
 * UI state for the Collection screen.
 *
 * @property manufacturerGroups Platform entries grouped by manufacturer, newest-first within each group.
 * @property games Games filtered by selected platform and search query.
 * @property accessories Accessories compatible with the selected platform's console.
 * @property searchQuery Current search query string.
 * @property selectedPlatform Currently selected platform name, or null for the overview.
 * @property isLoading Whether data is being loaded.
 */
data class CollectionUiState(
    val manufacturerGroups: List<ManufacturerGroup> = emptyList(),
    val games: List<Game> = emptyList(),
    val accessories: List<Accessory> = emptyList(),
    val searchQuery: String = "",
    val selectedPlatform: String? = null,
    val isLoading: Boolean = false,
)

/**
 * ViewModel for the Collection screen.
 * Combines games, consoles and accessories into a single reactive UI state.
 */
class CollectionViewModel(
    private val observeGamesUseCase: ObserveGamesUseCase,
    private val observeConsolesUseCase: ObserveConsolesUseCase,
    private val observeAccessoriesUseCase: ObserveAccessoriesUseCase,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedPlatform = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CollectionUiState> = combine(
        observeGamesUseCase(),
        observeConsolesUseCase(),
        observeAccessoriesUseCase(),
        _searchQuery,
        _selectedPlatform,
    ) { games, consoles, accessories, query, platform ->

        // Build platform entries — enrich with catalog when no Console entity exists
        val platformEntries = games
            .groupBy { it.platform }
            .map { (name, platformGames) ->
                val console = consoles.find { it.name.equals(name, ignoreCase = true) }
                val catalogInfo = IgdbPlatformCatalog.find(name)
                PlatformEntry(
                    name = name,
                    manufacturer = console?.manufacturer
                        ?: catalogInfo?.manufacturer
                        ?: "Autre",
                    productionYear = console?.productionYear
                        ?: catalogInfo?.releaseYear
                        ?: 0,
                    gameCount = platformGames.size,
                    coverUrls = platformGames.mapNotNull { it.coverUrl }.take(4),
                    console = console,
                )
            }

        // Group by manufacturer, sort groups alphabetically, platforms newest-first within each
        val manufacturerGroups = platformEntries
            .groupBy { it.manufacturer }
            .map { (manufacturer, entries) ->
                ManufacturerGroup(
                    manufacturer = manufacturer,
                    platforms = entries.sortedByDescending { it.productionYear },
                )
            }
            .sortedBy { it.manufacturer }

        val filteredGames = if (platform != null) {
            games.filter { game ->
                game.platform.equals(platform, ignoreCase = true) &&
                    game.title.contains(query, ignoreCase = true)
            }
        } else {
            emptyList()
        }

        val selectedConsole = consoles.find { it.name.equals(platform ?: "", ignoreCase = true) }
        val filteredAccessories = if (selectedConsole != null) {
            accessories.filter { acc ->
                acc.compatibleConsole.contains(selectedConsole.name, ignoreCase = true)
            }
        } else {
            emptyList()
        }

        CollectionUiState(
            manufacturerGroups = manufacturerGroups,
            games = filteredGames,
            accessories = filteredAccessories,
            searchQuery = query,
            selectedPlatform = platform,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CollectionUiState(isLoading = true),
    )

    /** Updates the search query used to filter games. */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /** Selects a platform to drill into its game list, or null to return to the overview. */
    fun onPlatformSelected(platform: String?) {
        _selectedPlatform.value = platform
        _searchQuery.value = ""
    }

    /** Legacy — kept for compatibility, delegates to [onPlatformSelected]. */
    fun onConsoleSelected(consoleId: String?) {
        // no-op: navigation is now platform-based
    }
}
