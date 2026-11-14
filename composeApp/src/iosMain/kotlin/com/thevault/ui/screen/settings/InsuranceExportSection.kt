package com.thevault.ui.screen.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thevault.ui.theme.Vault

/** iOS: file pickers are not available yet. */
@Composable
actual fun InsuranceExportSection(
    lines: suspend () -> List<InsuranceLine>,
    modifier: Modifier,
) {
    Text("Export disponible sur Android pour l'instant.", style = Vault.type.caption, color = Vault.colors.inkMuted, modifier = modifier)
}
