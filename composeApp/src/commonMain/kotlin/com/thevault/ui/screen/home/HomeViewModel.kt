package com.thevault.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.usecase.ObserveSyncedIdsUseCase
import com.thevault.domain.usecase.ObserveValueHistoryUseCase
import com.thevault.ui.screen.collection.ChartPeriod
import com.thevault.ui.screen.collection.HomeSummary
import com.thevault.ui.screen.collection.VaultEntriesSource
import com.thevault.ui.screen.collection.buildHomeSummary
import com.thevault.ui.screen.collection.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * UI state of the home screen.
 *
 * @property summary Collection summary, null while loading.
 * @property period Selected chart period.
 * @property points Daily values of the period, oldest first.
 * @property change Change over the period, null with fewer than two points.
 * @property pending Items not pushed to the catalogue yet.
 */
data class HomeUiState(
    val summary: HomeSummary? = null,
    val period: ChartPeriod = ChartPeriod.Month,
    val points: List<Double> = emptyList(),
    val change: Double? = null,
    val pending: Int = 0,
)

/** ViewModel of the home screen. */
class HomeViewModel(
    source: VaultEntriesSource,
    observeHistory: ObserveValueHistoryUseCase,
    observeSynced: ObserveSyncedIdsUseCase,
) : ViewModel() {
    private val period = MutableStateFlow(ChartPeriod.Month)

    /** Observable UI state. */
    val uiState: StateFlow<HomeUiState> =
        combine(source.entries(), observeHistory(), observeSynced(), period) { entries, history, synced, p ->
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val points = p.window(history, today).map { it.value }
            HomeUiState(
                summary = buildHomeSummary(entries),
                period = p,
                points = points,
                change = if (points.size >= 2) points.last() - points.first() else null,
                pending = entries.count { it.id !in synced },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    /** Selects the chart period. */
    fun onPeriodSelected(value: ChartPeriod) {
        period.value = value
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
