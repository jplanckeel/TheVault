package com.thevault.ui.screen.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun CsvImportExportSection(
    modifier: Modifier,
    onImport: (String) -> Unit,
    onExport: () -> Unit,
    exportContentProvider: suspend () -> String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
                if (content != null) {
                    onImport(content)
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            onExport() // Notify UI it started
            scope.launch {
                val content = exportContentProvider()
                if (content != null) {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                            writer.write(content)
                        }
                    }
                }
            }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = { importLauncher.launch("text/csv") },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Importer CSV")
            Text("Importer", modifier = Modifier.padding(start = 8.dp))
        }

        Button(
            onClick = { exportLauncher.launch("collection_export.csv") },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Exporter CSV")
            Text("Exporter", modifier = Modifier.padding(start = 8.dp))
        }
    }
}
