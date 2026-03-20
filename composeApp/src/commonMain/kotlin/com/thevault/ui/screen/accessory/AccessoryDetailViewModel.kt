package com.thevault.ui.screen.accessory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.Accessory
import com.thevault.domain.usecase.GetAccessoryByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the AccessoryDetail screen.
 *
 * @property accessory The accessory being displayed, null while loading.
 * @property isLoading Whether data is being fetched.
 */
data class AccessoryDetailUiState(
    val accessory: Accessory? = null,
    val isLoading: Boolean = false,
)

/**
 * ViewModel for the Accessory Detail screen.
 * Loads and exposes a single [Accessory] by ID.
 */
class AccessoryDetailViewModel(
    private val getAccessoryByIdUseCase: GetAccessoryByIdUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessoryDetailUiState())
    val uiState: StateFlow<AccessoryDetailUiState> = _uiState.asStateFlow()

    /** Loads the accessory with the given [accessoryId] from the repository. */
    fun loadAccessory(accessoryId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val accessory = getAccessoryByIdUseCase(accessoryId)
            _uiState.update { it.copy(accessory = accessory, isLoading = false) }
        }
    }
}
