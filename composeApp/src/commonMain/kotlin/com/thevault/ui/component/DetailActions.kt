package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.ItemStatus
import com.thevault.ui.theme.Background
import com.thevault.ui.theme.CardBorderColor
import com.thevault.ui.theme.Error
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.SurfaceContainerHigh
import com.thevault.ui.theme.VaultRadius

/** Clickable without ripple, for text links. */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onClick))

/**
 * Fixed bottom action bar of a detail screen: « Vendu / prêté » and « Modifier ».
 *
 * @param onStatus Opens the status dialog.
 * @param onEdit Opens the edit form, or null when editing is not available.
 * @param modifier Modifier applied to the bar.
 */
@Composable
fun DetailBottomBar(
    onStatus: () -> Unit,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(Background)) {
        HorizontalDivider(color = CardBorderColor)
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VaultButton("Vendu / prêté", onStatus, Modifier.weight(1f), VaultButtonVariant.Ghost)
            if (onEdit != null) VaultButton("Modifier", onEdit, Modifier.weight(1.4f))
        }
    }
}

/**
 * Status dialog: owned, sold, loaned or back to the wishlist.
 *
 * @param current Current status.
 * @param onSelect Called with the chosen status.
 * @param onDismiss Dismiss callback.
 */
@Composable
fun StatusDialog(
    current: ItemStatus,
    onSelect: (ItemStatus) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Statut de l'objet") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ItemStatus.entries.forEach { status ->
                    Text(
                        status.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (status == current) Primary else OnSurface,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onSelect(status) }
                                .padding(vertical = 10.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer", color = OnSurfaceVariant) } },
        containerColor = SurfaceContainerHigh,
        titleContentColor = OnSurface,
        shape = VaultRadius.xxl,
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
        containerColor = SurfaceContainerHigh,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant,
        shape = VaultRadius.xxl,
    )
}
