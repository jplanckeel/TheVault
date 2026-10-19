package com.thevault.ui.screen.console

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.Console
import com.thevault.ui.component.ChipTone
import com.thevault.ui.component.ChipVariant
import com.thevault.ui.component.DetailHero
import com.thevault.ui.component.DetailSpacing
import com.thevault.ui.component.InfoCard
import com.thevault.ui.component.NotesCard
import com.thevault.ui.component.ValueCard
import com.thevault.ui.component.VaultChip
import com.thevault.ui.component.label
import com.thevault.ui.component.toBadge
import com.thevault.ui.screen.gamedetail.ExternalLinksSection
import com.thevault.ui.theme.Primary
import org.koin.compose.viewmodel.koinViewModel

/** Console detail screen. */
@Composable
fun ConsoleDetailScreen(
    consoleId: String,
    onBack: () -> Unit,
    viewModel: ConsoleDetailViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(consoleId) { viewModel.loadConsole(consoleId) }
    ConsoleDetailContent(state, onBack)
}

/**
 * Stateless console detail.
 *
 * @param state UI state.
 * @param onBack Back navigation.
 */
@Composable
fun ConsoleDetailContent(
    state: ConsoleDetailUiState,
    onBack: () -> Unit,
) {
    val console = state.console
    if (state.isLoading || console == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DetailHero(title = console.name, platform = console.manufacturer, coverUrl = console.imageUrl, onBack = onBack) {
            ConsoleBadges(console)
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp), verticalArrangement = DetailSpacing) {
            ValueCard(console.currentValue, console.purchasePrice)
            InfoCard(
                "Informations",
                listOf(
                    "Constructeur" to console.manufacturer,
                    "Région" to console.region.label,
                    "Couleur" to console.color,
                    "Année" to console.productionYear.takeIf { it > 0 }?.toString(),
                    "État" to console.condition.label,
                    "Statut" to console.status.label,
                    "Acquise le" to console.acquiredAt?.toString(),
                ),
            )
            NotesCard(console.notes)
            ExternalLinksSection(query = console.name, youtubeQuery = "${console.name} review")
        }
    }
}

@Composable
private fun ConsoleBadges(console: Console) {
    VaultChip(console.manufacturer.uppercase(), tone = ChipTone.Primary)
    val badge = console.condition.toBadge()
    VaultChip(badge.label, tone = badge.tone, variant = ChipVariant.Solid)
    if (console.isCollectorEdition) VaultChip("COLLECTOR", tone = ChipTone.Warning)
    if (console.isLimitedEdition) VaultChip("ÉDITION LIMITÉE", tone = ChipTone.Secondary)
}
