package com.thevault.ui.screen.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.Console
import com.thevault.domain.usecase.ObserveConsolesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ConsoleUiState(
    val consoles: List<Console> = emptyList(),
    val isLoading: Boolean = false,
)

class ConsoleViewModel(
    private val observeConsolesUseCase: ObserveConsolesUseCase,
) : ViewModel() {

    val uiState: StateFlow<ConsoleUiState> = observeConsolesUseCase()
        .map { consoles -> ConsoleUiState(consoles = consoles, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConsoleUiState(isLoading = true),
        )
}
