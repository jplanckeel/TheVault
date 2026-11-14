package com.thevault.ui.screen.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.usecase.ObservePriceSnapshotsUseCase
import com.thevault.ui.component.EmptyState
import com.thevault.ui.component.KeyButton
import com.thevault.ui.component.ListRow
import com.thevault.ui.component.PriceDelta
import com.thevault.ui.component.ScreenHeader
import com.thevault.ui.component.SectionHeader
import com.thevault.ui.component.formatEuro
import com.thevault.ui.component.formatLongDate
import com.thevault.ui.component.shortPlatformName
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.VaultEntriesSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs

/** ViewModel of the quote report. */
class QuoteReportViewModel(
    source: VaultEntriesSource,
    observeSnapshots: ObservePriceSnapshotsUseCase,
) : ViewModel() {
    /** Report of the last refresh, null before the second one. */
    val report: StateFlow<QuoteReport?> =
        combine(source.entries(), observeSnapshots()) { entries, snaps -> buildQuoteReport(entries, snaps) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/**
 * Report of the last quote refresh: rises in green, drops in red, net total.
 *
 * @param onBack Back navigation.
 * @param onEntryClick Opens an item.
 * @param viewModel Screen ViewModel.
 */
@Composable
fun QuoteReportScreen(
    onBack: () -> Unit,
    onEntryClick: (CollectionEntry) -> Unit,
    viewModel: QuoteReportViewModel = koinViewModel(),
) {
    val report by viewModel.report.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val r = report
        item {
            ScreenHeader(
                "Cotes",
                eyebrow = r?.let { "Mise à jour du ${formatLongDate(it.day)}" } ?: "Rapport",
                leading = { KeyButton(Icons.AutoMirrored.Filled.ArrowBack, "Retour", onBack) },
            )
        }
        if (r == null || r.isEmpty) {
            item {
                EmptyState(
                    Icons.AutoMirrored.Filled.TrendingFlat,
                    "Rien n'a bougé",
                    message = "Le rapport compare deux relevés de cotes eBay : il apparaît après la deuxième mise à jour.",
                )
            }
            return@LazyColumn
        }
        item { PriceDelta(formatEuro(abs(r.total)), r.total >= 0, period = "${r.ups.size} hausses · ${r.downs.size} baisses", pill = true) }
        changes("Hausses", r.ups, onEntryClick)
        changes("Baisses", r.downs, onEntryClick)
    }
}

private fun LazyListScope.changes(
    title: String,
    list: List<QuoteChange>,
    onEntryClick: (CollectionEntry) -> Unit,
) {
    if (list.isEmpty()) return
    item { SectionHeader(title, Modifier.padding(top = 12.dp)) }
    items(list, key = { "${title}_${it.entry.id}" }) { c ->
        ListRow(
            c.entry.title,
            meta = "${shortPlatformName(c.entry.platform)} · avant ${formatEuro(c.old)}",
            trailing = formatEuro(c.new),
            cover = Triple(c.entry.coverUrl, c.entry.platform, c.entry.condition),
            onClick = { onEntryClick(c.entry) },
            trailingContent = { PriceDelta(formatEuro(abs(c.delta)), c.delta > 0, small = true) },
        )
    }
}
