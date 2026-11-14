package com.thevault.ui.screen.sets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thevault.domain.model.SetEntry
import com.thevault.ui.component.ButtonSize
import com.thevault.ui.component.ButtonVariant
import com.thevault.ui.component.Engrave
import com.thevault.ui.component.KeyButton
import com.thevault.ui.component.ListRow
import com.thevault.ui.component.LoadingBox
import com.thevault.ui.component.MenuEntry
import com.thevault.ui.component.Meter
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.SearchField
import com.thevault.ui.component.Segment
import com.thevault.ui.component.SegmentedControl
import com.thevault.ui.component.VaultButton
import com.thevault.ui.component.VaultMenu
import com.thevault.ui.component.clickableNoRipple
import com.thevault.ui.component.formatEuro
import com.thevault.ui.component.formatEuroOrDash
import com.thevault.ui.component.shortPlatformName
import com.thevault.ui.theme.Vault
import org.koin.compose.viewmodel.koinViewModel

/**
 * One completion list: progress, owned / missing titles, IGDB search to add titles and
 * « missing to wishlist » to get their quotes.
 *
 * @param setId List identifier.
 * @param onBack Back navigation.
 * @param viewModel Screen ViewModel.
 */
@Composable
fun SetDetailScreen(
    setId: String,
    onBack: () -> Unit,
    viewModel: SetDetailViewModel = koinViewModel(),
) {
    LaunchedEffect(setId) { viewModel.load(setId) }
    val completion by viewModel.completion.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val results by viewModel.results.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    val c = completion
    LazyColumn(
        Modifier.fillMaxSize().imePadding().windowInsetsPadding(WindowInsets.navigationBars),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Header(c, onBack, viewModel) }
        if (c == null) {
            item { LoadingBox() }
            return@LazyColumn
        }
        item { Progress(c, viewModel) }
        item {
            SegmentedControl(
                SetFilter.entries.map { Segment(it.label) },
                filter.ordinal,
                { viewModel.filter.value = SetFilter.entries[it] },
            )
        }
        val lines =
            when (filter) {
                SetFilter.All -> c.lines
                SetFilter.Owned -> c.lines.filter { it.isOwned }
                SetFilter.Missing -> c.missing
            }
        items(
            lines,
            key = { "${it.entry.title}|${it.entry.platform}" },
        ) { line -> Line(line) { viewModel.toggle(line.entry, add = false) } }
        item {
            Engrave("Ajouter des titres", Modifier.padding(top = 16.dp))
            SearchField(query, {
                query = it
                viewModel.search(it)
            }, "Rechercher sur IGDB", Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        searchResults(results, c, viewModel)
    }
}

@Composable
private fun Header(
    c: SetCompletion?,
    onBack: () -> Unit,
    viewModel: SetDetailViewModel,
) {
    var menu by remember { mutableStateOf(false) }
    ScreenHeader(
        c?.set?.name ?: "Liste",
        eyebrow = "Liste",
        leading = { KeyButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onBack) },
    ) {
        Box {
            KeyButton(Icons.Default.MoreHoriz, "Plus d'actions", { menu = true })
            VaultMenu(menu, { menu = false }, listOf(MenuEntry("Supprimer la liste", destructive = true) { viewModel.delete(onBack) }))
        }
    }
}

@Composable
private fun Progress(
    c: SetCompletion,
    viewModel: SetDetailViewModel,
) {
    Text("${c.ownedCount} / ${c.lines.size} · ${(c.ratio * PERCENT).toInt()} %", style = Vault.type.monoPrice, color = Vault.colors.ink)
    Meter(c.ratio, Vault.colors.indigo, Modifier.padding(top = 8.dp).fillMaxWidth(), height = 8.dp)
    if (c.missing.isNotEmpty()) {
        val cost = if (c.estimatedCost > 0) "≈ ${formatEuro(c.estimatedCost)} pour compléter" else "Coût inconnu"
        Text(
            "$cost${if (c.unquoted > 0) " · ${c.unquoted} titres sans cote" else ""}",
            style = Vault.type.caption,
            color = Vault.colors.inkMuted,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (c.missing.any { it.wished == null }) {
            VaultButton(
                "Suivre les manquants en envies",
                viewModel::wishMissing,
                Modifier.padding(top = 10.dp).fillMaxWidth(),
                ButtonVariant.Secondary,
                ButtonSize.Small,
            )
        }
    }
}

@Composable
private fun Line(
    line: SetLine,
    onRemove: () -> Unit,
) {
    val meta =
        shortPlatformName(line.entry.platform) +
            when {
                line.isOwned -> " · possédé"
                line.wished != null -> " · en envies"
                else -> " · manquant"
            }
    ListRow(
        line.entry.title,
        meta = meta,
        trailing = if (line.isOwned) null else formatEuroOrDash(line.missingCost),
        cover = Triple(line.owned?.coverUrl ?: line.wished?.coverUrl, line.entry.platform, line.owned?.condition),
        trailingContent = {
            Icon(
                if (line.isOwned) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (line.isOwned) "Possédé" else "Retirer de la liste",
                tint = if (line.isOwned) Vault.colors.ledSync else Vault.colors.inkFaint,
                modifier = if (line.isOwned) Modifier else Modifier.clickableNoRipple(onRemove),
            )
        },
    )
}

private fun LazyListScope.searchResults(
    results: List<com.thevault.domain.model.RemoteGame>,
    c: SetCompletion,
    viewModel: SetDetailViewModel,
) {
    val known = c.set.entries.toSet()
    items(
        results
            .flatMap { r ->
                r.platformNames.map { p ->
                    SetEntry(r.title, p) to r.coverUrl
                }
            }.filter { it.first !in known },
        key = { "r_${it.first}" },
    ) { (e, cover) ->
        ListRow(
            e.title,
            meta = shortPlatformName(e.platform),
            cover = Triple(cover, e.platform, null),
            onClick = { viewModel.toggle(e, add = true) },
            trailingContent = { Icon(Icons.Default.Add, contentDescription = "Ajouter", tint = Vault.colors.indigoInk) },
        )
    }
}

private const val PERCENT = 100
