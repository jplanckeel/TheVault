package com.thevault.ui.screen.accessory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.Accessory
import com.thevault.domain.usecase.ObserveAccessoriesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * UI state for the Accessory list screen.
 *
 * @property accessories The list of accessories to display.
 * @property isLoading Whether data is being fetched.
 */
data class AccessoryUiState(
    val accessories: List<Accessory> = emptyList(),
    val isLoading: Boolean = false,
)

/**
 * ViewModel for the Accessory list screen.
 * Observes all accessories and exposes them as UI state.
 */
class AccessoryViewModel(
    private val observeAccessoriesUseCase: ObserveAccessoriesUseCase,
) : ViewModel() {

    val uiState: StateFlow<AccessoryUiState> = observeAccessoriesUseCase()
        .map { accessories -> AccessoryUiState(accessories = accessories, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccessoryUiState(isLoading = true),
        )
}
