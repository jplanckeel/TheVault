package com.thevault.ui.screen.gamedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.usecase.DeleteGameUseCase
import com.thevault.domain.usecase.GetGameByIdUseCase
import com.thevault.domain.usecase.SaveGameUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state of the game detail.
 *
 * @property game Loaded game, null while loading or when missing.
 * @property isLoading Whether the game is loading.
 * @property isDeleted Whether the game was deleted (screen closes).
 */
data class GameDetailUiState(
    val game: Game? = null,
    val isLoading: Boolean = false,
    val isDeleted: Boolean = false,
)

/** ViewModel of the game detail. */
class GameDetailViewModel(
    private val getGameById: GetGameByIdUseCase,
    private val deleteGame: DeleteGameUseCase,
    private val saveGame: SaveGameUseCase,
) : ViewModel() {
    private val state = MutableStateFlow(GameDetailUiState())

    /** Observable UI state. */
    val uiState: StateFlow<GameDetailUiState> = state.asStateFlow()

    /** Loads the game [gameId]. */
    fun loadGame(gameId: String) {
        viewModelScope.launch {
            state.update { it.copy(isLoading = true) }
            val game = getGameById(gameId)
            state.update { it.copy(game = game, isLoading = false) }
        }
    }

    /** Changes the status (« Vendu / prêté »). */
    fun setStatus(status: ItemStatus) {
        val game = state.value.game ?: return
        viewModelScope.launch {
            val updated = game.copy(status = status)
            saveGame(updated)
            state.update { it.copy(game = updated) }
        }
    }

    /** Deletes the game. */
    fun delete() {
        val id = state.value.game?.id ?: return
        viewModelScope.launch {
            deleteGame(id)
            state.update { it.copy(isDeleted = true) }
        }
    }
}
