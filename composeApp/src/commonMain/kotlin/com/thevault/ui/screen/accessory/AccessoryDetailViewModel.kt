package com.thevault.ui.screen.accessory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.Accessory
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.usecase.GetAccessoryByIdUseCase
import com.thevault.ui.screen.collection.ItemActions
import com.thevault.ui.screen.collection.ItemKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state of the accessory detail.
 *
 * @property accessory Loaded accessory.
 * @property isLoading Whether data is loading.
 */
data class AccessoryDetailUiState(
    val accessory: Accessory? = null,
    val isLoading: Boolean = false,
)

/** ViewModel of the accessory detail. */
class AccessoryDetailViewModel(
    private val getAccessoryById: GetAccessoryByIdUseCase,
    private val actions: ItemActions,
) : ViewModel() {
    private val state = MutableStateFlow(AccessoryDetailUiState())

    /** Observable UI state. */
    val uiState: StateFlow<AccessoryDetailUiState> = state.asStateFlow()

    /** Loads the accessory [accessoryId]. */
    fun loadAccessory(accessoryId: String) {
        viewModelScope.launch {
            state.update { it.copy(isLoading = true) }
            val accessory = getAccessoryById(accessoryId)
            state.update { it.copy(accessory = accessory, isLoading = false) }
        }
    }

    /** Changes the status (« Vendu / prêté »). */
    fun setStatus(status: ItemStatus) {
        val accessory = state.value.accessory ?: return
        viewModelScope.launch {
            actions.setStatus(ItemKind.Accessory, accessory.id, status)
            state.update { it.copy(accessory = accessory.copy(status = status)) }
        }
    }
}
