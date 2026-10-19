package com.thevault.ui.screen.wishlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.EmptyState
import com.thevault.ui.component.FormatBadge
import com.thevault.ui.component.ItemCover
import com.thevault.ui.component.PlatformBadge
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.VaultButton
import com.thevault.ui.component.VaultButtonSize
import com.thevault.ui.component.VaultButtonVariant
import com.thevault.ui.component.VaultCard
import com.thevault.ui.component.formatDate
import com.thevault.ui.component.formatEuroOrDash
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.SurfaceContainerHigh
import com.thevault.ui.theme.VaultRadius
import org.koin.compose.viewmodel.koinViewModel

/** Wishlist screen. */
@Composable
fun WishlistScreen(
    onEntryClick: (CollectionEntry) -> Unit,
    onAdd: () -> Unit,
    viewModel: WishlistViewModel = koinViewModel(),
) {
    val entries by viewModel.entries.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val list = entries.orEmpty()
        item { ScreenHeader("Wishlist", summary = "${list.size} objets", modifier = Modifier.padding(bottom = 8.dp)) }
        if (entries != null && list.isEmpty()) {
            item {
                EmptyState(
                    Icons.Default.StarBorder,
                    "Aucun objet convoité",
                    message = "Ajoutez un objet en choisissant « Wishlist » dans l'écran d'ajout.",
                    actionLabel = "Ajouter",
                    onAction = onAdd,
                )
            }
        }
        items(list, key = { "${it.kind}_${it.id}" }) { entry ->
            WishCard(entry, onDetails = { onEntryClick(entry) }, onBought = { viewModel.markBought(entry) })
        }
    }
}

@Composable
private fun WishCard(
    entry: CollectionEntry,
    onDetails: () -> Unit,
    onBought: () -> Unit,
) {
    VaultCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ItemCover(entry.coverUrl, entry.platform, entry.title, Modifier.size(52.dp, 68.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.title, style = MaterialTheme.typography.titleMedium, color = OnSurface, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlatformBadge(entry.platform)
                    FormatBadge(entry.condition)
                }
            }
        }
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoBox("Cote estimée", formatEuroOrDash(entry.value, decimals = 2), Modifier.weight(1f))
            InfoBox("Ajouté le", entry.acquiredAt?.let { formatDate(it) } ?: "—", Modifier.weight(1f))
        }
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VaultButton("Détails", onDetails, Modifier.weight(1f), VaultButtonVariant.Ghost, VaultButtonSize.Medium)
            VaultButton("Je l'ai acheté", onBought, Modifier.weight(1f), size = VaultButtonSize.Medium)
        }
    }
}

@Composable
private fun InfoBox(
    label: String,
    value: String,
    modifier: Modifier,
) {
    Column(modifier.clip(VaultRadius.lg).background(SurfaceContainerHigh).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = OnSurface, modifier = Modifier.padding(top = 3.dp))
    }
}
