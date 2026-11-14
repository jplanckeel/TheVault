package com.thevault.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.domain.usecase.SyncStatus
import com.thevault.ui.app.AppViewModel
import com.thevault.ui.component.CollectionCard
import com.thevault.ui.component.EmptyState
import com.thevault.ui.component.KeyButton
import com.thevault.ui.component.LedColor
import com.thevault.ui.component.LoadingBox
import com.thevault.ui.component.Panel
import com.thevault.ui.component.PriceDelta
import com.thevault.ui.component.SectionHeader
import com.thevault.ui.component.StatTile
import com.thevault.ui.component.StatusLed
import com.thevault.ui.component.VaultLogo
import com.thevault.ui.component.clickableNoRipple
import com.thevault.ui.component.formatCount
import com.thevault.ui.component.formatEuro
import com.thevault.ui.component.formatLongDate
import com.thevault.ui.navigation.tabBarClearance
import com.thevault.ui.screen.collection.ChartPeriod
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.report.QuoteReport
import com.thevault.ui.theme.Vault
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs

/** Home screen: vault value, counters and the biggest risers. */
@Composable
fun HomeScreen(
    actions: HomeActions,
    app: AppViewModel,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val sync by app.syncStatus.collectAsState()
    HomeContent(state, sync, actions, viewModel::onPeriodSelected)
}

/**
 * Stateless home content.
 *
 * @param state UI state.
 * @param sync Background sync state.
 * @param actions Navigation callbacks.
 * @param onPeriod Chart period callback.
 */
@Composable
fun HomeContent(
    state: HomeUiState,
    sync: SyncStatus,
    actions: HomeActions,
    onPeriod: (ChartPeriod) -> Unit,
) {
    val summary = state.summary
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = tabBarClearance()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HomeHeader(state.pending, sync, actions) }
        when {
            summary == null -> item { LoadingBox() }
            summary.totalCount == 0 ->
                item {
                    EmptyState(
                        Icons.Default.Inventory2,
                        "Ton coffre est vide",
                        message = "Ajoute ton premier jeu, console ou accessoire.",
                        action = "Ajouter",
                        onAction = actions.onAdd,
                    )
                }
            else -> {
                item { ValueCard(state, summary, onPeriod) }
                state.report?.takeIf { !it.isEmpty }?.let { r -> item { ReportBanner(r, actions.onOpenReport) } }
                item { Counters(summary.byKind.mapValues { it.value.count }, actions.onOpenCollection) }
                val risers = summary.risers.isNotEmpty()
                val cards = if (risers) summary.risers else summary.topValue
                if (cards.isNotEmpty()) item { Highlights(if (risers) "Plus fortes hausses" else "Pièces maîtresses", cards, actions) }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    pending: Int,
    sync: SyncStatus,
    actions: HomeActions,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VaultLogo(Modifier.weight(1f))
        val (led, label) =
            when {
                sync.running -> LedColor.Indigo to "Sync"
                pending > 0 && sync.lastReport?.failed ?: 0 > 0 -> LedColor.Power to "Hors ligne"
                pending > 0 -> LedColor.Off to "$pending en attente"
                else -> LedColor.Sync to "Sync"
            }
        StatusLed(led, Modifier.clickableNoRipple(actions.onSync), label = label, blink = sync.running)
        KeyButton(Icons.Default.Search, "Je l'ai déjà ?", actions.onSearch)
    }
}

@Composable
private fun Counters(
    counts: Map<ItemKind, Int>,
    onOpen: (ItemKind) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ItemKind.entries.forEach { kind ->
            StatTile(formatCount((counts[kind] ?: 0).toLong()), kind.label, Modifier.weight(1f), onClick = { onOpen(kind) })
        }
    }
}

@Composable
private fun Highlights(
    title: String,
    entries: List<CollectionEntry>,
    actions: HomeActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title, action = "Tout voir", onAction = actions.onOpenRisers)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            entries.forEach { entry -> CollectionCard(entry.card, { actions.onEntryClick(entry) }, Modifier.weight(1f)) }
            repeat(HIGHLIGHTS - entries.size) { Box(Modifier.weight(1f)) }
        }
    }
}

private const val HIGHLIGHTS = 3

@Composable
private fun ReportBanner(
    report: QuoteReport,
    onOpen: () -> Unit,
) {
    Panel(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusLed(LedColor.Sync)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Cotes mises à jour le ${formatLongDate(report.day)}", style = Vault.type.bodyStrong, color = Vault.colors.ink)
                PriceDelta(
                    formatEuro(abs(report.total)),
                    report.total >= 0,
                    period = "${report.ups.size} hausses · ${report.downs.size} baisses",
                    small = true,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Voir le rapport", tint = Vault.colors.inkFaint)
        }
    }
}
