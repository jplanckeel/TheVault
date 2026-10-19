package com.thevault.ui.screen.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.ItemTile
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.SegmentedControl
import com.thevault.ui.component.formatEuro
import com.thevault.ui.theme.Primary
import org.koin.compose.viewmodel.koinViewModel

/** Columns of the collection grid: platform tiles span 3 (2 per row), item tiles span 2 (3 per row). */
internal const val GRID_COLUMNS = 6
internal const val PLATFORM_SPAN = 3
internal const val ITEM_SPAN = 2

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
    val total = state.totals[kind] ?: KindTotal()
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        fullWidth {
            ScreenHeader("Collection", summary = "${total.count} ${kind.label.lowercase()} · ${formatEuro(total.value)}")
        }
        fullWidth {
            SegmentedControl(ItemKind.entries, kind, { it.label }, viewModel::onKindSelected, Modifier.padding(top = 4.dp))
        }
        when {
            state.isLoading -> fullWidth { LoadingBox() }
            kind == ItemKind.Game -> gamesSection(state, viewModel, navigation)
            kind == ItemKind.Console -> consolesSection(state, viewModel, navigation)
            else -> accessoriesSection(state, viewModel, navigation)
        }
    }
}

/** 3-column item tiles. */
internal fun LazyGridScope.itemTiles(
    entries: List<CollectionEntry>,
    square: Boolean,
    onClick: (CollectionEntry) -> Unit,
) {
    items(entries, key = { "${it.kind}_${it.id}" }, span = { GridItemSpan(ITEM_SPAN) }) { entry ->
        ItemTile(entry.tile, { onClick(entry) }, square = square)
    }
}

/** Grid item spanning every column. */
internal fun LazyGridScope.fullWidth(content: @Composable () -> Unit) = item(span = { GridItemSpan(maxLineSpan) }) { content() }

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Primary)
    }
}
