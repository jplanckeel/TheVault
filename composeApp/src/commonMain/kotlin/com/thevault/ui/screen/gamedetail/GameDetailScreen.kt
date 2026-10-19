package com.thevault.ui.screen.gamedetail

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
import com.thevault.domain.model.Game
import com.thevault.ui.component.DeleteConfirmDialog
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

/** Game detail (« Fiche jeu »). */
@Composable
fun GameDetailScreen(
    gameId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: GameDetailViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(gameId) { viewModel.loadGame(gameId) }
    LaunchedEffect(state.isDeleted) { if (state.isDeleted) onBack() }
    var dialog by remember { mutableStateOf<DetailDialog?>(null) }
    val game = state.game
    if (state.isLoading || game == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
        return
    }
    when (dialog) {
        DetailDialog.Status ->
            StatusDialog(game.status, {
                viewModel.setStatus(it)
                dialog = null
            }, { dialog = null })
        DetailDialog.Delete ->
            DeleteConfirmDialog(game.title, {
                dialog = null
                viewModel.delete()
            }, { dialog = null })
        null -> Unit
    }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DetailTopBar(onBack, onDelete = { dialog = DetailDialog.Delete })
            GameHeader(game)
            QuoteCard(game.condition.formatLabel, game.currentValue, game.purchasePrice)
            InfoGrid(gameInfo(game))
            NotesCard(game.notes, onEdit)
        }
        DetailBottomBar({ dialog = DetailDialog.Status }, onEdit, Modifier.align(Alignment.BottomCenter))
    }
}

/** Dialogs of a detail screen. */
internal enum class DetailDialog {
    /** Status picker. */
    Status,

    /** Delete confirmation. */
    Delete,
}

@Composable
private fun GameHeader(game: Game) {
    DetailHeader(game.title, game.platform, game.coverUrl) {
        FormatBadge(game.condition)
        NeutralBadge(game.region.label)
        NeutralBadge(game.genre.takeIf { it != "N/A" }.orEmpty())
    }
}

private fun gameInfo(game: Game): List<Pair<String, String?>> =
    listOf(
        "Éditeur" to game.publisher,
        "Développeur" to game.developer,
        "Note" to game.rating?.takeIf { it > 0 }?.toString(),
        "Ajouté le" to game.acquiredAt?.let { formatDate(it) },
        "Sortie" to game.releaseYear.takeIf { it > 0 }?.toString(),
        "Statut" to game.status.label,
    )
