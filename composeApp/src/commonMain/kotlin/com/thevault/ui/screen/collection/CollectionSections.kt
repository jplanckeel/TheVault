package com.thevault.ui.screen.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.EmptyState
import com.thevault.ui.component.Engrave
import com.thevault.ui.component.LinkText
import com.thevault.ui.component.PlatformTile
import com.thevault.ui.component.PriceDelta
import com.thevault.ui.component.VaultChip
import com.thevault.ui.component.color
import com.thevault.ui.component.formatEuro
import com.thevault.ui.component.formatWholePercent
import com.thevault.ui.component.gameCount
import com.thevault.ui.component.shortPlatformName

/** « Jeux » tab: platform tiles, or the search results. */
internal fun LazyGridScope.gamesSection(
    state: CollectionUiState,
    viewModel: CollectionViewModel,
    navigation: CollectionNavigation,
) {
    val total = state.totals[ItemKind.Game] ?: KindTotal()
    if (state.filters.query.isNotBlank()) {
        if (state.gameResults.isEmpty()) {
            fullWidth {
                EmptyState(
                    Icons.Default.SearchOff,
                    "Aucun jeu trouvé",
                    message = "Essaie un autre titre.",
                )
            }
        }
        cards(state.gameResults, navigation.onEntryClick)
        return
    }
    if (total.count == 0) {
        fullWidth { EmptyState(Icons.Default.SportsEsports, "Aucun jeu", action = "Ajouter un jeu", onAction = navigation.onAdd) }
        return
    }
    fullWidth {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Engrave("${state.platforms.size} plateformes · ${formatEuro(total.value)}", Modifier.weight(1f))
            LinkText("⇅ ${state.filters.platformSort.label}", viewModel::onPlatformSortToggled)
        }
    }
    items(state.platforms, key = { "platform_${it.platform}" }, span = { GridItemSpan(maxLineSpan) }) { p ->
        PlatformRow(p, total.count, navigation.onOpenPlatform)
    }
    fullWidth { LinkText("Voir tous les jeux", { navigation.onOpenPlatform(null) }) }
}

@Composable
private fun PlatformRow(
    p: PlatformSummary,
    totalGames: Int,
    onOpen: (String) -> Unit,
) {
    PlatformTile(
        name = shortPlatformName(p.platform),
        meta = "${p.manufacturer.label} · ${gameCount(p.count)}",
        band = p.manufacturer.color,
        value = if (p.value > 0.0) formatEuro(p.value) else "—",
        onClick = { onOpen(p.platform) },
        share = if (totalGames > 0) p.count.toFloat() / totalGames else null,
        change = p.changePercent?.takeIf { it != 0.0 }?.let { pct -> { PriceDelta(formatWholePercent(pct), pct > 0, small = true) } },
    )
}

/** « Consoles » tab: manufacturer chips and cards. */
internal fun LazyGridScope.consolesSection(
    state: CollectionUiState,
    viewModel: CollectionViewModel,
    navigation: CollectionNavigation,
) {
    if (state.consoleMakers.size > 1) {
        fullWidth {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { VaultChip("Toutes", state.filters.consoleMaker == null, { viewModel.onConsoleMakerSelected(null) }) }
                items(state.consoleMakers) { maker ->
                    VaultChip(maker.label, state.filters.consoleMaker == maker, {
                        viewModel.onConsoleMakerSelected(maker)
                    })
                }
            }
        }
    }
    if (state.consoles.isEmpty()) {
        fullWidth { EmptyState(Icons.Default.SportsEsports, "Aucune console", action = "Ajouter une console", onAction = navigation.onAdd) }
    }
    cards(state.consoles, navigation.onEntryClick)
}

/** « Accessoires » tab: controller / other chips and cards. */
internal fun LazyGridScope.accessoriesSection(
    state: CollectionUiState,
    viewModel: CollectionViewModel,
    navigation: CollectionNavigation,
) {
    fullWidth {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(
                AccessoryFilter.entries,
            ) { f -> VaultChip(f.label, state.filters.accessoryFilter == f, { viewModel.onAccessoryFilterSelected(f) }) }
        }
    }
    if (state.accessories.isEmpty()) {
        fullWidth { EmptyState(Icons.Default.Cable, "Aucun accessoire", action = "Ajouter un accessoire", onAction = navigation.onAdd) }
    }
    cards(state.accessories, navigation.onEntryClick)
}
