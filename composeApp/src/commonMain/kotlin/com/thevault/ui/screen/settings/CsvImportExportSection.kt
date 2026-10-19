package com.thevault.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific CSV import / export buttons (file pickers).
 *
 * @param onImport Called with the content of the picked CSV file.
 * @param exportContent Produces the CSV to write once the user picked a destination.
 * @param modifier Modifier applied to the row of buttons.
 */
@Composable
expect fun CsvImportExportSection(
    onImport: (String) -> Unit,
    exportContent: suspend () -> String?,
    modifier: Modifier = Modifier,
)
