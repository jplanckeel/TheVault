package com.thevault.ui.screen.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.Engrave
import com.thevault.ui.component.FormatGroup
import com.thevault.ui.component.KeyButton
import com.thevault.ui.component.ListRow
import com.thevault.ui.component.LoadingBox
import com.thevault.ui.component.Meter
import com.thevault.ui.component.Panel
import com.thevault.ui.component.Pips
import com.thevault.ui.component.PriceDelta
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.SectionHeader
import com.thevault.ui.component.StatTile
import com.thevault.ui.component.color
import com.thevault.ui.component.formatEuro
import com.thevault.ui.component.formatLabel
import com.thevault.ui.component.formatPercent
import com.thevault.ui.component.shortPlatformName
import com.thevault.ui.navigation.tabBarClearance
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.StatsSummary
import com.thevault.ui.screen.collection.ValueSource
import com.thevault.ui.theme.Vault
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs

/** Stats: invested vs quoted, value per platform, games per condition, top value. */
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    onEntryClick: (CollectionEntry) -> Unit,
    onExport: () -> Unit,
    viewModel: StatsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = tabBarClearance()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val s = state?.summary
        item {
            val since = s?.since?.let { "Depuis ${MONTHS[it.month.ordinal]} ${it.year}" } ?: "Statistiques"
            ScreenHeader("Stats", eyebrow = since, leading = { KeyButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onBack) })
        }
        if (s == null) {
            item { LoadingBox() }
            return@LazyColumn
        }
        summaryItems(s)
        if (s.topPlatforms.isNotEmpty()) item { Platforms(s) }
        if (s.platformGains.isNotEmpty()) item { Gains(s.platformGains) }
        item { Conditions(s) }
        state?.let { st ->
            item { Months(st.months) }
            if (st.regions.size > 1 || st.regions.any { it.first.contains("·") }) item { Regions(st.regions) }
        }
        if (s.topValue.isNotEmpty()) {
            item { SectionHeader("Top 10 valeur", Modifier.padding(top = 8.dp), action = "Exporter CSV", onAction = onExport) }
            items(s.topValue, key = { "${it.kind}_${it.id}" }) { e ->
                ListRow(
                    e.title,
                    meta = "${shortPlatformName(e.platform)} · ${e.condition.formatLabel}",
                    trailing = (if (e.valueSource == ValueSource.UsIndicative) "≈ " else "") + formatEuro(e.value ?: 0.0),
                    cover = Triple(e.coverUrl, e.platform, e.condition),
                    onClick = { onEntryClick(e) },
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.summaryItems(s: StatsSummary) {
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(formatEuro(s.purchases.invested), "Investi", Modifier.weight(1f))
            StatTile(formatEuro(s.totalValue), "Cote actuelle", Modifier.weight(1f))
        }
    }
    if (s.purchases.filled > 0 && s.gainOnPriced != 0.0) {
        item {
            val invested = s.valueOfPriced - s.gainOnPriced
            val pct = if (invested > 0) " (${formatPercent(s.gainOnPriced / invested * PERCENT)})" else ""
            PriceDelta(formatEuro(abs(s.gainOnPriced)) + pct, s.gainOnPriced > 0, period = "plus-value", pill = true)
        }
    }
    if (s.purchases.missing > 0) {
        item {
            Text(
                "${s.purchases.missing} objets sans prix d'achat ne comptent pas dans la plus-value.",
                style = Vault.type.caption,
                color = Vault.colors.inkMuted,
            )
        }
    }
}

@Composable
private fun Platforms(s: StatsSummary) {
    val max = s.topPlatforms.maxOf { it.value }.takeIf { it > 0 } ?: 1.0
    Panel(Modifier.fillMaxWidth()) {
        Engrave("Valeur par plateforme")
        Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            s.topPlatforms.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        shortPlatformName(p.platform),
                        style = Vault.type.caption,
                        color = Vault.colors.ink,
                        modifier = Modifier.width(96.dp),
                        maxLines = 2,
                    )
                    Meter((p.value / max).toFloat(), p.manufacturer.color, Modifier.weight(1f), height = 8.dp)
                    Text(
                        formatEuro(p.value),
                        style = Vault.type.monoPrice.copy(fontSize = Vault.type.label.fontSize),
                        color = Vault.colors.ink,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(72.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Conditions(s: StatsSummary) {
    val groups = FormatGroup.entries.filter { it != FormatGroup.Digital || s.formats[it] ?: 0 > 0 }
    Panel(Modifier.fillMaxWidth()) {
        Engrave("Objets par état")
        Row(Modifier.padding(top = 14.dp)) {
            groups.forEach { g ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pips(g.pips)
                    Text(
                        "${s.formats[g] ?: 0}",
                        style = Vault.type.displayM.copy(fontSize = Vault.type.displayM.fontSize),
                        color = Vault.colors.ink,
                    )
                    Text(g.label, style = Vault.type.small, color = Vault.colors.inkMuted)
                }
            }
        }
    }
}

private const val PERCENT = 100.0
private val MONTHS =
    listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre")
