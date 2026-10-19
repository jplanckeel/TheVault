package com.thevault.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.Game
import com.thevault.domain.model.VaultStats
import com.thevault.ui.component.ChipTone
import com.thevault.ui.component.ItemCard
import com.thevault.ui.component.ItemRow
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.SectionHeader
import com.thevault.ui.component.StatCard
import com.thevault.ui.component.VaultCard
import com.thevault.ui.component.VaultChip
import com.thevault.ui.component.formatEuro
import com.thevault.ui.component.formatSignedEuro
import com.thevault.ui.component.toBadge
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.toEntry
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.SurfaceContainerHigh
import com.thevault.ui.theme.TabularNumbers
import com.thevault.ui.theme.VaultRadius
import org.koin.compose.viewmodel.koinViewModel

/** Dashboard screen. */
@Composable
fun DashboardScreen(
    actions: DashboardActions,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    DashboardContent(state, actions)
}

/**
 * Stateless dashboard: value hero, counters, latest additions, platforms, top rated.
 *
 * @param state UI state.
 * @param actions Navigation callbacks.
 */
@Composable
fun DashboardContent(
    state: DashboardUiState,
    actions: DashboardActions,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { ScreenHeader(title = "Dashboard", subtitle = "Votre vault en un coup d'œil") }
        item { ValueHero(state.stats) }
        item { CounterGrid(state.stats, actions) }
        if (state.latest.isNotEmpty()) {
            item { LatestAdditions(state.latest, actions) }
        }
        if (state.platforms.isNotEmpty()) {
            item { PlatformBreakdown(state.platforms) }
        }
        if (state.topRated.isNotEmpty()) {
            item { TopRated(state.topRated, actions) }
        }
    }
}

@Composable
private fun ValueHero(stats: VaultStats) {
    VaultCard(modifier = Modifier.fillMaxWidth(), shape = VaultRadius.xxl, contentPadding = PaddingValues(24.dp)) {
        Text("VALEUR ESTIMÉE", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
        Text(
            formatEuro(stats.estimatedValue),
            style = MaterialTheme.typography.displayLarge.merge(TabularNumbers),
            color = OnSurface,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (stats.totalInvested > 0) {
                val delta = stats.estimatedReturn
                VaultChip(
                    text = formatSignedEuro(delta),
                    tone = if (delta >= 0) ChipTone.Success else ChipTone.Error,
                )
            }
            Text(
                "Investi : ${formatEuro(stats.totalInvested)}",
                style = MaterialTheme.typography.bodySmall.merge(TabularNumbers),
                color = OnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CounterGrid(
    stats: VaultStats,
    actions: DashboardActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Jeux",
                value = stats.totalGames.toString(),
                icon = Icons.Default.VideogameAsset,
                onClick = { actions.onOpenCollection(ItemKind.Game) },
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Consoles",
                value = stats.totalConsoles.toString(),
                icon = Icons.Default.SportsEsports,
                onClick = { actions.onOpenCollection(ItemKind.Console) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Accessoires",
                value = stats.totalAccessories.toString(),
                icon = Icons.Default.Cable,
                onClick = { actions.onOpenCollection(ItemKind.Accessory) },
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Wishlist",
                value = stats.totalWishlist.toString(),
                icon = Icons.Default.Favorite,
                onClick = actions.onOpenWishlist,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LatestAdditions(
    entries: List<CollectionEntry>,
    actions: DashboardActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Derniers ajouts", actionLabel = "Voir tout", onAction = { actions.onOpenCollection(ItemKind.Game) })
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(entries, key = { "${it.kind}_${it.id}" }) { entry ->
                ItemCard(
                    title = entry.title,
                    platform = entry.platform,
                    coverUrl = entry.coverUrl,
                    badge = entry.condition.toBadge(),
                    price = entry.value,
                    onClick = { actions.onEntryClick(entry) },
                    modifier = Modifier.width(150.dp),
                )
            }
        }
    }
}

@Composable
private fun PlatformBreakdown(platforms: List<PlatformShare>) {
    VaultCard(modifier = Modifier.fillMaxWidth(), shape = VaultRadius.xxl) {
        SectionHeader("Par plateforme")
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            platforms.forEach { share ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row {
                        Text(share.platform, style = MaterialTheme.typography.titleSmall, color = OnSurface, modifier = Modifier.weight(1f))
                        Text(
                            "${share.count} jeux",
                            style = MaterialTheme.typography.bodySmall.merge(TabularNumbers),
                            color = OnSurfaceVariant,
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(VaultRadius.full)
                            .background(SurfaceContainerHigh),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(share.fraction)
                                .fillMaxHeight()
                                .clip(VaultRadius.full)
                                .background(Primary),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopRated(
    games: List<Game>,
    actions: DashboardActions,
) {
    VaultCard(modifier = Modifier.fillMaxWidth(), shape = VaultRadius.xxl) {
        SectionHeader("Mieux notés")
        Column(Modifier.padding(top = 4.dp)) {
            games.forEach { game ->
                ItemRow(
                    title = game.title,
                    subtitle = game.platform,
                    platform = game.platform,
                    coverUrl = game.coverUrl,
                    onClick = { actions.onEntryClick(game.toEntry()) },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Primary, modifier = Modifier.padding(end = 4.dp))
                            Text("${game.rating}", style = MaterialTheme.typography.titleSmall, color = OnSurface)
                        }
                    },
                )
            }
        }
    }
}
