package com.thevault.ui.screen.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.EmptyState
import com.thevault.ui.component.FormatGroup
import com.thevault.ui.component.IconSquareButton
import com.thevault.ui.component.VaultButton
import com.thevault.ui.component.VaultButtonSize
import com.thevault.ui.component.VaultButtonVariant
import com.thevault.ui.component.VaultFilterChip
import com.thevault.ui.component.VaultSearchField
import com.thevault.ui.component.formatEuro
import com.thevault.ui.component.gameCount
import com.thevault.ui.component.shortPlatformName
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.SurfaceContainerHigh
import org.koin.compose.viewmodel.koinViewModel

private val FormatFilters = listOf(FormatGroup.Cib, FormatGroup.Loose, FormatGroup.New)

/**
 * Games of one platform (e.g. « GameCube · 77 jeux · 2 989 € »), or of every platform.
 *
 * @param platform Platform to show, or null for every game.
 * @param onBack Back navigation.
 * @param onEntryClick Opens a game.
 * @param viewModel Screen ViewModel.
 */
@Composable
fun PlatformGamesScreen(
    platform: String?,
    onBack: () -> Unit,
    onEntryClick: (CollectionEntry) -> Unit,
    viewModel: PlatformGamesViewModel = koinViewModel(),
) {
    LaunchedEffect(platform) { viewModel.onPlatformSelected(platform) }
    val state by viewModel.uiState.collectAsState()
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        fullWidth { PlatformHeader(state, onBack, viewModel::onPlatformSelected) }
        fullWidth {
            val name = state.filters.platform?.let { shortPlatformName(it) } ?: "tous les jeux"
            VaultSearchField(state.filters.query, viewModel::onQueryChanged, "Chercher dans $name", Modifier.fillMaxWidth())
        }
        fullWidth { FilterRow(state, viewModel) }
        if (!state.isLoading && state.games.isEmpty()) {
            fullWidth { EmptyState(Icons.Default.SearchOff, "Aucun jeu", message = "Aucun jeu ne correspond à ces filtres.") }
        }
        itemTiles(state.games, square = false, onClick = onEntryClick)
    }
}

@Composable
private fun PlatformHeader(
    state: PlatformGamesUiState,
    onBack: () -> Unit,
    onChange: (String?) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconSquareButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onBack)
        Column(Modifier.weight(1f)) {
            Text(
                state.filters.platform?.let { shortPlatformName(it) } ?: "Tous les jeux",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface,
                maxLines = 1,
            )
            Text(
                "${gameCount(state.count)} · ${formatEuro(state.value)}",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
            )
        }
        PlatformSwitcher(state.platforms, onChange)
    }
}

@Composable
private fun PlatformSwitcher(
    platforms: List<PlatformSummary>,
    onChange: (String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        VaultButton("Changer", {
            open = true
        }, variant = VaultButtonVariant.Ghost, size = VaultButtonSize.Medium, leadingIcon = Icons.Default.KeyboardArrowDown)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = SurfaceContainerHigh) {
            DropdownMenuItem(text = { Text("Tous les jeux", color = OnSurface) }, onClick = {
                open = false
                onChange(null)
            })
            platforms.forEach { p ->
                DropdownMenuItem(
                    text = { Text("${shortPlatformName(p.platform)} · ${p.count}", color = OnSurface) },
                    onClick = {
                        open = false
                        onChange(p.platform)
                    },
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    state: PlatformGamesUiState,
    viewModel: PlatformGamesViewModel,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { VaultFilterChip("Tous", state.filters.format == null, { viewModel.onFormatSelected(null) }) }
            items(FormatFilters) { format ->
                VaultFilterChip(format.label, state.filters.format == format, { viewModel.onFormatSelected(format) })
            }
        }
        Text(
            state.filters.sort.label,
            style = MaterialTheme.typography.titleSmall,
            color = OnSurface,
            modifier = Modifier.clickable(onClick = viewModel::onSortToggled).padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
        )
    }
}
