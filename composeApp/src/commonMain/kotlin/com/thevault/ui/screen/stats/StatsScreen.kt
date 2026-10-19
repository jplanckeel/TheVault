package com.thevault.ui.screen.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.Eyebrow
import com.thevault.ui.component.FormatGroup
import com.thevault.ui.component.PlatformBadge
import com.thevault.ui.component.ProgressTrack
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.VaultCard
import com.thevault.ui.component.formatEuro
import com.thevault.ui.component.gameCount
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.StatsSummary
import com.thevault.ui.theme.BadgeColors
import com.thevault.ui.theme.FormatBadgeColors
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.SurfaceContainerHighest
import com.thevault.ui.theme.VaultRadius
import org.koin.compose.viewmodel.koinViewModel

/** One color per category in the « Valeur par type » bar. */
private val KindColors =
    mapOf(ItemKind.Game to Primary, ItemKind.Console to Color(0xFF7F95E8), ItemKind.Accessory to Color(0xFF4E5CA8))

/** Stats screen. */
@Composable
fun StatsScreen(viewModel: StatsViewModel = koinViewModel()) {
    val summary by viewModel.summary.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader("Stats", summary = summary?.let { "${it.totalCount} objets" }) }
        summary?.let { s ->
            item { ValueByKind(s) }
            if (s.topPlatforms.isNotEmpty()) item { TopPlatforms(s) }
            item { Formats(s) }
            item { Purchases(s) }
        }
    }
}

@Composable
private fun ValueByKind(s: StatsSummary) {
    VaultCard(Modifier.fillMaxWidth()) {
        Eyebrow("Valeur par type")
        Text(
            formatEuro(s.totalValue),
            style = MaterialTheme.typography.displayLarge,
            color = OnSurface,
            modifier = Modifier.padding(top = 10.dp),
        )
        Row(
            Modifier
                .padding(vertical = 14.dp)
                .fillMaxWidth()
                .height(10.dp)
                .clip(VaultRadius.full)
                .background(SurfaceContainerHighest),
        ) {
            ItemKind.entries.forEach { kind ->
                val v = s.byKind[kind]?.value ?: 0.0
                if (v > 0 && s.totalValue > 0) {
                    Box(Modifier.weight((v / s.totalValue).toFloat()).fillMaxHeight().background(KindColors.getValue(kind)))
                }
            }
        }
        KindLegend(s)
    }
}

@Composable
private fun KindLegend(s: StatsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ItemKind.entries.forEach { kind ->
            val t = s.byKind[kind]
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(12.dp).clip(VaultRadius.badge).background(KindColors.getValue(kind)))
                Text(kind.label, style = MaterialTheme.typography.bodyLarge, color = OnSurface)
                Text(
                    "${t?.count ?: 0}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(formatEuro(t?.value ?: 0.0), style = MaterialTheme.typography.titleMedium, color = OnSurface)
            }
        }
    }
}

@Composable
private fun TopPlatforms(s: StatsSummary) {
    val max = s.topPlatforms.maxOf { it.value }.takeIf { it > 0 } ?: 1.0
    VaultCard(Modifier.fillMaxWidth()) {
        Eyebrow("Jeux · top plateformes en valeur", Modifier.padding(bottom = 14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            s.topPlatforms.forEach { p ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlatformBadge(p.platform)
                        Text(
                            gameCount(p.count),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(formatEuro(p.value), style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    }
                    ProgressTrack((p.value / max).toFloat(), height = 6.dp)
                }
            }
        }
    }
}

@Composable
private fun Formats(s: StatsSummary) {
    VaultCard(Modifier.fillMaxWidth()) {
        Eyebrow("Formats · ${s.totalCount} objets", Modifier.padding(bottom = 14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormatGroup.entries.forEach { group ->
                val count = s.formats[group] ?: 0
                val pct = if (s.totalCount > 0) "\n${count * PERCENT / s.totalCount}\u00A0%" else ""
                FormatTile(count, group.label + if (group == FormatGroup.Cib) pct else "", formatColors(group), Modifier.weight(1f))
            }
        }
    }
}

private const val PERCENT = 100

private fun formatColors(group: FormatGroup): BadgeColors =
    when (group) {
        FormatGroup.Cib -> FormatBadgeColors.Cib
        FormatGroup.Loose -> FormatBadgeColors.Loose
        FormatGroup.New -> FormatBadgeColors.New
        FormatGroup.Other -> FormatBadgeColors.Other
    }

@Composable
private fun FormatTile(
    count: Int,
    label: String,
    colors: BadgeColors,
    modifier: Modifier,
) {
    Column(modifier.clip(VaultRadius.lg).background(colors.container).padding(12.dp)) {
        Text("$count", style = MaterialTheme.typography.headlineMedium, color = OnSurface)
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.content, maxLines = 2)
    }
}

@Composable
private fun Purchases(s: StatsSummary) {
    val p = s.purchases
    VaultCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("Prix d'achat renseignés", Modifier.weight(1f))
            Text("${p.filled} / ${s.totalCount}", style = MaterialTheme.typography.titleMedium, color = OnSurface)
        }
        ProgressTrack(if (s.totalCount > 0) p.filled.toFloat() / s.totalCount else 0f, Modifier.padding(vertical = 14.dp), height = 6.dp)
        Text(
            "La plus-value globale n'est fiable que sur les objets dont tu as saisi le prix payé (${formatEuro(p.invested)} investis).",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
        )
    }
}
