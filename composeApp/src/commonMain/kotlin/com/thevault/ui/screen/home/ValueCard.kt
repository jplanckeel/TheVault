package com.thevault.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.Panel
import com.thevault.ui.component.PriceDelta
import com.thevault.ui.component.Segment
import com.thevault.ui.component.SegmentedControl
import com.thevault.ui.component.SourceBadge
import com.thevault.ui.component.Sparkline
import com.thevault.ui.component.formatAge
import com.thevault.ui.component.formatEuro
import com.thevault.ui.component.formatPercent
import com.thevault.ui.screen.collection.ChartPeriod
import com.thevault.ui.screen.collection.HomeSummary
import com.thevault.ui.theme.Vault
import com.thevault.ui.theme.VaultRadius
import com.thevault.ui.theme.well
import kotlin.math.abs
import kotlin.time.Clock

/** « Valeur du coffre » card: amount, change over the period, chart and period selector. */
@Composable
internal fun ValueCard(
    state: HomeUiState,
    summary: HomeSummary,
    onPeriod: (ChartPeriod) -> Unit,
) {
    val c = Vault.colors
    Panel(Modifier.fillMaxWidth(), PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Valeur du coffre", style = Vault.type.caption, color = c.inkMuted, modifier = Modifier.weight(1f))
            val at = summary.quotedAt
            if (at != null) {
                SourceBadge("eBay", updated = formatAge(Clock.System.now() - at), stale = Clock.System.now() - at > STALE)
            } else {
                SourceBadge("Cotes manuelles", stale = true)
            }
        }
        Text(formatEuro(summary.totalValue), style = Vault.type.displayXl, color = c.ink, modifier = Modifier.padding(top = 12.dp))
        val change = state.change
        val first = state.points.firstOrNull()
        if (change != null && first != null && change != 0.0) {
            val pct = if (first > 0) " (${formatPercent(change / first * PERCENT)})" else ""
            PriceDelta(formatEuro(abs(change)) + pct, change > 0, Modifier.padding(top = 12.dp), period = state.period.suffix, pill = true)
        }
        ChartScreen(state)
        SegmentedControl(
            ChartPeriod.entries.map { Segment(it.label) },
            state.period.ordinal,
            { onPeriod(ChartPeriod.entries[it]) },
            Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun ChartScreen(state: HomeUiState) {
    val c = Vault.colors
    Box(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .well(VaultRadius.md)
            .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
    ) {
        if (state.points.size >= 2) {
            Sparkline(state.points)
        } else {
            Text(
                "La courbe se dessine jour après jour : reviens demain pour voir l'évolution.",
                style = Vault.type.caption,
                color = c.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().height(72.dp).padding(top = 18.dp, start = 8.dp, end = 8.dp),
            )
        }
    }
}

private val STALE = kotlin.time.Duration.parse("7d")
private const val PERCENT = 100.0
