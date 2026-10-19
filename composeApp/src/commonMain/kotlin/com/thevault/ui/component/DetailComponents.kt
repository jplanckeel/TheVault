package com.thevault.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.ui.theme.Error
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceMuted
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.Success
import com.thevault.ui.theme.SurfaceContainerHigh

/**
 * Detail top bar: back button and a « … » menu with a destructive action.
 *
 * @param onBack Back navigation.
 * @param onDelete Optional delete action shown in the menu.
 */
@Composable
fun DetailTopBar(
    onBack: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        IconSquareButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onBack)
        if (onDelete != null) {
            var open by remember { mutableStateOf(false) }
            Box {
                IconSquareButton(Icons.Default.MoreHoriz, "Plus d'actions", { open = true })
                DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = SurfaceContainerHigh) {
                    DropdownMenuItem(
                        text = { Text("Supprimer", color = Error) },
                        onClick = {
                            open = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

/**
 * Detail header: cover on the left, title, platform badge and secondary badges.
 *
 * @param title Item title.
 * @param platform Platform.
 * @param coverUrl Cover URL.
 * @param badges Secondary badges (format, region, genre…).
 */
@Composable
fun DetailHeader(
    title: String,
    platform: String,
    coverUrl: String?,
    badges: @Composable () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        ItemCover(coverUrl, platform, title, Modifier.width(132.dp).aspectRatio(3f / 4f))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = OnSurface)
            PlatformBadge(platform)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                badges()
            }
        }
    }
}

/**
 * « Ta cote » card: estimated value, purchase price and gain / loss.
 *
 * @param formatLabel Format of the item ("CIB", "Loose"…).
 * @param value Estimated value.
 * @param purchasePrice Purchase price.
 */
@Composable
fun QuoteCard(
    formatLabel: String,
    value: Double?,
    purchasePrice: Double?,
) {
    VaultCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Ta cote · $formatLabel")
                Text(
                    formatEuroOrDash(value, decimals = 2),
                    style = MaterialTheme.typography.displayLarge,
                    color = OnSurface,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (purchasePrice != null) "Payé ${formatEuro(purchasePrice, decimals = 2)}" else "Prix payé ?",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )
                if (value != null && purchasePrice != null) {
                    val delta = value - purchasePrice
                    Text(
                        (if (delta > 0) "+" else "") + formatEuro(delta, decimals = 2),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (delta >= 0) Success else Error,
                    )
                }
            }
        }
    }
}

/**
 * Two-column key / value grid; blank values are skipped.
 *
 * @param rows Key / value pairs.
 */
@Composable
fun InfoGrid(rows: List<Pair<String, String?>>) {
    val visible = rows.filter { !it.second.isNullOrBlank() }
    if (visible.isEmpty()) return
    VaultCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            visible.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    pair.forEach { (key, value) ->
                        Column(Modifier.weight(1f)) {
                            Text(key, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
                            Text(
                                value.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurface,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Notes card with an « + Ajouter une note » action when empty.
 *
 * @param notes Notes text.
 * @param onEdit Opens the edit form.
 */
@Composable
fun NotesCard(
    notes: String?,
    onEdit: (() -> Unit)?,
) {
    if (notes.isNullOrBlank() && onEdit == null) return
    VaultCard(Modifier.fillMaxWidth()) {
        Eyebrow("Notes", Modifier.padding(bottom = 10.dp))
        if (notes.isNullOrBlank()) {
            Text(
                "+ Ajouter une note",
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                modifier = Modifier.padding(vertical = 2.dp).then(if (onEdit != null) Modifier.clickableNoRipple(onEdit) else Modifier),
            )
        } else {
            Text(notes, style = MaterialTheme.typography.bodyLarge, color = OnSurface)
        }
    }
}

/** Minimal spacer used between the scrolling content and the bottom action bar. */
@Composable
fun BottomBarSpacer() {
    Box(Modifier.size(96.dp))
}
