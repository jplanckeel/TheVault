package com.thevault.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.VaultEntriesSource
import com.thevault.ui.screen.collection.owned
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * UI state of the profile.
 *
 * @property level Player level.
 * @property items Owned items.
 * @property platforms Game platforms.
 * @property achievements Achievements, unlocked first.
 * @property unlocked Unlocked achievements.
 */
data class ProfileUiState(
    val level: PlayerLevel = levelOf(0),
    val items: Int = 0,
    val platforms: Int = 0,
    val achievements: List<AchievementProgress> = emptyList(),
    val unlocked: Int = 0,
)

/** ViewModel of the profile. */
class ProfileViewModel(
    source: VaultEntriesSource,
) : ViewModel() {
    /** Observable UI state, null while loading. */
    val uiState: StateFlow<ProfileUiState?> =
        source
            .entries()
            .map { all ->
                val owned = all.owned()
                val progress = achievementProgress(all)
                ProfileUiState(
                    level = levelOf(totalXp(owned.size, progress)),
                    items = owned.size,
                    platforms =
                        owned
                            .filter { it.kind == ItemKind.Game }
                            .map { it.platform }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .size,
                    achievements =
                        progress.sortedWith(
                            compareByDescending<AchievementProgress> { it.unlocked }.thenByDescending {
                                it.progress.toDouble() /
                                    it.def.target
                            },
                        ),
                    unlocked = progress.count { it.unlocked },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
