package com.thevault.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.ui.screen.collection.HomeSummary
import com.thevault.ui.screen.collection.VaultEntriesSource
import com.thevault.ui.screen.collection.buildHomeSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** ViewModel of the home screen (« Ma collection »). */
class HomeViewModel(
    source: VaultEntriesSource,
) : ViewModel() {
    /** Home data, null while loading. */
    val summary: StateFlow<HomeSummary?> =
        source
            .entries()
            .map { buildHomeSummary(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
