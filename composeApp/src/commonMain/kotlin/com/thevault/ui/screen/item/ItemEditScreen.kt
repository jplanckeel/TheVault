package com.thevault.ui.screen.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.ItemStatus
import com.thevault.ui.component.ButtonSize
import com.thevault.ui.component.Engrave
import com.thevault.ui.component.KeyButton
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.VaultButton
import com.thevault.ui.component.VaultChip
import com.thevault.ui.component.VaultTextField
import com.thevault.ui.component.label
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.copy.CopyFormSections
import org.koin.compose.viewmodel.koinViewModel

/**
 * Edits a console or an accessory.
 *
 * @param kind Console or accessory.
 * @param id Item identifier.
 * @param onBack Back navigation.
 * @param viewModel Screen ViewModel.
 */
@Composable
fun ItemEditScreen(
    kind: ItemKind,
    id: String,
    onBack: () -> Unit,
    viewModel: ItemEditViewModel = koinViewModel(),
) {
    LaunchedEffect(kind, id) { viewModel.load(kind, id) }
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeader("Modifier", eyebrow = kind.singular, leading = { KeyButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onBack) })
        VaultTextField("Nom", state.name, { v -> viewModel.update { it.copy(name = v) } })
        if (kind == ItemKind.Accessory) {
            VaultTextField("Console compatible", state.platform, { v -> viewModel.update { it.copy(platform = v) } })
        }
        Engrave("Statut")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ItemStatus.entries) { s -> VaultChip(s.label, s == state.status, { viewModel.update { it.copy(status = s) } }) }
        }
        CopyFormSections(state.draft, state.status) { d -> viewModel.update { it.copy(draft = d) } }
        VaultTextField(
            "Valeur manuelle",
            state.value,
            { v -> viewModel.update { it.copy(value = v) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            suffix = "€",
        )
        VaultButton(
            "Enregistrer",
            viewModel::save,
            Modifier.fillMaxWidth(),
            size = ButtonSize.Large,
            icon = Icons.Default.Check,
            enabled = state.canSave,
        )
    }
}
