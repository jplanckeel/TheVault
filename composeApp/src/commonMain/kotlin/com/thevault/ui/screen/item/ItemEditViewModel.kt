package com.thevault.ui.screen.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.Accessory
import com.thevault.domain.model.Console
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.usecase.GetAccessoryByIdUseCase
import com.thevault.domain.usecase.GetConsoleByIdUseCase
import com.thevault.domain.usecase.SaveAccessoryUseCase
import com.thevault.domain.usecase.SaveConsoleUseCase
import com.thevault.ui.screen.additem.toAmount
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.copy.CopyDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State of the console / accessory form.
 *
 * @property kind Console or accessory.
 * @property console Console being edited.
 * @property accessory Accessory being edited.
 * @property name Name.
 * @property platform Compatible console (accessories).
 * @property status Status.
 * @property value Manual value as typed.
 * @property draft Copy fields.
 * @property saved Whether the item was saved (screen closes).
 */
data class ItemEditUiState(
    val kind: ItemKind = ItemKind.Console,
    val console: Console? = null,
    val accessory: Accessory? = null,
    val name: String = "",
    val platform: String = "",
    val status: ItemStatus = ItemStatus.OWNED,
    val value: String = "",
    val draft: CopyDraft = CopyDraft(),
    val saved: Boolean = false,
) {
    /** Whether the form can be saved. */
    val canSave: Boolean get() = name.isNotBlank() && draft.datesValid && (console != null || accessory != null)
}

/** ViewModel of the console / accessory form. */
class ItemEditViewModel(
    private val getConsole: GetConsoleByIdUseCase,
    private val saveConsole: SaveConsoleUseCase,
    private val getAccessory: GetAccessoryByIdUseCase,
    private val saveAccessory: SaveAccessoryUseCase,
) : ViewModel() {
    private val state = MutableStateFlow(ItemEditUiState())

    /** Observable UI state. */
    val uiState: StateFlow<ItemEditUiState> = state.asStateFlow()

    /** Loads the item [id] of [kind]. */
    fun load(
        kind: ItemKind,
        id: String,
    ) {
        viewModelScope.launch {
            state.value =
                when (kind) {
                    ItemKind.Accessory -> getAccessory(id)?.let { it.toForm() } ?: ItemEditUiState(kind)
                    else -> getConsole(id)?.let { it.toForm() } ?: ItemEditUiState(kind)
                }
        }
    }

    /** Applies a change. */
    fun update(transform: (ItemEditUiState) -> ItemEditUiState) = state.update(transform)

    /** Saves the item. */
    fun save() {
        val s = state.value
        if (!s.canSave) return
        viewModelScope.launch {
            s.console?.let { saveConsole(s.applyTo(it)) }
            s.accessory?.let { saveAccessory(s.applyTo(it)) }
            state.update { it.copy(saved = true) }
        }
    }
}

private fun Console.toForm() =
    ItemEditUiState(
        ItemKind.Console,
        this,
        null,
        name,
        name,
        status,
        currentValue.plainValue(),
        CopyDraft.of(details, purchasePrice, acquiredAt),
    )

private fun Accessory.toForm() =
    ItemEditUiState(
        ItemKind.Accessory,
        null,
        this,
        name,
        compatibleConsole,
        status,
        currentValue.plainValue(),
        CopyDraft.of(details, purchasePrice, acquiredAt),
    )

private fun Double?.plainValue(): String =
    this
        ?.let {
            if (it % 1.0 ==
                0.0
            ) {
                it.toLong().toString()
            } else {
                it.toString().replace('.', ',')
            }
        }.orEmpty()

private fun ItemEditUiState.applyTo(c: Console) =
    c.copy(
        name = name.trim(),
        status = status,
        currentValue = value.toAmount(),
        purchasePrice = draft.purchasePrice,
        acquiredAt = draft.purchaseDate ?: c.acquiredAt,
        details = draft.toDetails(),
    )

private fun ItemEditUiState.applyTo(a: Accessory) =
    a.copy(
        name = name.trim(),
        compatibleConsole = platform.trim(),
        status = status,
        currentValue = value.toAmount(),
        purchasePrice = draft.purchasePrice,
        acquiredAt = draft.purchaseDate ?: a.acquiredAt,
        details = draft.toDetails(),
    )
