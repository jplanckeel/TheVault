package com.thevault.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.ItemStatus
import com.thevault.ui.theme.Vault
import com.thevault.ui.theme.VaultRadius

/**
 * Entry of a [VaultMenu].
 *
 * @property label Label.
 * @property destructive Shown in red.
 * @property onClick Callback.
 */
data class MenuEntry(
    val label: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Drop-down menu styled as a panel.
 *
 * @param expanded Whether it is open.
 * @param onDismiss Dismiss callback.
 * @param entries Entries.
 */
@Composable
fun VaultMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    entries: List<MenuEntry>,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, containerColor = Vault.colors.panel, shape = VaultRadius.md) {
        entries.forEach { entry ->
            DropdownMenuItem(
                text = {
                    Text(
                        entry.label,
                        style = Vault.type.bodyStrong,
                        color = if (entry.destructive) Vault.colors.loss else Vault.colors.ink,
                    )
                },
                onClick = {
                    onDismiss()
                    entry.onClick()
                },
            )
        }
    }
}

/**
 * Status picker: owned, wishlist, sold or loaned.
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
    val c = Vault.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Statut de l'objet", style = Vault.type.displayM) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ItemStatus.entries.forEach { status ->
                    Row(
                        Modifier.fillMaxWidth().clickableNoRipple { onSelect(status) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(status.label, style = Vault.type.bodyStrong, color = c.ink, modifier = Modifier.weight(1f))
                        if (status == current) Icon(Icons.Default.Check, contentDescription = "Actuel", tint = c.indigoInk)
                    }
                }
            }
        },
        confirmButton = { VaultButton("Fermer", onDismiss, variant = ButtonVariant.Ghost, size = ButtonSize.Small) },
        containerColor = c.panel,
        titleContentColor = c.ink,
        shape = VaultRadius.lg,
    )
}

/**
 * Destructive confirmation.
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
    val c = Vault.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Supprimer « $itemName » ?", style = Vault.type.displayM) },
        text = { Text("L'objet sera retiré définitivement du coffre.", style = Vault.type.body) },
        confirmButton = { VaultButton("Supprimer", onConfirm, variant = ButtonVariant.Danger, size = ButtonSize.Small) },
        dismissButton = { VaultButton("Annuler", onDismiss, variant = ButtonVariant.Ghost, size = ButtonSize.Small) },
        containerColor = c.panel,
        titleContentColor = c.ink,
        textContentColor = c.inkMuted,
        shape = VaultRadius.lg,
    )
}
