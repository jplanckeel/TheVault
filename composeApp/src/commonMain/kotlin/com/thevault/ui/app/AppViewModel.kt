package com.thevault.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.AchievementUnlock
import com.thevault.domain.model.ValueSnapshot
import com.thevault.domain.usecase.MarkAchievementSeenUseCase
import com.thevault.domain.usecase.ObserveAchievementUnlocksUseCase
import com.thevault.domain.usecase.ObserveSettingUseCase
import com.thevault.domain.usecase.RecordAchievementUnlocksUseCase
import com.thevault.domain.usecase.RecordValueSnapshotUseCase
import com.thevault.domain.usecase.RefreshCatalogUseCase
import com.thevault.domain.usecase.RefreshQuotesUseCase
import com.thevault.domain.usecase.SetSettingUseCase
import com.thevault.domain.usecase.SyncScheduler
import com.thevault.domain.usecase.SyncStatus
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.VaultEntriesSource
import com.thevault.ui.screen.collection.owned
import com.thevault.ui.screen.collection.totalValue
import com.thevault.ui.screen.profile.AchievementDef
import com.thevault.ui.screen.profile.Achievements
import com.thevault.ui.screen.profile.achievementProgress
import com.thevault.ui.theme.ThemeMode
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Application-wide work: shell, catalogue sync, quote refresh, daily value snapshot and
 * achievement unlocks.
 */
@Suppress("LongParameterList")
@OptIn(FlowPreview::class)
class AppViewModel(
    private val source: VaultEntriesSource,
    private val observeSetting: ObserveSettingUseCase,
    private val setSetting: SetSettingUseCase,
    private val scheduler: SyncScheduler,
    private val refreshQuotes: RefreshQuotesUseCase,
    private val refreshCatalog: RefreshCatalogUseCase,
    private val recordSnapshot: RecordValueSnapshotUseCase,
    observeUnlocks: ObserveAchievementUnlocksUseCase,
    private val recordUnlocks: RecordAchievementUnlocksUseCase,
    private val markSeen: MarkAchievementSeenUseCase,
) : ViewModel() {
    /** Shell chosen by the user. */
    val themeMode: StateFlow<ThemeMode> =
        observeSetting(ThemeMode.SETTING_KEY)
            .map { ThemeMode.fromKey(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.System)

    /** Whether amounts are hidden (« Masquer les prix »). */
    val hidePrices: StateFlow<Boolean> =
        observeSetting(KEY_HIDE_PRICES).map { it == "1" }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Hides or shows the amounts. */
    fun setHidePrices(hide: Boolean) {
        viewModelScope.launch { setSetting(KEY_HIDE_PRICES, if (hide) "1" else "0") }
    }

    /** Background sync state. */
    val syncStatus: StateFlow<SyncStatus> = scheduler.status

    /** Achievement to announce, null when none. */
    val toast: StateFlow<AchievementDef?> =
        observeUnlocks()
            .map { unlocks -> unlocks.firstOrNull { !it.seen }?.let { u -> Achievements.find { it.id == u.id } } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        scheduler.request()
        viewModelScope.launch { refreshQuotes(source.entries().first().map { it.id }) }
        viewModelScope.launch { refreshCatalog() }
        viewModelScope.launch {
            source.entries().debounce(SNAPSHOT_DEBOUNCE_MS).collect { entries ->
                recordToday(entries)
                recordNewUnlocks(entries)
            }
        }
        viewModelScope.launch {
            combine(
                scheduler.status.map { it.lastReport }.distinctUntilChanged(),
                source.entries(),
            ) { report, entries -> report to entries }
                .first { (report, _) -> report != null && report.synced > 0 }
                .let { (_, entries) -> refreshQuotes(entries.map { it.id }, force = true) }
        }
    }

    /** Stores the shell. */
    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { setSetting(ThemeMode.SETTING_KEY, mode.key) }
    }

    /** Hides the current achievement toast. */
    fun dismissToast(id: String) {
        viewModelScope.launch { markSeen(id) }
    }

    private suspend fun recordToday(entries: List<CollectionEntry>) {
        val owned = entries.owned()
        if (owned.isEmpty()) return
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        recordSnapshot(ValueSnapshot(today, owned.totalValue(), owned.sumOf { it.purchasePrice ?: 0.0 }))
    }

    private suspend fun recordNewUnlocks(entries: List<CollectionEntry>) {
        val unlocked = achievementProgress(entries).filter { it.unlocked }.map { it.def.id }
        if (unlocked.isEmpty()) return
        // The first computation only records what the collection already earned, silently.
        val firstRun = observeSetting(KEY_ACHIEVEMENTS_READY).first() == null
        val now = Clock.System.now()
        recordUnlocks(unlocked.map { AchievementUnlock(it, now, seen = firstRun) })
        if (firstRun) setSetting(KEY_ACHIEVEMENTS_READY, "1")
    }

    private companion object {
        const val SNAPSHOT_DEBOUNCE_MS = 1_500L
        const val KEY_ACHIEVEMENTS_READY = "achievements.ready"
        const val KEY_HIDE_PRICES = "ui.hide_prices"
    }
}
