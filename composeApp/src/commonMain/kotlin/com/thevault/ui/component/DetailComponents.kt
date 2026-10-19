package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thevault.ui.theme.Background
import com.thevault.ui.theme.Error
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.Success
import com.thevault.ui.theme.SurfaceContainerHighest
import com.thevault.ui.theme.SurfaceContainerLowest
import com.thevault.ui.theme.TabularNumbers
import com.thevault.ui.theme.VaultRadius

private val HeroHeight = 420.dp

/**
 * Detail screen hero: full-width cover fading into the background, back / delete
 * buttons, badges and title.
 *
 * @param title Item title.
 * @param platform Platform used for the placeholder.
 * @param coverUrl Cover URL, or null.
 * @param onBack Back navigation.
 * @param onDelete Optional delete action (shows a trash button).
 * @param badges Chips displayed above the title.
 */
@Composable
fun DetailHero(
    title: String,
    platform: String,
    coverUrl: String?,
    onBack: () -> Unit,
    onDelete: (() -> Unit)? = null,
    badges: @Composable () -> Unit = {},
) {
    Box(Modifier.fillMaxWidth().height(HeroHeight)) {
        ItemCover(coverUrl, platform, title, Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Transparent, 0.5f to Background.copy(alpha = 0.5f), 1f to Background),
            ),
        )
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            HeroIconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = OnSurface)
            }
            if (onDelete != null) {
                HeroIconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Error)
                }
            }
        }
        Column(
            Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { badges() }
            Text(title, style = MaterialTheme.typography.displayLarge, color = OnSurface)
        }
    }
}

@Composable
private fun HeroIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.clip(VaultRadius.lg).background(SurfaceContainerLowest.copy(alpha = 0.7f)),
    ) { content() }
}

/**
 * Card listing label / value pairs; blank values are skipped.
 *
 * @param title Uppercase card title.
 * @param rows Label / value pairs.
 */
@Composable
fun InfoCard(
    title: String,
    rows: List<Pair<String, String?>>,
) {
    val visible = rows.filter { !it.second.isNullOrBlank() }
    if (visible.isEmpty()) return
    VaultCard(Modifier.fillMaxWidth()) {
        CardTitle(title)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            visible.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                    Text(
                        value.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        color = OnSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * Value card: estimated value, purchase price and gain / loss.
 *
 * @param currentValue Estimated value, if known.
 * @param purchasePrice Purchase price, if known.
 */
@Composable
fun ValueCard(
    currentValue: Double?,
    purchasePrice: Double?,
) {
    VaultCard(Modifier.fillMaxWidth(), shape = VaultRadius.xxl) {
        CardTitle("Valeur")
        Text(
            formatEuroOrDash(currentValue, decimals = 2),
            style = MaterialTheme.typography.displaySmall.merge(TabularNumbers),
            color = OnSurface,
        )
        Text("Valeur estimée", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            LabeledAmount("Prix d'achat", formatEuroOrDash(purchasePrice, decimals = 2), OnSurface, Modifier.weight(1f))
            if (currentValue != null && purchasePrice != null) {
                val delta = currentValue - purchasePrice
                LabeledAmount(
                    "Plus-value",
                    (if (delta > 0) "+" else "") + formatEuro(delta, decimals = 2),
                    if (delta >= 0) Success else Error,
                    Modifier.weight(1f),
                    alignEnd = true,
                )
            }
        }
    }
}

@Composable
private fun LabeledAmount(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier,
    alignEnd: Boolean = false,
) {
    Column(modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium.merge(TabularNumbers), color = color)
    }
}

/**
 * Personal notes card; hidden when notes are blank.
 *
 * @param notes Notes text.
 */
@Composable
fun NotesCard(notes: String?) {
    if (notes.isNullOrBlank()) return
    VaultCard(Modifier.fillMaxWidth()) {
        CardTitle("Notes")
        Text(notes, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
    }
}

/**
 * Uppercase card title in primary.
 *
 * @param text Title text.
 */
@Composable
fun CardTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Primary,
        modifier = Modifier.padding(bottom = 16.dp),
    )
}

/**
 * Destructive confirmation dialog.
 *
 * @param itemName Name of the item to delete.
 * @param onConfirm Confirmation callback.
 * @param onDismiss Dismiss callback.
 */
@Composable
fun DeleteConfirmDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Supprimer « $itemName » ?") },
        text = { Text("L'objet sera retiré définitivement de votre collection.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Supprimer", color = Error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = OnSurface) } },
        containerColor = SurfaceContainerHighest,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant,
        shape = VaultRadius.xxl,
    )
}

/** Vertical rhythm between detail cards. */
val DetailSpacing = Arrangement.spacedBy(16.dp)
