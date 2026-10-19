package com.thevault.ui.screen.gamedetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
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
import com.thevault.ui.component.ChipTone
import com.thevault.ui.component.ChipVariant
import com.thevault.ui.component.DeleteConfirmDialog
import com.thevault.ui.component.DetailHero
import com.thevault.ui.component.DetailSpacing
import com.thevault.ui.component.InfoCard
import com.thevault.ui.component.NotesCard
import com.thevault.ui.component.ValueCard
import com.thevault.ui.component.VaultButton
import com.thevault.ui.component.VaultChip
import com.thevault.ui.component.label
import com.thevault.ui.component.toBadge
import com.thevault.ui.theme.Primary
import org.koin.compose.viewmodel.koinViewModel

/** Game detail screen. */
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
    GameDetailContent(state, onBack, onEdit, viewModel::deleteGame)
}

/**
 * Stateless game detail.
 *
 * @param state UI state.
 * @param onBack Back navigation.
 * @param onEdit Opens the edit form.
 * @param onDelete Deletes the game (after confirmation).
 */
@Composable
fun GameDetailContent(
    state: GameDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val game = state.game
    if (state.isLoading || game == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
        return
    }
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        DeleteConfirmDialog(game.title, onConfirm = {
            confirmDelete = false
            onDelete()
        }, onDismiss = { confirmDelete = false })
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DetailHero(
            title = game.title,
            platform = game.platform,
            coverUrl = game.coverUrl,
            onBack = onBack,
            onDelete = { confirmDelete = true },
        ) { GameBadges(game) }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp), verticalArrangement = DetailSpacing) {
            VaultButton(text = "Modifier", onClick = onEdit, leadingIcon = Icons.Default.Edit, modifier = Modifier.fillMaxWidth())
            ValueCard(game.currentValue, game.purchasePrice)
            InfoCard("Informations", gameInfoRows(game))
            NotesCard(game.notes)
            ExternalLinksSection(
                query = "${game.title} ${game.platform}",
                youtubeQuery = "${game.title} ${game.platform} gameplay review",
            )
        }
    }
}

@Composable
private fun GameBadges(game: Game) {
    VaultChip(game.platform.uppercase(), tone = ChipTone.Primary)
    val badge = game.condition.toBadge()
    VaultChip(badge.label, tone = badge.tone, variant = ChipVariant.Solid)
    if (game.isCollectorEdition) VaultChip("COLLECTOR", tone = ChipTone.Warning)
    if (game.isLimitedEdition) VaultChip("ÉDITION LIMITÉE", tone = ChipTone.Secondary)
    game.rating?.takeIf { it > 0 }?.let { VaultChip("$it", tone = ChipTone.Neutral, icon = Icons.Default.Star) }
}

private fun gameInfoRows(game: Game): List<Pair<String, String?>> =
    listOf(
        "Plateforme" to game.platform,
        "Région" to game.region.label,
        "État" to game.condition.label,
        "Statut" to game.status.label,
        "Genre" to game.genre.takeIf { it != "N/A" },
        "Développeur" to game.developer,
        "Éditeur" to game.publisher,
        "Année" to game.releaseYear.takeIf { it > 0 }?.toString(),
        "Acquis le" to game.acquiredAt?.toString(),
    )
