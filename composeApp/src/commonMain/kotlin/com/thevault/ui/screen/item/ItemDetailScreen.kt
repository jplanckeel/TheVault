package com.thevault.ui.screen.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.PriceGrade
import com.thevault.domain.model.priceGrade
import com.thevault.ui.component.ButtonVariant
import com.thevault.ui.component.CopyPanel
import com.thevault.ui.component.CopyPurchase
import com.thevault.ui.component.DeleteConfirmDialog
import com.thevault.ui.component.DetailHeader
import com.thevault.ui.component.DetailTopBar
import com.thevault.ui.component.LoadingBox
import com.thevault.ui.component.MarketSection
import com.thevault.ui.component.MenuEntry
import com.thevault.ui.component.StatusDialog
import com.thevault.ui.component.VaultButton
import com.thevault.ui.component.label
import com.thevault.ui.component.needsDetails
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemActions
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.ValueSource
import com.thevault.ui.screen.collection.VaultEntriesSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * UI state of a console or accessory detail.
 *
 * @property entry Item, null while loading or once deleted.
 * @property grade Grade shown in the « Cote eBay » section.
 * @property deleted Whether the item was deleted.
 */
data class ItemDetailUiState(
    val entry: CollectionEntry? = null,
    val grade: PriceGrade = PriceGrade.CIB,
    val deleted: Boolean = false,
)

/** ViewModel of a console or accessory detail. */
class ItemDetailViewModel(
    source: VaultEntriesSource,
    private val actions: ItemActions,
) : ViewModel() {
    private val target = MutableStateFlow<Pair<ItemKind, String>?>(null)
    private val grade = MutableStateFlow<PriceGrade?>(null)
    private val deleted = MutableStateFlow(false)

    /** Observable UI state. */
    val uiState: StateFlow<ItemDetailUiState> =
        combine(source.entries(), target, grade, deleted) { all, t, g, d ->
            val entry = t?.let { (kind, id) -> all.find { it.kind == kind && it.id == id } }
            ItemDetailUiState(entry, g ?: entry?.condition?.priceGrade ?: PriceGrade.CIB, d)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ItemDetailUiState())

    /** Shows the item [id] of [kind]. */
    fun load(
        kind: ItemKind,
        id: String,
    ) {
        target.value = kind to id
    }

    /** Shows the quote of [value]. */
    fun onGradeSelected(value: PriceGrade) {
        grade.value = value
    }

    /** Changes the status. */
    fun setStatus(status: ItemStatus) {
        val (kind, id) = target.value ?: return
        viewModelScope.launch { actions.setStatus(kind, id, status) }
    }

    /** Deletes the item. */
    fun delete() {
        val (kind, id) = target.value ?: return
        viewModelScope.launch {
            actions.delete(kind, id)
            deleted.value = true
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/**
 * Console or accessory detail.
 *
 * @param kind Console or accessory.
 * @param id Item identifier.
 * @param onBack Back navigation.
 * @param onEdit Opens the form.
 * @param viewModel Screen ViewModel.
 */
@Composable
fun ItemDetailScreen(
    kind: ItemKind,
    id: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: ItemDetailViewModel = koinViewModel(),
) {
    LaunchedEffect(kind, id) { viewModel.load(kind, id) }
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    var statusDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    val entry = state.entry ?: return LoadingBox()
    if (statusDialog) {
        StatusDialog(entry.status, {
            viewModel.setStatus(it)
            statusDialog = false
            if (it.needsDetails) onEdit()
        }, { statusDialog = false })
    }
    if (deleteDialog) {
        DeleteConfirmDialog(entry.title, {
            deleteDialog = false
            viewModel.delete()
        }, { deleteDialog = false })
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DetailTopBar(
            onBack,
            caption = "${kind.singular} · ${entry.status.label}",
            menu =
                listOf(
                    MenuEntry("Modifier", onClick = onEdit),
                    MenuEntry("Changer le statut", onClick = { statusDialog = true }),
                    MenuEntry("Supprimer", destructive = true, onClick = { deleteDialog = true }),
                ),
        )
        val credits = listOf(entry.manufacturer.label, entry.subtype).filter { it.isNotBlank() && it != "Autres" }.joinToString(" · ")
        DetailHeader(entry.title, entry.platform, entry.region, entry.details.regionVariant, entry.coverUrl, credits, fromIgdb = false)
        CopyPanel(
            entry.details,
            CopyPurchase(
                entry.purchasePrice,
                entry.acquiredAt,
                entry.value,
                entry.valueSource == ValueSource.UsIndicative,
            ),
        )
        MarketSection(entry.quotes, state.grade, emptyList(), viewModel::onGradeSelected, entry.usPrice)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VaultButton("Statut", { statusDialog = true }, Modifier.weight(1f), ButtonVariant.Secondary)
            VaultButton("Modifier", onEdit, Modifier.weight(1f))
        }
    }
}
