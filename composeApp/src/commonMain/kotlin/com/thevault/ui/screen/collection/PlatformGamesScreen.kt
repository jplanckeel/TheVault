package com.thevault.ui.screen.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.ButtonSize
import com.thevault.ui.component.EmptyState
import com.thevault.ui.component.Engrave
import com.thevault.ui.component.KeyButton
import com.thevault.ui.component.RefreshFeedback
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.Segment
import com.thevault.ui.component.SegmentedControl
import com.thevault.ui.component.VaultButton
import com.thevault.ui.component.VaultChip
import com.thevault.ui.component.VaultPullRefresh
import com.thevault.ui.component.formatEuro
import com.thevault.ui.component.gameCount
import com.thevault.ui.component.label
import com.thevault.ui.component.shortPlatformName
import com.thevault.ui.navigation.tabBarClearance
import org.koin.compose.viewmodel.koinViewModel

/**
 * Games of one platform, or of every platform.
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
    val selection by viewModel.selection.collectAsState()
    val refresh by viewModel.refresh.collectAsState()
    var bulk by remember { mutableStateOf(false) }
    if (bulk) {
        BulkEditSheet(selection.size, { viewModel.applyBulk(it).also { bulk = false } }, { bulk = false })
    }
    VaultPullRefresh(refresh.isRefreshing, viewModel::onRefresh) {
        PlatformGamesGrid(state, selection, refresh.message, viewModel, onBack, onEntryClick) { bulk = true }
    }
}

@Suppress("LongParameterList")
@Composable
private fun PlatformGamesGrid(
    state: PlatformGamesUiState,
    selection: Set<String>,
    refreshMessage: RefreshMessage?,
    viewModel: PlatformGamesViewModel,
    onBack: () -> Unit,
    onEntryClick: (CollectionEntry) -> Unit,
    onBulkEdit: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = tabBarClearance()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        fullWidth {
            if (selection.isEmpty()) {
                Header(state, onBack, viewModel::onSortToggled)
            } else {
                SelectionBar(selection.size, viewModel::clearSelection, onBulkEdit)
            }
        }
        refreshMessage?.let { fullWidth { RefreshFeedback(it.text, it.success) } }
        fullWidth { Engrave("${gameCount(state.ownedCount)} · ${formatEuro(state.value)} · tri ${state.filters.sort.label}") }
        fullWidth {
            val options =
                listOf(
                    Ownership.Owned to "Possédés · ${state.ownedCount}",
                    Ownership.Wishlist to "Envies · ${state.wishCount}",
                    Ownership.All to "Tous",
                )
            SegmentedControl(
                options.map { Segment(it.second) },
                state.ownership.ordinal,
                { viewModel.onOwnershipSelected(options[it].first) },
            )
        }
        if (state.formats.size > 1) fullWidth { FormatChips(state, viewModel) }
        if (!state.isLoading && state.games.isEmpty()) {
            fullWidth { EmptyState(Icons.Default.SearchOff, "Aucun jeu", message = "Aucun jeu ne correspond à ces filtres.") }
        }
        cards(state.games, onEntryClick, missing = false, selection = selection) { viewModel.toggleSelection(it.id) }
    }
}

@Composable
private fun Header(
    state: PlatformGamesUiState,
    onBack: () -> Unit,
    onSort: () -> Unit,
) {
    val eyebrow = listOfNotNull(state.manufacturer.label.takeIf { state.platform != null }, state.region?.label).joinToString(" · ")
    ScreenHeader(
        title = state.platform?.let { shortPlatformName(it) } ?: "Tous les jeux",
        eyebrow = eyebrow.ifBlank { "Collection" },
        leading = { KeyButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onBack) },
    ) {
        KeyButton(Icons.AutoMirrored.Filled.Sort, "Changer le tri", onSort)
    }
}

@Composable
private fun FormatChips(
    state: PlatformGamesUiState,
    viewModel: PlatformGamesViewModel,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { VaultChip("Tous", state.filters.format == null, { viewModel.onFormatSelected(null) }) }
        items(state.formats.entries.toList()) { (group, count) ->
            VaultChip(group.label, state.filters.format == group, { viewModel.onFormatSelected(group) }, count = count)
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onCancel: () -> Unit,
    onEdit: () -> Unit,
) {
    ScreenHeader(
        "$count sélectionnés",
        eyebrow = "Édition en lot",
        leading = { KeyButton(Icons.Default.Close, "Annuler la sélection", onCancel) },
    ) {
        VaultButton("Modifier", onEdit, size = ButtonSize.Small)
    }
}
