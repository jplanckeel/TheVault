package com.thevault.ui.screen.additem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.ItemCondition
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import com.thevault.ui.component.VaultButton
import com.thevault.ui.component.VaultButtonSize
import com.thevault.ui.component.VaultCard
import com.thevault.ui.component.label
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Primary
import org.koin.compose.viewmodel.koinViewModel

private val FormConditions =
    listOf(ItemCondition.LOOSE, ItemCondition.CIB_NO_MANUAL, ItemCondition.CIB, ItemCondition.NEW_SEALED, ItemCondition.MINT)
private val FormStatuses = listOf(ItemStatus.OWNED, ItemStatus.WISHLIST, ItemStatus.LOANED, ItemStatus.SOLD)

/** Add (or edit when [gameId] is set) a game. */
@Composable
fun AddItemScreen(
    gameId: String? = null,
    onBack: () -> Unit,
    viewModel: AddItemViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(gameId) { if (gameId != null) viewModel.loadGame(gameId) }
    LaunchedEffect(state.isSuccess) { if (state.isSuccess) onBack() }
    AddItemContent(
        state = state,
        isEdit = gameId != null,
        actions =
            AddItemActions(
                onBack = onBack,
                onTitleChange = viewModel::onTitleChange,
                onSelectRemote = viewModel::selectGame,
                onPlatformChange = viewModel::onPlatformChange,
                onStatusChange = viewModel::onStatusChange,
                onConditionChange = viewModel::onConditionChange,
                onRegionChange = viewModel::onRegionChange,
                onPriceChange = viewModel::onPriceChange,
                onValueChange = viewModel::onValueChange,
                onRatingChange = viewModel::onRatingChange,
                onNotesChange = viewModel::onNotesChange,
                onSave = viewModel::saveItem,
            ),
    )
}

/**
 * Stateless add / edit form.
 *
 * @param state Form state.
 * @param isEdit Whether an existing game is being edited.
 * @param actions User callbacks.
 */
@Composable
fun AddItemContent(
    state: AddItemUiState,
    isEdit: Boolean,
    actions: AddItemActions,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        FormHeader(isEdit, actions.onBack)
        TitleField(state, actions)
        if (state.searchResults.isNotEmpty()) RemoteResults(state.searchResults, actions.onSelectRemote)
        PlatformField(state, actions)
        MetadataSummary(state)
        ChoiceField("Statut", FormStatuses, state.status, { it.label }, actions.onStatusChange)
        ChoiceField("État", FormConditions, state.condition, { it.label }, actions.onConditionChange)
        ChoiceField("Région", Region.entries, state.region, { it.label }, actions.onRegionChange)
        PriceFields(state, actions)
        RatingField(state.rating, actions.onRatingChange)
        FormField("Notes", state.notes, actions.onNotesChange, singleLine = false, placeholder = "Édition, défauts, provenance…")
        SaveButton(state, isEdit, actions.onSave)
    }
}

@Composable
private fun FormHeader(
    isEdit: Boolean,
    onBack: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = OnSurface) }
        Text(if (isEdit) "Modifier le jeu" else "Ajouter un jeu", style = MaterialTheme.typography.headlineSmall, color = OnSurface)
    }
}

@Composable
private fun TitleField(
    state: AddItemUiState,
    actions: AddItemActions,
) {
    FormField(
        label = "Titre",
        value = state.title,
        onValueChange = actions.onTitleChange,
        placeholder = "Rechercher sur IGDB…",
        trailing = {
            if (state.isSearching) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Primary, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Search, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        },
    )
}

@Composable
private fun PriceFields(
    state: AddItemUiState,
    actions: AddItemActions,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FormField(
            "Prix d'achat (€)",
            state.purchasePrice,
            actions.onPriceChange,
            Modifier.weight(1f),
            keyboardType = KeyboardType.Decimal,
        )
        FormField(
            "Valeur estimée (€)",
            state.currentValue,
            actions.onValueChange,
            Modifier.weight(1f),
            keyboardType = KeyboardType.Decimal,
        )
    }
}

@Composable
private fun SaveButton(
    state: AddItemUiState,
    isEdit: Boolean,
    onSave: () -> Unit,
) {
    val label =
        when {
            state.isSaving -> "Enregistrement…"
            isEdit -> "Enregistrer"
            else -> "Ajouter au vault"
        }
    VaultButton(
        text = label,
        onClick = onSave,
        enabled = !state.isSaving && state.title.isNotBlank() && state.platform.isNotBlank(),
        size = VaultButtonSize.Large,
        leadingIcon = Icons.Default.Check,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PlatformField(
    state: AddItemUiState,
    actions: AddItemActions,
) {
    if (state.availablePlatforms.size > 1) {
        ChoiceField("Plateforme", state.availablePlatforms, state.platform, { it }, actions.onPlatformChange)
    } else {
        FormField("Plateforme", state.platform, actions.onPlatformChange, placeholder = "PlayStation 2, Switch…")
    }
}

@Composable
private fun MetadataSummary(state: AddItemUiState) {
    val rows =
        listOf("Développeur" to state.developer, "Éditeur" to state.publisher, "Modes" to state.gameModes, "Thèmes" to state.themes)
            .filter { it.second.isNotBlank() }
    if (rows.isEmpty()) return
    VaultCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { (label, value) ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(0.4f)) { FieldLabel(label) }
                    Text(value, style = MaterialTheme.typography.bodyMedium, color = OnSurface, modifier = Modifier.weight(0.6f))
                }
            }
        }
    }
}
