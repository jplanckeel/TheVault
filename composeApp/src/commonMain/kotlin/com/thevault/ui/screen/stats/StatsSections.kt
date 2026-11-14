package com.thevault.ui.screen.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thevault.ui.component.Engrave
import com.thevault.ui.component.Meter
import com.thevault.ui.component.Panel
import com.thevault.ui.component.PriceDelta
import com.thevault.ui.component.color
import com.thevault.ui.component.formatEuro
import com.thevault.ui.component.shortPlatformName
import com.thevault.ui.screen.collection.MonthSpend
import com.thevault.ui.screen.collection.PlatformGain
import com.thevault.ui.theme.Vault
import com.thevault.ui.theme.VaultRadius
import com.thevault.ui.theme.well
import kotlin.math.abs

/** Latent gain (value − price − shipping) per platform. */
@Composable
internal fun Gains(gains: List<PlatformGain>) {
    Panel(Modifier.fillMaxWidth()) {
        Engrave("Plus-value latente par plateforme")
        Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            gains.forEach { g ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(6.dp)
                            .height(18.dp)
                            .clip(VaultRadius.pill)
                            .background(g.manufacturer.color),
                    )
                    Text(
                        shortPlatformName(g.platform),
                        style = Vault.type.caption,
                        color = Vault.colors.ink,
                        modifier = Modifier.padding(start = 10.dp).weight(1f),
                    )
                    Text(
                        "payé ${formatEuro(g.cost)}",
                        style = Vault.type.monoTag,
                        color = Vault.colors.inkFaint,
                        modifier = Modifier.padding(end = 10.dp),
                    )
                    PriceDelta(formatEuro(abs(g.gain)), g.gain >= 0, small = true)
                }
            }
        }
    }
}

/** Purchases of the last 12 months: one bar per month, height by amount spent. */
@Composable
internal fun Months(months: List<MonthSpend>) {
    val max = months.maxOfOrNull { it.spent }?.takeIf { it > 0 } ?: 1.0
    val total = months.sumOf { it.spent }
    val count = months.sumOf { it.count }
    Panel(Modifier.fillMaxWidth()) {
        Engrave("Achats sur 12 mois · $count objets · ${formatEuro(total)}")
        Row(
            Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .height(96.dp)
                .well(VaultRadius.md)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            months.forEach { m ->
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight((m.spent / max).toFloat().coerceIn(if (m.count > 0) MIN_BAR else 0f, 1f) * BAR_SHARE)
                            .clip(VaultRadius.xs)
                            .background(Vault.colors.indigoInk),
                    )
                    Text(
                        MONTH_LETTERS[m.month - 1],
                        style = Vault.type.monoTag.copy(fontSize = 9.sp),
                        color = Vault.colors.inkFaint,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Text(
            "Montants payés + port, quand le prix est connu.",
            style = Vault.type.caption,
            color = Vault.colors.inkMuted,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Items per region and PAL variant. */
@Composable
internal fun Regions(regions: List<Pair<String, Int>>) {
    val total = regions.sumOf { it.second }.coerceAtLeast(1)
    Panel(Modifier.fillMaxWidth()) {
        Engrave("Régions et variantes")
        Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            regions.take(MAX_REGIONS).forEach { (label, n) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(label, style = Vault.type.monoTag, color = Vault.colors.ink, modifier = Modifier.width(96.dp))
                    Meter(n.toFloat() / total, Vault.colors.indigo, Modifier.weight(1f), height = 8.dp)
                    Text(
                        "$n",
                        style = Vault.type.monoTag,
                        color = Vault.colors.inkMuted,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

private val MONTH_LETTERS = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
private const val MIN_BAR = 0.04f
private const val BAR_SHARE = 0.8f
private const val MAX_REGIONS = 6
