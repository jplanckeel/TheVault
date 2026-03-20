package com.thevault.ui.screen.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.Console
import com.thevault.domain.usecase.GetConsoleByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the ConsoleDetail screen.
 *
 * @property console The console being displayed, null while loading.
 * @property isLoading Whether data is being fetched.
 */
data class ConsoleDetailUiState(
    val console: Console? = null,
    val isLoading: Boolean = false,
)

/**
 * ViewModel for the Console Detail screen.
 * Loads and exposes a single [Console] by ID.
 */
class ConsoleDetailViewModel(
    private val getConsoleByIdUseCase: GetConsoleByIdUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsoleDetailUiState())
    val uiState: StateFlow<ConsoleDetailUiState> = _uiState.asStateFlow()

    /** Loads the console with the given [consoleId] from the repository. */
    fun loadConsole(consoleId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val console = getConsoleByIdUseCase(consoleId)
            _uiState.update { it.copy(console = console, isLoading = false) }
        }
    }
}
