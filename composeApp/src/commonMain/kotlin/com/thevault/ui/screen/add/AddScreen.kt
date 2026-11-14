package com.thevault.ui.screen.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.RemoteGame
import com.thevault.ui.component.EmptyState
import com.thevault.ui.component.Engrave
import com.thevault.ui.component.KeyButton
import com.thevault.ui.component.ListRow
import com.thevault.ui.component.LoadingBox
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.SearchField
import com.thevault.ui.component.VaultChip
import com.thevault.ui.component.shortPlatformName
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.platformSummaries
import com.thevault.ui.theme.Vault
import org.koin.compose.viewmodel.koinViewModel

/**
 * Add screen: search IGDB (games) or type a console / accessory name, then confirm in a sheet.
 *
 * @param onClose Closes the screen.
 * @param onAddDetails Opens the full form of a game just added.
 * @param viewModel Screen ViewModel.
 */
@Composable
fun AddScreen(
    onClose: () -> Unit,
    onAddDetails: (String) -> Unit,
    viewModel: AddViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val keys = remember(entries) { ownedKeys(entries) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LazyColumn(
        Modifier.fillMaxSize().imePadding().windowInsetsPadding(WindowInsets.navigationBars),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader("Ajouter", eyebrow = if (state.kind == ItemKind.Game) "Recherche IGDB" else "Saisie libre") {
                KeyButton(Icons.Default.Close, "Fermer", onClose)
            }
        }
        item {
            val hint =
                if (state.kind ==
                    ItemKind.Game
                ) {
                    "Titre du jeu"
                } else {
                    "Nom ${if (state.kind == ItemKind.Console) "de la console" else "de l'accessoire"}"
                }
            SearchField(state.query, viewModel::onQueryChanged, hint, Modifier.fillMaxWidth(), focus)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ItemKind.entries) { k -> VaultChip(k.label, k == state.kind, { viewModel.onKindSelected(k) }) }
            }
        }
        if (state.kind == ItemKind.Game) gameResults(state, keys, viewModel) else manualCandidate(state, entries, viewModel)
    }
    AddSheet(state, viewModel, onClose, onAddDetails)
}

@Composable
private fun AddSheet(
    state: AddUiState,
    viewModel: AddViewModel,
    onClose: () -> Unit,
    onAddDetails: (String) -> Unit,
) {
    state.candidate?.let { candidate ->
        QuickAddSheet(
            candidate,
            state.form,
            QuickAddActions(viewModel::onFormChanged, viewModel::dismiss) { status, details ->
                viewModel.save(status) { gameId -> if (details && gameId != null) onAddDetails(gameId) else onClose() }
            },
        )
    }
}

private fun LazyListScope.gameResults(
    state: AddUiState,
    keys: Set<String>,
    viewModel: AddViewModel,
) {
    val platforms = state.results.flatMap { it.platformNames }.distinct()
    if (platforms.size > 1) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { VaultChip("Toutes", state.platform == null, { viewModel.onPlatformSelected(null) }) }
                items(platforms) { p -> VaultChip(shortPlatformName(p), state.platform == p, { viewModel.onPlatformSelected(p) }) }
            }
        }
    }
    val visible = state.results.filter { state.platform == null || state.platform in it.platformNames }
    when {
        state.isSearching -> item { LoadingBox() }
        state.error != null -> item { EmptyState(Icons.Default.CloudOff, "Hors ligne", message = state.error) }
        state.query.trim().length >= MIN_QUERY_LENGTH -> {
            item { Engrave("${visible.size} résultats") }
            items(visible, key = { it.id }) { result -> ResultRow(result, state.platform, result.isOwned(keys), viewModel) }
        }
    }
}

@Composable
private fun ResultRow(
    result: RemoteGame,
    platformFilter: String?,
    owned: Boolean,
    viewModel: AddViewModel,
) {
    val platform = platformFilter ?: result.platformNames.firstOrNull().orEmpty()
    val meta =
        listOfNotNull(
            shortPlatformName(platform).ifBlank { null },
            result.releaseYear?.toString(),
            result.developer,
        ).joinToString(" · ")
    ListRow(
        title = result.title,
        meta = meta,
        cover = Triple(result.coverUrl, platform, null),
        onClick = { viewModel.open(AddCandidate(ItemKind.Game, result.title, result.platformNames, result)) },
        trailingContent = {
            if (owned) {
                Text("Possédé", style = Vault.type.monoTag, color = Vault.colors.ledSync)
            } else {
                Icon(Icons.Default.Add, contentDescription = "Ajouter", tint = Vault.colors.indigoInk)
            }
        },
    )
}

private fun LazyListScope.manualCandidate(
    state: AddUiState,
    entries: List<CollectionEntry>,
    viewModel: AddViewModel,
) {
    if (state.query.isBlank()) return
    val platforms = platformSummaries(entries.filter { it.kind == ItemKind.Game }).map { it.platform }
    item {
        ListRow(
            title = "Ajouter « ${state.query.trim()} »",
            meta = state.kind.singular,
            onClick = { viewModel.open(AddCandidate(state.kind, state.query.trim(), platforms)) },
            trailingContent = { Icon(Icons.Default.Add, contentDescription = null, tint = Vault.colors.indigoInk) },
        )
    }
}

private const val MIN_QUERY_LENGTH = 3
