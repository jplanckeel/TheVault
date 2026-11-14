package com.thevault.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thevault.domain.model.PriceGrade
import com.thevault.domain.model.PricePoint
import com.thevault.domain.model.PriceQuote
import com.thevault.domain.model.Region
import com.thevault.ui.theme.Vault
import com.thevault.ui.theme.VaultRadius
import com.thevault.ui.theme.well
import kotlin.time.Clock

/**
 * Detail top bar: back key, centered mono caption and a « ··· » menu.
 *
 * @param onBack Back callback.
 * @param caption Centered caption.
 * @param menu Menu entries.
 */
@Composable
fun DetailTopBar(
    onBack: () -> Unit,
    caption: String,
    menu: List<MenuEntry>,
) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        KeyButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onBack)
        Text(
            caption.uppercase(),
            style = Vault.type.monoTag,
            color = Vault.colors.inkFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Box {
            KeyButton(Icons.Default.MoreHoriz, "Plus d'actions", { open = true })
            VaultMenu(open, { open = false }, menu)
        }
    }
}

/**
 * Detail header: large cover, platform tag, title, credits and source.
 *
 * @param title Title.
 * @param platform Platform.
 * @param region Region.
 * @param variant Region variant.
 * @param coverUrl Cover URL.
 * @param credits Credits line.
 * @param fromIgdb Shows « Fiche IGDB ».
 */
@Suppress("LongParameterList")
@Composable
fun DetailHeader(
    title: String,
    platform: String,
    region: Region?,
    variant: String?,
    coverUrl: String?,
    credits: String,
    fromIgdb: Boolean,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        GameCover(coverUrl, title, platform, Modifier.width(132.dp), CoverSize.L)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PlatformTag(platform, region = region, variant = variant)
            val style =
                if (title.length >
                    LONG_TITLE
                ) {
                    Vault.type.displayM.copy(fontSize = 21.sp, lineHeight = 25.sp)
                } else {
                    Vault.type.displayL
                }
            Text(title, style = style, color = Vault.colors.ink)
            if (credits.isNotBlank()) Text(credits, style = Vault.type.caption, color = Vault.colors.inkMuted)
            if (fromIgdb) SourceBadge("Fiche IGDB")
        }
    }
}

/** Label of a price grade. */
val PriceGrade.label: String
    get() =
        when (this) {
            PriceGrade.LOOSE -> "Loose"
            PriceGrade.CIB -> "Complet"
            PriceGrade.NEW -> "Neuf"
        }

/**
 * « Cote eBay » section: one segment per grade with its median, the history chart of the
 * selected grade and the listings it was computed from.
 *
 * @param quotes Quotes of the item.
 * @param selected Selected grade.
 * @param history Price history.
 * @param onSelect Grade callback.
 * @param usPrice Indicative US price of the import, shown apart.
 */
@Composable
fun ColumnScope.MarketSection(
    quotes: List<PriceQuote>,
    selected: PriceGrade,
    history: List<PricePoint>,
    onSelect: (PriceGrade) -> Unit,
    usPrice: Double? = null,
) {
    val c = Vault.colors
    val latest = quotes.maxOfOrNull { it.scrapedAt }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Cote eBay", style = Vault.type.displayM, color = c.ink, modifier = Modifier.weight(1f))
        if (latest != null) SourceBadge("eBay", updated = formatAge(Clock.System.now() - latest)) else SourceBadge("eBay", stale = true)
    }
    if (usPrice != null) {
        Text(
            "Cote US indicative (import Gameye) : ${formatEuro(usPrice, decimals = 2)} — prix américain, pas une cote PAL.",
            style = Vault.type.caption,
            color = c.inkMuted,
        )
    }
    if (quotes.isEmpty()) {
        Text(
            "Pas encore de cote : le serveur relève les prix eBay des objets synchronisés plusieurs fois par semaine.",
            style = Vault.type.caption,
            color = c.inkMuted,
        )
        return
    }
    val grades = PriceGrade.entries
    SegmentedControl(
        grades.map { g -> Segment(g.label, formatEuroOrDash(quotes.find { it.grade == g }?.median)) },
        grades.indexOf(selected),
        { onSelect(grades[it]) },
        large = true,
    )
    val points = history.filter { it.grade == selected }.map { it.median }
    Box(Modifier.fillMaxWidth().well(VaultRadius.md).padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
        if (points.size >= 2) {
            Sparkline(points, description = "Évolution de la cote ${selected.label}")
        } else {
            Text("Historique en cours de constitution.", style = Vault.type.caption, color = c.inkMuted, modifier = Modifier.padding(8.dp))
        }
    }
    quotes.find { it.grade == selected }?.let { q ->
        Text(
            "${q.samples} annonces analysées · de ${formatEuro(q.min)} à ${formatEuro(q.max)}",
            style = Vault.type.caption,
            color = c.inkMuted,
        )
    }
}

private const val LONG_TITLE = 28
