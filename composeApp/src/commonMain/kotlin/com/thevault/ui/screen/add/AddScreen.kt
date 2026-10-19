package com.thevault.ui.screen.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.RemoteGame
import com.thevault.ui.component.Eyebrow
import com.thevault.ui.component.IconSquareButton
import com.thevault.ui.component.ItemCover
import com.thevault.ui.component.SegmentedControl
import com.thevault.ui.component.VaultBadge
import com.thevault.ui.component.VaultFilterChip
import com.thevault.ui.component.VaultSearchField
import com.thevault.ui.component.platformBadgeColors
import com.thevault.ui.component.shortPlatformName
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.platformSummaries
import com.thevault.ui.theme.FormatBadgeColors
import com.thevault.ui.theme.OnPrimary
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.SurfaceContainerHigh
import com.thevault.ui.theme.VaultRadius
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
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconSquareButton(Icons.Default.Close, "Fermer", onClose)
                Text("Ajouter", style = MaterialTheme.typography.headlineLarge, color = OnSurface)
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
            VaultSearchField(state.query, viewModel::onQueryChanged, hint, Modifier.fillMaxWidth(), focus)
        }
        item { SegmentedControl(ItemKind.entries, state.kind, { it.singular }, viewModel::onKindSelected) }
        if (state.kind == ItemKind.Game) gameResults(state, keys, viewModel) else manualCandidate(state, entries, viewModel)
    }
    state.candidate?.let { candidate ->
        val actions =
            QuickAddActions(viewModel::onFormChanged, viewModel::dismiss) { details ->
                viewModel.save { gameId -> if (details && gameId != null) onAddDetails(gameId) else onClose() }
            }
        QuickAddSheet(candidate, state.form, entries, actions)
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("Plateforme")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { VaultFilterChip("Toutes", state.platform == null, { viewModel.onPlatformSelected(null) }) }
                    items(platforms) { p ->
                        VaultFilterChip(
                            shortPlatformName(p),
                            state.platform == p,
                            { viewModel.onPlatformSelected(p) },
                            dot = platformBadgeColors(p).content,
                        )
                    }
                }
            }
        }
    }
    val visible = state.results.filter { state.platform == null || state.platform in it.platformNames }
    when {
        state.isSearching ->
            item {
                Box(
                    Modifier.fillMaxWidth().padding(24.dp),
                    Alignment.Center,
                ) { CircularProgressIndicator(color = Primary) }
            }
        state.query.trim().length >= MIN_QUERY_LENGTH -> {
            item { Eyebrow("${visible.size} résultats IGDB") }
            items(visible, key = { it.id }) { result ->
                ResultRow(result, state.platform, result.isOwned(keys)) {
                    viewModel.open(AddCandidate(ItemKind.Game, result.title, result.platformNames, result))
                }
            }
        }
    }
}

private const val MIN_QUERY_LENGTH = 3

private fun LazyListScope.manualCandidate(
    state: AddUiState,
    entries: List<CollectionEntry>,
    viewModel: AddViewModel,
) {
    if (state.query.isBlank()) return
    val platforms = platformSummaries(entries.filter { it.kind == ItemKind.Game }).map { it.platform }
    item {
        ResultRow(null, null, owned = false, title = "Ajouter « ${state.query.trim()} »", subtitle = state.kind.singular) {
            viewModel.open(AddCandidate(state.kind, state.query.trim(), platforms))
        }
    }
}

@Composable
private fun ResultRow(
    result: RemoteGame?,
    platformFilter: String?,
    owned: Boolean,
    title: String = result?.title.orEmpty(),
    subtitle: String? = null,
    onAdd: () -> Unit,
) {
    val platform = platformFilter ?: result?.platformNames?.firstOrNull().orEmpty()
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = !owned, onClick = onAdd).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ItemCover(result?.coverUrl, platform, title, Modifier.size(40.dp, 52.dp), VaultRadius.md)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = OnSurface, maxLines = 2)
                val line =
                    subtitle
                        ?: listOfNotNull(shortPlatformName(platform).ifBlank { null }, result?.releaseYear?.toString()).joinToString(" · ")
                Text(line, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            }
            if (owned) {
                VaultBadge("Possédé", FormatBadgeColors.New)
            } else {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(VaultRadius.lg)
                        .background(Primary)
                        .clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter", tint = OnPrimary)
                }
            }
        }
        HorizontalDivider(color = SurfaceContainerHigh)
    }
}
