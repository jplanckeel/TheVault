package com.thevault.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.VaultButton
import com.thevault.ui.component.VaultButtonVariant

/** iOS implementation: file pickers are not available yet, buttons are shown disabled. */
@Composable
actual fun CsvImportExportSection(
    onImport: (String) -> Unit,
    exportContent: suspend () -> String?,
    modifier: Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VaultButton(
            text = "Importer",
            onClick = {},
            enabled = false,
            variant = VaultButtonVariant.Ghost,
            leadingIcon = Icons.Default.FileDownload,
            modifier = Modifier.weight(1f),
        )
        VaultButton(
            text = "Exporter",
            onClick = {},
            enabled = false,
            variant = VaultButtonVariant.Ghost,
            leadingIcon = Icons.Default.FileUpload,
            modifier = Modifier.weight(1f),
        )
    }
}
