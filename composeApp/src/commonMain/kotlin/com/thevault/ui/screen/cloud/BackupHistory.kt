package com.thevault.ui.screen.cloud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.BackupShrinkException
import com.thevault.domain.model.BackupVersion
import com.thevault.ui.component.ButtonSize
import com.thevault.ui.component.ButtonVariant
import com.thevault.ui.component.Engrave
import com.thevault.ui.component.LinkText
import com.thevault.ui.component.VaultButton
import com.thevault.ui.theme.Vault
import com.thevault.ui.theme.VaultRadius
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Backups kept on the account, each one restorable.
 *
 * @param versions Saved backups, newest first.
 * @param enabled Whether a request may start.
 * @param onRestore Restores one version.
 */
@Composable
fun BackupHistory(
    versions: List<BackupVersion>,
    enabled: Boolean,
    onRestore: (Long) -> Unit,
) {
    if (versions.isEmpty()) return
    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Engrave("Sauvegardes gardées · ${versions.size}")
        versions.forEach { v ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${formatBackupTime(v.createdAt)} · ${v.itemCount} objets",
                    style = Vault.type.body,
                    color = Vault.colors.ink,
                    modifier = Modifier.weight(1f),
                )
                if (enabled) LinkText("Restaurer", { onRestore(v.id) })
            }
        }
    }
}

/**
 * Asks before a backup replaces a much bigger one (empty or new phone).
 *
 * @param shrink Counts of the refused backup.
 * @param onConfirm Replaces anyway.
 * @param onDismiss Keeps the server backup.
 */
@Composable
fun ShrinkConfirmDialog(
    shrink: BackupShrinkException,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Vault.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remplacer la sauvegarde ?", style = Vault.type.displayM) },
        text = {
            Text(
                "Le serveur garde ${shrink.currentCount} objets, ce téléphone en a ${shrink.newCount}. " +
                    "Si tu remplaces, les 10 dernières sauvegardes restent récupérables ici.",
                style = Vault.type.body,
            )
        },
        confirmButton = { VaultButton("Remplacer", onConfirm, variant = ButtonVariant.Danger, size = ButtonSize.Small) },
        dismissButton = { VaultButton("Annuler", onDismiss, variant = ButtonVariant.Ghost, size = ButtonSize.Small) },
        containerColor = c.panel,
        titleContentColor = c.ink,
        textContentColor = c.inkMuted,
        shape = VaultRadius.lg,
    )
}

/** « 15/11 · 10:42 » in the phone's time zone; the raw value when it cannot be read. */
internal fun formatBackupTime(iso: String): String =
    runCatching {
        val t = Instant.parse(iso).toLocalDateTime(TimeZone.currentSystemDefault())

        fun two(n: Int) = n.toString().padStart(2, '0')
        "${two(t.day)}/${two(t.month.ordinal + 1)}/${t.year} · ${two(t.hour)}:${two(t.minute)}"
    }.getOrDefault(iso)
