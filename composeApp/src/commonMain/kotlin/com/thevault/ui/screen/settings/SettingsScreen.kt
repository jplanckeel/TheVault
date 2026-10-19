package com.thevault.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.VaultButton
import com.thevault.ui.component.VaultButtonVariant
import com.thevault.ui.component.VaultCard
import com.thevault.ui.theme.Error
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.Success
import com.thevault.ui.theme.VaultRadius
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

private const val MESSAGE_DURATION_MS = 5_000L

/** Settings screen: data import / export and synchronisation. */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    SettingsContent(
        state = state,
        onImportCsv = viewModel::importCsv,
        exportCsv = viewModel::exportCsv,
        onSyncIgdb = viewModel::syncIgdb,
        onMessageShown = viewModel::clearMessage,
    )
}

/**
 * Stateless settings content.
 *
 * @param state UI state.
 * @param onImportCsv Called with the picked CSV content.
 * @param exportCsv Produces the CSV export.
 * @param onSyncIgdb Starts the IGDB enrichment.
 * @param onMessageShown Called once the feedback message has been displayed long enough.
 */
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onImportCsv: (String) -> Unit,
    exportCsv: suspend () -> String?,
    onSyncIgdb: () -> Unit,
    onMessageShown: () -> Unit,
) {
    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(MESSAGE_DURATION_MS)
            onMessageShown()
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(title = "Réglages", subtitle = "Données et synchronisation")
        state.message?.let { FeedbackBanner(it) }
        SettingsSection(
            icon = Icons.Default.Description,
            title = "Import / export CSV",
            description = "Importez un export GameEye ou sauvegardez votre collection au format CSV.",
        ) {
            CsvImportExportSection(onImport = onImportCsv, exportContent = exportCsv)
        }
        SettingsSection(
            icon = Icons.Default.CloudSync,
            title = "Enrichissement IGDB",
            description = "Complète les jaquettes, genres, éditeurs et dates de sortie de vos jeux.",
        ) {
            VaultButton(
                text = if (state.isSyncing) "Synchronisation…" else "Synchroniser avec IGDB",
                onClick = onSyncIgdb,
                enabled = !state.isSyncing,
                variant = VaultButtonVariant.Subtle,
                leadingIcon = Icons.Default.Sync,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SettingsSection(
    icon: ImageVector,
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    VaultCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(36.dp).clip(VaultRadius.md).background(Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, color = OnSurface)
        }
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        content()
    }
}

@Composable
private fun FeedbackBanner(message: SettingsMessage) {
    val color =
        when (message.tone) {
            MessageTone.Info -> Primary
            MessageTone.Success -> Success
            MessageTone.Error -> Error
        }
    Text(
        message.text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(VaultRadius.lg)
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
