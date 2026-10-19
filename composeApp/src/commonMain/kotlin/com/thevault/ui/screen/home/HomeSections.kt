package com.thevault.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thevault.ui.component.Eyebrow
import com.thevault.ui.component.FormatBadge
import com.thevault.ui.component.ItemCover
import com.thevault.ui.component.PlatformBadge
import com.thevault.ui.component.PlatformLogo
import com.thevault.ui.component.ProgressTrack
import com.thevault.ui.component.SectionHeader
import com.thevault.ui.component.VaultCard
import com.thevault.ui.component.coverTint
import com.thevault.ui.component.formatEuro
import com.thevault.ui.component.gameCount
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.HomeSummary
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.PlatformSummary
import com.thevault.ui.theme.HighlightBorder
import com.thevault.ui.theme.HighlightContainer
import com.thevault.ui.theme.OnHighlightVariant
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Outline
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.SurfaceContainerHigh
import com.thevault.ui.theme.SurfaceContainerHighest
import com.thevault.ui.theme.VaultRadius

/** « Valeur estimée » card with counters and missing purchase prices. */
@Composable
internal fun ValueCard(
    summary: HomeSummary,
    actions: HomeActions,
) {
    VaultCard(Modifier.fillMaxWidth(), shape = VaultRadius.xxl, contentPadding = PaddingValues(20.dp)) {
        Eyebrow("Valeur estimée")
        Text(
            formatEuro(summary.totalValue),
            style = MaterialTheme.typography.displayLarge,
            color = OnSurface,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            "${summary.totalCount} objets · ${summary.platformCount} plateformes",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
        )
        Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ItemKind.entries.forEach { kind ->
                KindCounter(summary.byKind[kind]?.count ?: 0, kind.label, { actions.onOpenCollection(kind) }, Modifier.weight(1f))
            }
        }
        if (summary.purchases.missing > 0) MissingPurchaseBanner(summary, actions.onOpenStats)
    }
}

@Composable
private fun KindCounter(
    count: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
            .clip(VaultRadius.lg)
            .background(SurfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(
            "$count",
            style = MaterialTheme.typography.displaySmall.copy(fontSize = MaterialTheme.typography.headlineMedium.fontSize),
            color = OnSurface,
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun MissingPurchaseBanner(
    summary: HomeSummary,
    onOpen: () -> Unit,
) {
    val p = summary.purchases
    Row(
        Modifier
            .padding(top = 18.dp)
            .fillMaxWidth()
            .clip(VaultRadius.lg)
            .background(HighlightContainer)
            .border(1.dp, HighlightBorder, VaultRadius.lg)
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "${p.missing} objets sans prix d'achat",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                color = OnSurface,
            )
            Text(
                "Renseignés : ${p.filled} objets, ${formatEuro(p.invested)} investis",
                style = MaterialTheme.typography.bodySmall,
                color = OnHighlightVariant,
            )
        }
        Text("Voir", style = MaterialTheme.typography.titleSmall, color = Primary)
    }
}

/** « Ajouts récents » carousel. */
@Composable
internal fun RecentAdditions(
    entries: List<CollectionEntry>,
    actions: HomeActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader("Ajouts récents", actionLabel = "Tout voir", onAction = { actions.onOpenCollection(ItemKind.Game) })
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(entries, key = { "${it.kind}_${it.id}" }) { entry ->
                Column(Modifier.width(112.dp).clickable { actions.onEntryClick(entry) }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ItemCover(entry.coverUrl, entry.platform, entry.title, Modifier.width(112.dp).height(150.dp))
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = OnSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    PlatformBadge(entry.platform)
                }
            }
        }
    }
}

/** « Par plateforme » card: top platforms by value. */
@Composable
internal fun PlatformBreakdown(
    platforms: List<PlatformSummary>,
    actions: HomeActions,
) {
    val max = platforms.maxOf { it.value }.takeIf { it > 0 } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader("Par plateforme", actionLabel = "Stats", onAction = actions.onOpenStats)
        VaultCard(Modifier.fillMaxWidth(), shape = VaultRadius.xl, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
            platforms.forEachIndexed { index, p ->
                Column(
                    Modifier.clickable(onClick = { actions.onOpenPlatform(p.platform) }).padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlatformLogo(p.platform, size = 36.dp)
                        Box(Modifier.weight(1f)) { PlatformBadge(p.platform) }
                        Text(gameCount(p.count), style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                        Text(
                            formatEuro(p.value),
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurface,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(84.dp),
                        )
                    }
                    ProgressTrack((p.value / max).toFloat())
                }
                if (index < platforms.lastIndex) HorizontalDivider(color = SurfaceContainerHighest)
            }
        }
    }
}

/** « Pièces maîtresses » — the most valuable items. */
@Composable
internal fun Masterpieces(
    entries: List<CollectionEntry>,
    onClick: (CollectionEntry) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader("Pièces maîtresses")
        entries.forEachIndexed { index, entry -> MasterpieceRow(index + 1, entry, onClick) }
    }
}

@Composable
private fun MasterpieceRow(
    rank: Int,
    entry: CollectionEntry,
    onClick: (CollectionEntry) -> Unit,
) {
    VaultCard(
        Modifier.fillMaxWidth(),
        shape = VaultRadius.button,
        contentPadding = PaddingValues(12.dp),
        onClick = { onClick(entry) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(VaultRadius.md)
                    .background(coverTint(entry.platform))
                    .border(1.dp, Outline, VaultRadius.md),
                contentAlignment = Alignment.Center,
            ) {
                Text("$rank", style = MaterialTheme.typography.titleMedium, color = Primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                    color = OnSurface,
                    maxLines = 2,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlatformBadge(entry.platform)
                    FormatBadge(entry.condition)
                }
            }
            Text(formatEuro(entry.value ?: 0.0, decimals = 2), style = MaterialTheme.typography.titleMedium, color = OnSurface)
        }
    }
}
