package com.thevault.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.ui.app.AppViewModel
import com.thevault.ui.component.ButtonVariant
import com.thevault.ui.component.Engrave
import com.thevault.ui.component.KeyButton
import com.thevault.ui.component.LedColor
import com.thevault.ui.component.Panel
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.Segment
import com.thevault.ui.component.SegmentedControl
import com.thevault.ui.component.StatusLed
import com.thevault.ui.component.VaultButton
import com.thevault.ui.component.VaultChip
import com.thevault.ui.theme.ThemeMode
import com.thevault.ui.theme.Vault
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

private const val MESSAGE_DURATION_MS = 5_000L

/** Settings: shell, catalogue sync, CSV import / export and IGDB enrichment. */
@Composable
fun SettingsScreen(
    app: AppViewModel,
    onBack: () -> Unit,
    onCloud: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val mode by app.themeMode.collectAsState()
    val sync by app.syncStatus.collectAsState()
    AutoDismiss(state.message, viewModel::clearMessage)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("Réglages", eyebrow = "Système", leading = { KeyButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onBack) })
        state.message?.let { Feedback(it) }
        MoreSections(app, viewModel, onCloud)
        Section("Coque", "Le thème de l'application.") {
            SegmentedControl(ThemeMode.entries.map { Segment(it.label) }, mode.ordinal, { app.setTheme(ThemeMode.entries[it]) })
        }
        Section(
            "Synchronisation",
            "Ta collection est envoyée au serveur, qui complète les fiches (jaquette, studio, année) et relève les cotes eBay de chaque objet.",
        ) {
            SyncContent(state, sync, viewModel)
        }
        Section("Import / export CSV", "Importe un export GameEye (réimporter met à jour sans doublon) ou sauvegarde ta collection.") {
            CsvImportExportSection(onImport = viewModel::importCsv, exportContent = viewModel::exportCsv)
        }
        Section(
            "Cotes",
            "D'où vient la valeur de tes objets. Les prix US de l'import Gameye servent seulement en attendant une cote eBay PAL fiable.",
        ) {
            ValuationContent(state.valuation)
        }
    }
}

@Composable
private fun SyncContent(
    state: SettingsUiState,
    sync: com.thevault.domain.usecase.SyncStatus,
    viewModel: SettingsViewModel,
) {
    val (led, text) =
        when {
            sync.running -> LedColor.Indigo to "Envoi en cours…"
            state.pending == 0 -> LedColor.Sync to "${state.total} objets synchronisés"
            else -> LedColor.Power to "${state.pending} objets en attente d'envoi"
        }
    StatusLed(led, label = text, blink = sync.running)
    VaultButton(
        if (state.isSyncing) "Mise à jour…" else "Mettre à jour fiches et cotes",
        viewModel::syncNow,
        Modifier.fillMaxWidth(),
        ButtonVariant.Secondary,
        icon = Icons.Default.Sync,
        enabled = !state.isSyncing,
    )
}

@Composable
private fun ValuationContent(split: ValuationSplit) {
    ValuationRow("Cote eBay PAL", split.ebay, LedColor.Sync)
    ValuationRow("Cote US indicative (Gameye)", split.usIndicative, LedColor.Power)
    ValuationRow("Valeur saisie", split.manual, LedColor.Indigo)
    ValuationRow("Sans valeur", split.none, null)
    if (split.usIndicative > 0) {
        Text(
            "Un objet passe à la cote eBay dès que le serveur a trouvé au moins 3 annonces PAL pour son état. " +
                "Il faut qu'il soit synchronisé ; le serveur cote en priorité les objets jamais cotés.",
            style = Vault.type.caption,
            color = Vault.colors.inkMuted,
        )
    }
}

@Composable
private fun ValuationRow(
    label: String,
    count: Int,
    led: LedColor?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (led != null) StatusLed(led) else Spacer(Modifier.width(8.dp))
        Text(label, style = Vault.type.body, color = Vault.colors.ink, modifier = Modifier.weight(1f).padding(start = 10.dp))
        Text(count.toString(), style = Vault.type.monoPrice, color = Vault.colors.ink)
    }
}

@Composable
private fun AutoDismiss(
    message: SettingsMessage?,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(MESSAGE_DURATION_MS)
            onDismiss()
        }
    }
}

@Composable
private fun Section(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Panel(Modifier.fillMaxWidth()) {
        Engrave(title)
        Text(
            description,
            style = Vault.type.caption,
            color = Vault.colors.inkMuted,
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun Feedback(message: SettingsMessage) {
    val led =
        when (message.tone) {
            MessageTone.Info -> LedColor.Indigo
            MessageTone.Success -> LedColor.Sync
            MessageTone.Error -> LedColor.Power
        }
    Panel(Modifier.fillMaxWidth()) {
        StatusLed(led, blink = message.tone == MessageTone.Info)
        Text(message.text, style = Vault.type.bodyStrong, color = Vault.colors.ink, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun MoreSections(
    app: AppViewModel,
    viewModel: SettingsViewModel,
    onCloud: () -> Unit,
) {
    val hide by app.hidePrices.collectAsState()
    Section("Compte et partage", "Sauvegarde sur ton compte et liens de partage en lecture seule.") {
        VaultButton("Ouvrir", onCloud, Modifier.fillMaxWidth(), ButtonVariant.Secondary, icon = Icons.Default.CloudSync)
    }
    Section("Confidentialité", "Masque tous les montants à l'écran (pour montrer l'app sans dévoiler les prix).") {
        VaultChip("Masquer les prix", hide, { app.setHidePrices(!hide) })
    }
    Section("Inventaire pour l'assurance", "Liste des objets possédés avec prix payés et valeurs, en PDF ou CSV.") {
        InsuranceExportSection(viewModel::insuranceLines)
    }
}
