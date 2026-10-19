package com.thevault.ui.screen.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.EmptyState
import com.thevault.ui.component.ItemCard
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.VaultFilterChip
import com.thevault.ui.component.VaultSearchField
import com.thevault.ui.component.formatEuro
import com.thevault.ui.component.toBadge
import com.thevault.ui.theme.Primary
import org.koin.compose.viewmodel.koinViewModel

/** Owned items screen. */
@Composable
fun CollectionScreen(
    onEntryClick: (CollectionEntry) -> Unit,
    onAddItem: () -> Unit,
    viewModel: CollectionViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    CollectionGridContent(
        title = "Ma collection",
        emptyIcon = Icons.Default.Inventory2,
        emptyTitle = "Votre vault est vide",
        state = state,
        actions = viewModel.actions(onEntryClick, onAddItem),
    )
}

/** Wishlist screen — same layout, wishlist items only. */
@Composable
fun WishlistScreen(
    onEntryClick: (CollectionEntry) -> Unit,
    onAddItem: () -> Unit,
    viewModel: WishlistViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    CollectionGridContent(
        title = "Wishlist",
        emptyIcon = Icons.Default.FavoriteBorder,
        emptyTitle = "Aucun objet convoité",
        state = state,
        actions = viewModel.actions(onEntryClick, onAddItem),
    )
}

private fun BaseCollectionViewModel.actions(
    onEntryClick: (CollectionEntry) -> Unit,
    onAddItem: () -> Unit,
) = CollectionActions(::onKindSelected, ::onQueryChanged, ::onPlatformSelected, ::onSortSelected, onEntryClick, onAddItem)

/**
 * Stateless collection grid: header, category selector, search, platform chips and cards.
 *
 * @param title Screen title.
 * @param emptyIcon Icon of the empty state.
 * @param emptyTitle Title of the empty state.
 * @param state UI state.
 * @param actions User callbacks.
 */
@Composable
fun CollectionGridContent(
    title: String,
    emptyIcon: ImageVector,
    emptyTitle: String,
    state: CollectionUiState,
    actions: CollectionActions,
) {
    val view = state.view
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        fullWidth {
            ScreenHeader(
                title = title,
                subtitle = "${view.totalCount} objets · ${formatEuro(view.totalValue)} estimés",
            )
        }
        fullWidth { CollectionFiltersBar(state, actions) }
        when {
            state.isLoading -> fullWidth { LoadingBox() }
            view.entries.isEmpty() -> fullWidth { CollectionEmptyState(state.filters, emptyIcon, emptyTitle, actions.onAddItem) }
            else ->
                items(view.entries, key = { "${it.kind}_${it.id}" }) { entry ->
                    ItemCard(
                        title = entry.title,
                        platform = entry.platform,
                        coverUrl = entry.coverUrl,
                        badge = entry.condition.toBadge(),
                        price = entry.value,
                        onClick = { actions.onEntryClick(entry) },
                    )
                }
        }
    }
}

@Composable
private fun CollectionFiltersBar(
    state: CollectionUiState,
    actions: CollectionActions,
) {
    val filters = state.filters
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ItemKind.entries) { kind ->
                val count = state.view.countByKind[kind] ?: 0
                VaultFilterChip("${kind.label} · $count", filters.kind == kind, { actions.onKindSelected(kind) })
            }
        }
        VaultSearchField(
            query = filters.query,
            onQueryChange = actions.onQueryChanged,
            placeholder = "Rechercher dans ${filters.kind.label.lowercase()}…",
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.view.platforms.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { VaultFilterChip("Toutes", filters.platform == null, { actions.onPlatformSelected(null) }) }
                items(state.view.platforms) { platform ->
                    VaultFilterChip(platform, filters.platform == platform, { actions.onPlatformSelected(platform) })
                }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CollectionSort.entries) { sort ->
                VaultFilterChip(sort.label, filters.sort == sort, { actions.onSortSelected(sort) })
            }
        }
    }
}

@Composable
private fun CollectionEmptyState(
    filters: CollectionFilters,
    icon: ImageVector,
    title: String,
    onAddItem: () -> Unit,
) {
    val filtered = filters.query.isNotBlank() || filters.platform != null
    EmptyState(
        icon = icon,
        title = if (filtered) "Aucun résultat" else title,
        message = if (filtered) "Essayez une autre recherche ou plateforme." else "Ajoutez votre premier objet.",
        actionLabel = if (filtered) null else "Ajouter un objet",
        onAction = onAddItem,
    )
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Primary)
    }
}

/** Grid item spanning every column (headers, empty states). */
private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) = item(span = { GridItemSpan(maxLineSpan) }) { content() }
