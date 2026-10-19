package com.thevault.ui.screen.accessory

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
import com.thevault.domain.model.Accessory
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

/** Accessory detail screen. */
@Composable
fun AccessoryDetailScreen(
    accessoryId: String,
    onBack: () -> Unit,
    viewModel: AccessoryDetailViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(accessoryId) { viewModel.loadAccessory(accessoryId) }
    AccessoryDetailContent(state, onBack)
}

/**
 * Stateless accessory detail.
 *
 * @param state UI state.
 * @param onBack Back navigation.
 */
@Composable
fun AccessoryDetailContent(
    state: AccessoryDetailUiState,
    onBack: () -> Unit,
) {
    val accessory = state.accessory
    if (state.isLoading || accessory == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DetailHero(
            title = accessory.name,
            platform = accessory.compatibleConsole,
            coverUrl = accessory.imageUrl,
            onBack = onBack,
        ) { AccessoryBadges(accessory) }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp), verticalArrangement = DetailSpacing) {
            ValueCard(accessory.currentValue, accessory.purchasePrice)
            InfoCard(
                "Informations",
                listOf(
                    "Type" to accessory.type,
                    "Compatible" to accessory.compatibleConsole,
                    "État" to accessory.condition.label,
                    "Statut" to accessory.status.label,
                    "Acquis le" to accessory.acquiredAt?.toString(),
                ),
            )
            NotesCard(accessory.notes)
            ExternalLinksSection(query = "${accessory.name} ${accessory.compatibleConsole}")
        }
    }
}

@Composable
private fun AccessoryBadges(accessory: Accessory) {
    if (accessory.type.isNotBlank()) VaultChip(accessory.type.uppercase(), tone = ChipTone.Primary)
    val badge = accessory.condition.toBadge()
    VaultChip(badge.label, tone = badge.tone, variant = ChipVariant.Solid)
    if (accessory.isCollectorEdition) VaultChip("COLLECTOR", tone = ChipTone.Warning)
    if (accessory.isLimitedEdition) VaultChip("ÉDITION LIMITÉE", tone = ChipTone.Secondary)
}
