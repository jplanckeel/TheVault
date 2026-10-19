package com.thevault.ui.screen.accessory

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.DetailBottomBar
import com.thevault.ui.component.DetailHeader
import com.thevault.ui.component.DetailTopBar
import com.thevault.ui.component.FormatBadge
import com.thevault.ui.component.InfoGrid
import com.thevault.ui.component.NeutralBadge
import com.thevault.ui.component.NotesCard
import com.thevault.ui.component.QuoteCard
import com.thevault.ui.component.StatusDialog
import com.thevault.ui.component.formatDate
import com.thevault.ui.component.formatLabel
import com.thevault.ui.component.label
import com.thevault.ui.theme.Primary
import org.koin.compose.viewmodel.koinViewModel

/** Accessory detail. */
@Composable
fun AccessoryDetailScreen(
    accessoryId: String,
    onBack: () -> Unit,
    viewModel: AccessoryDetailViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(accessoryId) { viewModel.loadAccessory(accessoryId) }
    var statusDialog by remember { mutableStateOf(false) }
    val accessory = state.accessory
    if (state.isLoading || accessory == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
        return
    }
    if (statusDialog) {
        StatusDialog(accessory.status, {
            viewModel.setStatus(it)
            statusDialog = false
        }, { statusDialog = false })
    }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DetailTopBar(onBack)
            DetailHeader(accessory.name, accessory.compatibleConsole, accessory.imageUrl) {
                FormatBadge(accessory.condition)
                NeutralBadge(accessory.type)
            }
            QuoteCard(accessory.condition.formatLabel, accessory.currentValue, accessory.purchasePrice)
            InfoGrid(
                listOf(
                    "Type" to accessory.type,
                    "Compatible" to accessory.compatibleConsole,
                    "Ajouté le" to accessory.acquiredAt?.let { formatDate(it) },
                    "Statut" to accessory.status.label,
                ),
            )
            NotesCard(accessory.notes, onEdit = null)
        }
        DetailBottomBar({ statusDialog = true }, onEdit = null, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
