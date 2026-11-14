package com.thevault.ui.screen.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.ui.screen.collection.MonthSpend
import com.thevault.ui.screen.collection.StatsSummary
import com.thevault.ui.screen.collection.VaultEntriesSource
import com.thevault.ui.screen.collection.buildStatsSummary
import com.thevault.ui.screen.collection.owned
import com.thevault.ui.screen.collection.purchasesByMonth
import com.thevault.ui.screen.collection.regionSplit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Data of the stats screen.
 *
 * @property summary Totals, platforms, conditions, top items, gains.
 * @property months Purchases of the last 12 months.
 * @property regions Items per region and variant.
 */
data class StatsUiState(
    val summary: StatsSummary,
    val months: List<MonthSpend>,
    val regions: List<Pair<String, Int>>,
)

/** ViewModel of the stats screen. */
class StatsViewModel(
    source: VaultEntriesSource,
) : ViewModel() {
    /** Stats data, null while loading. */
    val state: StateFlow<StatsUiState?> =
        source
            .entries()
            .map { all ->
                val owned = all.owned()
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                StatsUiState(buildStatsSummary(all), purchasesByMonth(owned, today), regionSplit(owned))
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
