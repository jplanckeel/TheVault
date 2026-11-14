package com.thevault.ui.screen.gamedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.Edition
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemStatus
import com.thevault.ui.component.ButtonVariant
import com.thevault.ui.component.CopyPanel
import com.thevault.ui.component.CopyPurchase
import com.thevault.ui.component.DeleteConfirmDialog
import com.thevault.ui.component.DetailHeader
import com.thevault.ui.component.DetailTopBar
import com.thevault.ui.component.Engrave
import com.thevault.ui.component.LoadingBox
import com.thevault.ui.component.MarketSection
import com.thevault.ui.component.MenuEntry
import com.thevault.ui.component.Panel
import com.thevault.ui.component.StatusDialog
import com.thevault.ui.component.VaultButton
import com.thevault.ui.component.label
import com.thevault.ui.component.needsDetails
import com.thevault.ui.screen.collection.ValueSource
import com.thevault.ui.theme.Vault
import org.koin.compose.viewmodel.koinViewModel

/** Dialogs of a detail screen. */
internal enum class DetailDialog {
    /** Status picker. */
    Status,

    /** Delete confirmation. */
    Delete,
}

/** Game detail (« Fiche jeu »). */
@Composable
fun GameDetailScreen(
    gameId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: GameDetailViewModel = koinViewModel(),
) {
    LaunchedEffect(gameId) { viewModel.loadGame(gameId) }
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.isDeleted) { if (state.isDeleted) onBack() }
    var dialog by remember { mutableStateOf<DetailDialog?>(null) }
    val game = state.game
    if (game == null) {
        LoadingBox()
        return
    }
    DetailDialogs(dialog, game, viewModel, onEdit) { dialog = null }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DetailTopBar(
            onBack,
            caption = game.status.label,
            menu =
                listOf(
                    MenuEntry("Modifier la fiche", onClick = onEdit),
                    MenuEntry("Changer le statut", onClick = { dialog = DetailDialog.Status }),
                    MenuEntry("Supprimer", destructive = true, onClick = { dialog = DetailDialog.Delete }),
                ),
        )
        DetailHeader(
            game.title,
            game.platform,
            game.region,
            game.details.regionVariant,
            game.coverUrl,
            credits(game),
            fromIgdb = game.coverUrl != null,
        )
        CopyPanel(
            game.details,
            CopyPurchase(game.purchasePrice, game.acquiredAt, state.value, state.valueSource == ValueSource.UsIndicative),
        )
        MarketSection(state.quotes, state.grade, state.history, viewModel::onGradeSelected, state.usPrice)
        BacklogPanel(game)
        NotesPanel(game.notes)
        val wish = game.status == ItemStatus.WISHLIST
        DetailButtons(wish, { if (wish) viewModel.setStatus(ItemStatus.OWNED) else dialog = DetailDialog.Status }, onEdit)
    }
}

@Composable
private fun BacklogPanel(game: Game) {
    val parts =
        listOfNotNull(
            game.play.status?.label,
            game.play.minutes?.let { m ->
                "${m / MINUTES_PER_HOUR} h" +
                    if (m % MINUTES_PER_HOUR != 0) " ${m % MINUTES_PER_HOUR} min" else ""
            },
            game.rating?.takeIf { it > 0 }?.let { "note $it/10" },
        )
    if (parts.isEmpty()) return
    Panel(Modifier.fillMaxWidth()) {
        Engrave("Backlog")
        Text(parts.joinToString(" · "), style = Vault.type.bodyStrong, color = Vault.colors.ink, modifier = Modifier.padding(top = 8.dp))
    }
}

private const val MINUTES_PER_HOUR = 60

@Composable
private fun NotesPanel(notes: String?) {
    if (notes.isNullOrBlank()) return
    Panel(Modifier.fillMaxWidth()) {
        Engrave("Notes")
        Text(notes, style = Vault.type.body, color = Vault.colors.ink, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun DetailButtons(
    wish: Boolean,
    onStatus: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VaultButton(if (wish) "Je l'ai !" else "Statut", onStatus, Modifier.weight(1f), ButtonVariant.Secondary)
        VaultButton("Modifier", onEdit, Modifier.weight(1f))
    }
}

@Composable
private fun DetailDialogs(
    dialog: DetailDialog?,
    game: Game,
    viewModel: GameDetailViewModel,
    onEdit: () -> Unit,
    onClose: () -> Unit,
) {
    when (dialog) {
        DetailDialog.Status ->
            StatusDialog(game.status, {
                viewModel.setStatus(it)
                onClose()
                if (it.needsDetails) onEdit()
            }, { onClose() })
        DetailDialog.Delete ->
            DeleteConfirmDialog(game.title, {
                onClose()
                viewModel.delete()
            }, { onClose() })
        null -> Unit
    }
}

private fun credits(game: Game): String =
    listOfNotNull(
        game.developer?.takeIf { it.isNotBlank() && it != "N/A" },
        game.releaseYear.takeIf { it > 0 }?.toString(),
        game.genre.takeIf { it.isNotBlank() && it != "N/A" },
    ).joinToString(" · ")

private fun editions(game: Game): List<String> =
    listOfNotNull(
        game.details.edition
            .takeIf { it != Edition.STANDARD }
            ?.let { "Édition ${it.label.lowercase()}" },
    )
