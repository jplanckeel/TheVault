package com.thevault.ui.screen.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.EmptyState
import com.thevault.ui.component.Eyebrow
import com.thevault.ui.component.PlatformBadge
import com.thevault.ui.component.VaultFilterChip
import com.thevault.ui.component.VaultSearchField
import com.thevault.ui.component.formatEuro
import com.thevault.ui.component.gameCount
import com.thevault.ui.component.gameNoun
import com.thevault.ui.component.shortPlatformName
import com.thevault.ui.theme.CardBorderColor
import com.thevault.ui.theme.HighlightBorder
import com.thevault.ui.theme.HighlightContainer
import com.thevault.ui.theme.OnHighlightVariant
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.SurfaceContainerLow
import com.thevault.ui.theme.VaultRadius

private const val VISIBLE_PLATFORMS = 6

/** « Jeux » tab: search, « Tous les jeux », platforms grouped by manufacturer. */
internal fun LazyGridScope.gamesSection(
    state: CollectionUiState,
    viewModel: CollectionViewModel,
    navigation: CollectionNavigation,
) {
    val total = state.totals[ItemKind.Game] ?: KindTotal()
    fullWidth {
        VaultSearchField(state.filters.query, viewModel::onQueryChanged, "Chercher dans les ${total.count} jeux", Modifier.fillMaxWidth())
    }
    if (state.filters.query.isNotBlank()) {
        if (state.gameResults.isEmpty()) {
            fullWidth { EmptyState(Icons.Default.SearchOff, "Aucun jeu trouvé", message = "Essayez un autre titre.") }
        }
        itemTiles(state.gameResults, square = false, onClick = navigation.onEntryClick)
        return
    }
    if (total.count == 0) {
        fullWidth { EmptyState(Icons.Default.SportsEsports, "Aucun jeu", actionLabel = "Ajouter un jeu", onAction = navigation.onAdd) }
        return
    }
    fullWidth { AllGamesBanner(total.count, state.platformCount) { navigation.onOpenPlatform(null) } }
    state.groups.forEach { group -> manufacturerGroup(group, navigation) }
}

private fun LazyGridScope.manufacturerGroup(
    group: ManufacturerGroup,
    navigation: CollectionNavigation,
) {
    fullWidth {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("${group.manufacturer.label} · ${group.platforms.size}", Modifier.weight(1f))
            Text(gameCount(group.count), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
    }
    item(span = { GridItemSpan(maxLineSpan) }, key = "group_${group.manufacturer}") {
        PlatformTilesGrid(group.platforms, navigation.onOpenPlatform)
    }
}

/** Platform tiles of one manufacturer, collapsed after six platforms. */
@Composable
private fun PlatformTilesGrid(
    platforms: List<PlatformSummary>,
    onOpen: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val visible = if (expanded) platforms else platforms.take(VISIBLE_PLATFORMS)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        visible.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { PlatformTile(it, { onOpen(it.platform) }, Modifier.weight(1f)) }
                if (row.size == 1) Column(Modifier.weight(1f)) {}
            }
        }
        val hidden = platforms.drop(VISIBLE_PLATFORMS)
        if (!expanded && hidden.isNotEmpty()) {
            Text(
                "${hidden.size} autres : ${hidden.joinToString { shortPlatformName(it.platform) }}",
                style = MaterialTheme.typography.titleSmall,
                color = OnSurface,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(VaultRadius.lg)
                        .border(1.dp, CardBorderColor, VaultRadius.lg)
                        .clickable { expanded = true }
                        .padding(10.dp),
            )
        }
    }
}

@Composable
private fun PlatformTile(
    platform: PlatformSummary,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
            .heightIn(min = 92.dp)
            .clip(VaultRadius.xl)
            .background(SurfaceContainerLow)
            .border(1.dp, CardBorderColor, VaultRadius.xl)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PlatformBadge(platform.platform)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${platform.count}", style = MaterialTheme.typography.headlineMedium, color = OnSurface)
            Text(
                "${gameNoun(platform.count)} · ${formatEuro(platform.value)}",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

@Composable
private fun AllGamesBanner(
    count: Int,
    platformCount: Int,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(VaultRadius.xl)
            .background(HighlightContainer)
            .border(1.dp, HighlightBorder, VaultRadius.xl)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Tous les jeux", style = MaterialTheme.typography.titleMedium, color = OnSurface)
            Text("$platformCount plateformes", style = MaterialTheme.typography.bodySmall, color = OnHighlightVariant)
        }
        Text("$count", style = MaterialTheme.typography.titleMedium, color = OnSurface)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
    }
}

/** « Consoles » tab: manufacturer chips and square tiles. */
internal fun LazyGridScope.consolesSection(
    state: CollectionUiState,
    viewModel: CollectionViewModel,
    navigation: CollectionNavigation,
) {
    if (state.consoleMakers.size > 1) {
        fullWidth {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { VaultFilterChip("Toutes", state.filters.consoleMaker == null, { viewModel.onConsoleMakerSelected(null) }) }
                items(state.consoleMakers) { maker ->
                    VaultFilterChip(maker.label, state.filters.consoleMaker == maker, { viewModel.onConsoleMakerSelected(maker) })
                }
            }
        }
    }
    if (state.consoles.isEmpty()) {
        fullWidth {
            EmptyState(
                Icons.Default.SportsEsports,
                "Aucune console",
                actionLabel = "Ajouter une console",
                onAction = navigation.onAdd,
            )
        }
    }
    itemTiles(state.consoles, square = true, onClick = navigation.onEntryClick)
}

/** « Accessoires » tab: controller / other chips and square tiles. */
internal fun LazyGridScope.accessoriesSection(
    state: CollectionUiState,
    viewModel: CollectionViewModel,
    navigation: CollectionNavigation,
) {
    fullWidth {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AccessoryFilter.entries) { filter ->
                VaultFilterChip(filter.label, state.filters.accessoryFilter == filter, { viewModel.onAccessoryFilterSelected(filter) })
            }
        }
    }
    if (state.accessories.isEmpty()) {
        fullWidth {
            EmptyState(
                Icons.Default.Cable,
                "Aucun accessoire",
                actionLabel = "Ajouter un accessoire",
                onAction = navigation.onAdd,
            )
        }
    }
    itemTiles(state.accessories, square = true, onClick = navigation.onEntryClick)
}
