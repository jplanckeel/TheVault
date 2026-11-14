package com.thevault.ui.screen.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.ItemStatus
import com.thevault.ui.component.CollectionCard
import com.thevault.ui.component.KeyButton
import com.thevault.ui.component.LoadingBox
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.SearchField
import com.thevault.ui.component.VaultChip
import com.thevault.ui.component.formatCount
import com.thevault.ui.navigation.tabBarClearance
import org.koin.compose.viewmodel.koinViewModel

/** Columns of the card grids. */
internal const val GRID_COLUMNS = 3

/**
 * Callbacks of the collection screen.
 *
 * @property onEntryClick Opens an item detail.
 * @property onOpenPlatform Opens the games of a platform (null = every game).
 * @property onAdd Opens the add screen.
 */
data class CollectionNavigation(
    val onEntryClick: (CollectionEntry) -> Unit = {},
    val onOpenPlatform: (String?) -> Unit = {},
    val onAdd: () -> Unit = {},
)

/** Collection screen: games by platform, consoles and accessories. */
@Composable
fun CollectionScreen(
    navigation: CollectionNavigation,
    viewModel: CollectionViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    CollectionContent(state, viewModel, navigation)
}

/**
 * Collection content.
 *
 * @param state UI state.
 * @param viewModel Receives the filter changes.
 * @param navigation Navigation callbacks.
 */
@Composable
fun CollectionContent(
    state: CollectionUiState,
    viewModel: CollectionViewModel,
    navigation: CollectionNavigation,
) {
    val kind = state.filters.kind
    var searching by rememberSaveable { mutableStateOf(false) }
    val total = state.totals.values.sumOf { it.count }
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = tabBarClearance()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        fullWidth {
            CollectionHeader(total, searching) {
                if (searching) viewModel.onQueryChanged("")
                searching = !searching
            }
        }
        if (searching) fullWidth { SearchField(state.filters.query, viewModel::onQueryChanged, "Chercher un jeu", Modifier.fillMaxWidth()) }
        fullWidth {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ItemKind.entries) { k ->
                    VaultChip(
                        k.label,
                        k == kind && !state.filters.showSold,
                        { viewModel.onKindSelected(k) },
                        count =
                            state.totals[k]?.count ?: 0,
                    )
                }
                if (state.sold.isNotEmpty()) {
                    item { VaultChip("Vendus", state.filters.showSold, viewModel::onSoldSelected, count = state.sold.size) }
                }
            }
        }
        when {
            state.isLoading -> fullWidth { LoadingBox() }
            state.filters.showSold -> soldSection(state.sold, navigation)
            kind == ItemKind.Game -> gamesSection(state, viewModel, navigation)
            kind == ItemKind.Console -> consolesSection(state, viewModel, navigation)
            else -> accessoriesSection(state, viewModel, navigation)
        }
    }
}

@Composable
private fun CollectionHeader(
    total: Int,
    searching: Boolean,
    onToggle: () -> Unit,
) {
    ScreenHeader("Collection", eyebrow = "Coffre · ${formatCount(total.toLong())} objets") {
        KeyButton(if (searching) Icons.Default.Close else Icons.Default.Search, "Rechercher", onToggle)
    }
}

/** Grid of collection cards. */
internal fun LazyGridScope.cards(
    entries: List<CollectionEntry>,
    onClick: (CollectionEntry) -> Unit,
    missing: Boolean = false,
    selection: Set<String>? = null,
    onSelect: ((CollectionEntry) -> Unit)? = null,
) {
    items(entries, key = { "${it.kind}_${it.id}" }) { entry ->
        val selecting = !selection.isNullOrEmpty()
        CollectionCard(
            entry.card,
            { if (selecting && onSelect != null) onSelect(entry) else onClick(entry) },
            missing = missing || entry.status == ItemStatus.WISHLIST,
            selected = selection?.contains(entry.id) == true,
            onLongClick = onSelect?.let { { it(entry) } },
        )
    }
}

/** Grid item spanning every column. */
internal fun LazyGridScope.fullWidth(content: @Composable () -> Unit) = item(span = { GridItemSpan(maxLineSpan) }) { content() }
