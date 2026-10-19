package com.thevault.ui.screen.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.Console
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.usecase.GetConsoleByIdUseCase
import com.thevault.ui.screen.collection.ItemActions
import com.thevault.ui.screen.collection.ItemKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state of the console detail.
 *
 * @property console Loaded console.
 * @property isLoading Whether data is loading.
 */
data class ConsoleDetailUiState(
    val console: Console? = null,
    val isLoading: Boolean = false,
)

/** ViewModel of the console detail. */
class ConsoleDetailViewModel(
    private val getConsoleById: GetConsoleByIdUseCase,
    private val actions: ItemActions,
) : ViewModel() {
    private val state = MutableStateFlow(ConsoleDetailUiState())

    /** Observable UI state. */
    val uiState: StateFlow<ConsoleDetailUiState> = state.asStateFlow()

    /** Loads the console [consoleId]. */
    fun loadConsole(consoleId: String) {
        viewModelScope.launch {
            state.update { it.copy(isLoading = true) }
            val console = getConsoleById(consoleId)
            state.update { it.copy(console = console, isLoading = false) }
        }
    }

    /** Changes the status (« Vendu / prêté »). */
    fun setStatus(status: ItemStatus) {
        val console = state.value.console ?: return
        viewModelScope.launch {
            actions.setStatus(ItemKind.Console, console.id, status)
            state.update { it.copy(console = console.copy(status = status)) }
        }
    }
}
