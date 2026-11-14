package com.thevault.ui.screen.additem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import com.thevault.ui.component.ButtonSize
import com.thevault.ui.component.Engrave
import com.thevault.ui.component.KeyButton
import com.thevault.ui.component.ListRow
import com.thevault.ui.component.Panel
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.VaultButton
import com.thevault.ui.component.VaultChip
import com.thevault.ui.component.VaultTextField
import com.thevault.ui.component.label
import com.thevault.ui.component.shortPlatformName
import com.thevault.ui.screen.copy.CopyFormSections
import com.thevault.ui.theme.Vault
import org.koin.compose.viewmodel.koinViewModel

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
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeader(
            if (gameId != null) "Modifier" else "Nouveau jeu",
            eyebrow = "Fiche jeu",
            leading = { KeyButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onBack) },
        )
        IdentityFields(state, viewModel)
        Choices(state, viewModel)
        PriceFields(state, viewModel)
        VaultTextField("Notes", state.notes, { v ->
            viewModel.update {
                it.copy(notes = v)
            }
        }, placeholder = "Provenance, défauts…", singleLine = false)
        VaultButton(
            if (state.isSaving) "Enregistrement…" else "Enregistrer",
            viewModel::saveItem,
            Modifier.fillMaxWidth(),
            size = ButtonSize.Large,
            icon = Icons.Default.Check,
            enabled = state.canSave,
        )
    }
}

@Composable
private fun IdentityFields(
    state: AddItemUiState,
    viewModel: AddItemViewModel,
) {
    VaultTextField("Titre", state.title, viewModel::onTitleChange, placeholder = "Rechercher sur IGDB…")
    if (state.isSearching) Text("Recherche…", style = Vault.type.caption, color = Vault.colors.inkMuted)
    state.searchError?.let { Text(it, style = Vault.type.caption, color = Vault.colors.loss) }
    Suggestions(state, viewModel)
    if (state.availablePlatforms.size > 1) {
        Engrave("Plateforme")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.availablePlatforms) { p ->
                VaultChip(shortPlatformName(p), state.platform == p, {
                    viewModel.update { it.copy(platform = p) }
                })
            }
        }
    } else {
        VaultTextField(
            "Plateforme",
            state.platform,
            { v -> viewModel.update { it.copy(platform = v) } },
            placeholder = "Super Nintendo, PS2…",
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VaultTextField("Genre", state.genre, { v -> viewModel.update { it.copy(genre = v) } }, Modifier.weight(1f))
        VaultTextField(
            "Année",
            state.releaseYear,
            { v -> viewModel.update { it.copy(releaseYear = v.filter(Char::isDigit).take(YEAR_DIGITS)) } },
            Modifier.weight(0.6f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
}

@Composable
private fun Suggestions(
    state: AddItemUiState,
    viewModel: AddItemViewModel,
) {
    if (state.searchResults.isNotEmpty()) {
        Panel(Modifier.fillMaxWidth()) {
            state.searchResults.take(MAX_SUGGESTIONS).forEach { r ->
                val platform = r.platformNames.firstOrNull().orEmpty()
                ListRow(
                    r.title,
                    meta =
                        listOfNotNull(
                            shortPlatformName(platform),
                            r.releaseYear?.toString(),
                        ).joinToString(" · "),
                    cover = Triple(r.coverUrl, platform, null),
                    onClick = {
                        viewModel.selectGame(r)
                    },
                )
            }
        }
    }
}

@Composable
private fun Choices(
    state: AddItemUiState,
    viewModel: AddItemViewModel,
) {
    ChipGroup("Statut", ItemStatus.entries, state.status, { it.label }) { s -> viewModel.update { it.copy(status = s) } }
    ChipGroup("Région", Region.entries, state.region, { it.label }) { r -> viewModel.update { it.copy(region = r) } }
    CopyFormSections(state.draft, state.status) { d -> viewModel.update { it.copy(draft = d) } }
}

@Composable
private fun <T> ChipGroup(
    label: String,
    options: List<T>,
    selected: T,
    text: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Engrave(label)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { o -> VaultChip(text(o), o == selected, { onSelect(o) }) }
    }
}

@Composable
private fun PriceFields(
    state: AddItemUiState,
    viewModel: AddItemViewModel,
) {
    VaultTextField(
        "Valeur manuelle",
        state.currentValue,
        { v -> viewModel.update { it.copy(currentValue = v) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        suffix = "€",
    )
    Text("La cote eBay remplace la valeur manuelle dès qu'elle est disponible.", style = Vault.type.caption, color = Vault.colors.inkMuted)
}

private const val MAX_SUGGESTIONS = 5
private const val YEAR_DIGITS = 4
