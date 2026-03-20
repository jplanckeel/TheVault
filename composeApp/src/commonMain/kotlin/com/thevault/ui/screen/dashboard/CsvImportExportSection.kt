package com.thevault.ui.screen.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CsvImportExportSection(
    modifier: Modifier = Modifier,
    onImport: (String) -> Unit,
    onExport: () -> Unit,
    exportContentProvider: suspend () -> String?
)
