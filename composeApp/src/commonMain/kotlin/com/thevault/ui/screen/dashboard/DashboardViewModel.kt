package com.thevault.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.Game
import com.thevault.domain.model.VaultStats
import com.thevault.domain.usecase.ObserveGamesUseCase
import com.thevault.domain.usecase.ObserveVaultStatsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State of the Dashboard screen.
 *
 * @property stats Use case provided vault statistics.
 * @property latestGames List of the most recently added games.
 * @property topRatedGames List of the highest rated games.
 * @property isLoading Whether the data is being loaded.
 */
data class DashboardUiState(
    val stats: VaultStats = VaultStats(0, 0, 0, 0, 0.0, 0.0),
    val latestGames: List<Game> = emptyList(),
    val topRatedGames: List<Game> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
)

/**
 * ViewModel for the Dashboard screen.
 * Observes the vault statistics and maps them to the UI state.
 *
 * @property observeVaultStatsUseCase The use case to observe vault stats.
 */
class DashboardViewModel(
    private val observeVaultStatsUseCase: ObserveVaultStatsUseCase,
    private val observeGamesUseCase: ObserveGamesUseCase,
    private val csvService: com.thevault.domain.usecase.CsvService,
    private val syncVaultWithIgdbUseCase: com.thevault.domain.usecase.SyncVaultWithIgdbUseCase,
) : ViewModel() {

    private val _message = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val uiState: StateFlow<DashboardUiState> = kotlinx.coroutines.flow.combine(
        observeVaultStatsUseCase(),
        observeGamesUseCase(),
        _message
    ) { stats, games, message ->
        val latest = games.sortedByDescending { it.acquiredAt }.take(3)
        val topRated = games.filter { it.rating != null && it.rating!! > 0 }
            .sortedByDescending { it.rating }
            .take(3)
        
        DashboardUiState(
            stats = stats,
            latestGames = latest,
            topRatedGames = topRated,
            isLoading = false,
            message = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true),
    )

    fun importCsv(content: String) {
        _message.value = "Importation en cours..."
        viewModelScope.launch {
            val result = csvService.importCsv(content)
            if (result.isSuccess) {
                _message.value = "Importation réussie !"
            } else {
                _message.value = "Erreur lors de l'importation."
            }
        }
    }

    fun exportCsv(onExported: (String) -> Unit) {
        viewModelScope.launch {
            val result = csvService.exportCsv()
            result.onSuccess {
                onExported(it)
                _message.value = "Exportation prête !"
            }.onFailure {
                _message.value = "Erreur lors de l'exportation."
            }
        }
    }

    fun syncIgdb() {
        _message.value = "Synchronisation IGDB en cours... (Cela peut prendre un peu de temps)"
        viewModelScope.launch {
            val result = syncVaultWithIgdbUseCase()
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _message.value = "Synchronisation terminée ! $count jeu(x) mis à jour."
            } else {
                _message.value = "Erreur de synchronisation avec IGDB."
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
