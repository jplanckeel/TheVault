package com.thevault.ui.screen.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun CsvImportExportSection(
    modifier: Modifier,
    onImport: (String) -> Unit,
    onExport: () -> Unit,
    exportContentProvider: suspend () -> String?
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = { /* Not yet implemented for iOS */ },
            modifier = Modifier.weight(1f),
            enabled = false
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Importer CSV")
            Text("Importer", modifier = Modifier.padding(start = 8.dp))
        }

        Button(
            onClick = { /* Not yet implemented for iOS */ },
            modifier = Modifier.weight(1f),
            enabled = false
        ) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Exporter CSV")
            Text("Exporter", modifier = Modifier.padding(start = 8.dp))
        }
    }
}
