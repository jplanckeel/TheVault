package com.thevault.ui.screen.wishlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.EmptyState
import com.thevault.ui.component.KeyButton
import com.thevault.ui.component.LedColor
import com.thevault.ui.component.ListRow
import com.thevault.ui.component.LoadingBox
import com.thevault.ui.component.Panel
import com.thevault.ui.component.PriceDelta
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.SectionHeader
import com.thevault.ui.component.StatusLed
import com.thevault.ui.component.formatEuro
import com.thevault.ui.component.formatEuroOrDash
import com.thevault.ui.component.formatLabel
import com.thevault.ui.component.formatWholePercent
import com.thevault.ui.component.shortPlatformName
import com.thevault.ui.navigation.tabBarClearance
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.copy.WishPriorities
import com.thevault.ui.screen.copy.label
import com.thevault.ui.theme.Vault
import org.koin.compose.viewmodel.koinViewModel

/** Wishlist (« Envies »): eBay drops and the items to watch. */
@Composable
fun WishlistScreen(
    onEntryClick: (CollectionEntry) -> Unit,
    onAdd: () -> Unit,
    viewModel: WishlistViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = tabBarClearance()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            ScreenHeader("Envies", eyebrow = "${state.watched.size} objets · cote ${formatEuro(state.total)}") {
                KeyButton(Icons.AutoMirrored.Filled.Sort, "Trier : ${state.sort.label}", viewModel::onSortToggled)
            }
        }
        when {
            state.isLoading -> item { LoadingBox() }
            state.watched.isEmpty() ->
                item {
                    EmptyState(
                        Icons.Default.FavoriteBorder,
                        "Aucune envie",
                        message = "Ajoute un jeu en choisissant « Envie » pour suivre sa cote eBay.",
                        action = "Ajouter",
                        onAction = onAdd,
                    )
                }
            else -> content(state, onEntryClick)
        }
    }
}

private fun LazyListScope.content(
    state: WishlistUiState,
    onEntryClick: (CollectionEntry) -> Unit,
) {
    if (state.underPrice.isNotEmpty()) {
        item {
            Panel(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                StatusLed(LedColor.Power, label = "${state.underPrice.size} sous ton prix")
                Text(
                    "La cote eBay PAL est passée sous le prix max que tu as fixé.",
                    style = Vault.type.caption,
                    color = Vault.colors.ink,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        item { SectionHeader("Sous ton prix", Modifier.padding(top = 12.dp)) }
        items(state.underPrice, key = { "under_${it.id}" }) { WishRow(it, onEntryClick) }
    }
    if (state.dropping.isNotEmpty()) {
        item {
            Panel(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                StatusLed(LedColor.Power, label = "${state.dropping.size} en baisse sur eBay")
                Text(
                    "Ces objets sont moins chers qu'il y a 30 jours.",
                    style = Vault.type.caption,
                    color = Vault.colors.ink,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        item { SectionHeader("En baisse", Modifier.padding(top = 12.dp)) }
        items(state.dropping, key = { "drop_${it.id}" }) { WishRow(it, onEntryClick) }
    }
    item { SectionHeader("À surveiller", Modifier.padding(top = 12.dp), action = "Trier : ${state.sort.label}") }
    items(state.watched, key = { "watch_${it.id}" }) { WishRow(it, onEntryClick) }
}

@Composable
private fun WishRow(
    entry: CollectionEntry,
    onClick: (CollectionEntry) -> Unit,
) {
    val where =
        if (entry.kind ==
            ItemKind.Accessory
        ) {
            "Accessoire ${shortPlatformName(entry.platform)}"
        } else {
            shortPlatformName(entry.platform)
        }
    ListRow(
        title = entry.title,
        meta =
            listOfNotNull(
                where,
                (
                    entry.details.wish.target
                        ?.label ?: entry.condition.formatLabel
                ),
                entry.details.wish.maxPrice
                    ?.let { "max ${formatEuro(it)}" },
                WishPriorities.find { it.first == entry.details.wish.priority }?.second?.let { "priorité $it" },
            ).filter { it.isNotBlank() }.joinToString(" · "),
        trailing = formatEuroOrDash(entry.value),
        cover = Triple(entry.coverUrl, entry.platform, entry.condition),
        onClick = { onClick(entry) },
        trailingContent =
            entry.changePercent?.takeIf { it != 0.0 }?.let { pct ->
                { PriceDelta(formatWholePercent(pct), pct > 0, small = true) }
            },
    )
}
