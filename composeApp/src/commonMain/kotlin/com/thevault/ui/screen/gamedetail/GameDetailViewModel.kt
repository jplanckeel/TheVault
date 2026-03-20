package com.thevault.ui.screen.gamedetail

import androidx.lifecycle.ViewModel
import com.thevault.domain.model.Game
import androidx.lifecycle.viewModelScope
import com.thevault.domain.usecase.DeleteGameUseCase
import com.thevault.domain.usecase.GetGameByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameDetailUiState(
    val game: Game? = null,
    val isLoading: Boolean = false,
    val isDeleted: Boolean = false,
)

class GameDetailViewModel(
    private val getGameByIdUseCase: GetGameByIdUseCase,
    private val deleteGameUseCase: DeleteGameUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameDetailUiState())
    val uiState: StateFlow<GameDetailUiState> = _uiState.asStateFlow()

    fun loadGame(gameId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val game = getGameByIdUseCase(gameId)
            _uiState.update { it.copy(game = game, isLoading = false) }
        }
    }

    fun deleteGame() {
        viewModelScope.launch {
            val gameId = _uiState.value.game?.id ?: return@launch
            deleteGameUseCase(gameId)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }
}

