package com.thevault.ui.screen.settings

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.ButtonVariant
import com.thevault.ui.component.VaultButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Android implementation: system file pickers (open document / create document). */
@Composable
actual fun CsvImportExportSection(
    onImport: (String) -> Unit,
    exportContent: suspend () -> String?,
    modifier: Modifier,
) {
    val importLauncher = rememberImportLauncher(onImport)
    val exportLauncher = rememberExportLauncher(exportContent)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VaultButton(
            text = "Importer",
            onClick = { importLauncher.launch("text/*") },
            variant = ButtonVariant.Secondary,
            icon = Icons.Default.FileDownload,
            modifier = Modifier.weight(1f),
        )
        VaultButton(
            text = "Exporter",
            onClick = { exportLauncher.launch("thevault_collection.csv") },
            variant = ButtonVariant.Secondary,
            icon = Icons.Default.FileUpload,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Picks a text file and passes its content to [onImport]. */
@Composable
private fun rememberImportLauncher(onImport: (String) -> Unit): ManagedActivityResultLauncher<String, android.net.Uri?> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val content =
                withContext(Dispatchers.IO) {
                    context.contentResolver
                        .openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                }
            if (content != null) onImport(content)
        }
    }
}

/** Lets the user choose a destination, then writes the CSV produced by [exportContent]. */
@Composable
private fun rememberExportLauncher(exportContent: suspend () -> String?): ManagedActivityResultLauncher<String, android.net.Uri?> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val content = exportContent() ?: return@launch
            withContext(Dispatchers.IO) {
                context.contentResolver
                    .openOutputStream(uri)
                    ?.bufferedWriter()
                    ?.use { it.write(content) }
            }
        }
    }
}
