package com.thevault.ui.screen.console

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
import com.thevault.domain.model.Console
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

/** Console detail. */
@Composable
fun ConsoleDetailScreen(
    consoleId: String,
    onBack: () -> Unit,
    viewModel: ConsoleDetailViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(consoleId) { viewModel.loadConsole(consoleId) }
    var statusDialog by remember { mutableStateOf(false) }
    val console = state.console
    if (state.isLoading || console == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
        return
    }
    if (statusDialog) {
        StatusDialog(console.status, {
            viewModel.setStatus(it)
            statusDialog = false
        }, { statusDialog = false })
    }
    Box(Modifier.fillMaxSize()) {
        ConsoleBody(console, onBack)
        DetailBottomBar({ statusDialog = true }, onEdit = null, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ConsoleBody(
    console: Console,
    onBack: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DetailTopBar(onBack)
        DetailHeader(console.name, console.name, console.imageUrl) {
            FormatBadge(console.condition)
            NeutralBadge(console.region.label)
            if (console.color.isNotBlank()) NeutralBadge(console.color)
        }
        QuoteCard(console.condition.formatLabel, console.currentValue, console.purchasePrice)
        InfoGrid(
            listOf(
                "Constructeur" to console.manufacturer,
                "Année" to console.productionYear.takeIf { it > 0 }?.toString(),
                "Couleur" to console.color,
                "Ajoutée le" to console.acquiredAt?.let { formatDate(it) },
                "Statut" to console.status.label,
            ),
        )
        NotesCard(console.notes, onEdit = null)
    }
}
