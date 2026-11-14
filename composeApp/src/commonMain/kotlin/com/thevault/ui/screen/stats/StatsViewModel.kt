package com.thevault.ui.screen.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.ui.screen.collection.StatsSummary
import com.thevault.ui.screen.collection.VaultEntriesSource
import com.thevault.ui.screen.collection.buildStatsSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** ViewModel of the stats screen. */
class StatsViewModel(
    source: VaultEntriesSource,
) : ViewModel() {
    /** Stats data, null while loading. */
    val summary: StateFlow<StatsSummary?> =
        source
            .entries()
            .map { buildStatsSummary(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
