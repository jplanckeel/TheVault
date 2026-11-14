package com.thevault.domain.usecase

import com.thevault.domain.model.AchievementUnlock
import com.thevault.domain.model.ValueSnapshot
import com.thevault.domain.repository.AchievementRepository
import com.thevault.domain.repository.SettingsRepository
import com.thevault.domain.repository.ValueHistoryRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes the daily value history.
 *
 * @param repository Value history repository.
 */
class ObserveValueHistoryUseCase(
    private val repository: ValueHistoryRepository,
) {
    /** Snapshots, oldest first. */
    operator fun invoke(): Flow<List<ValueSnapshot>> = repository.observe()
}

/**
 * Records today's value of the collection.
 *
 * @param repository Value history repository.
 */
class RecordValueSnapshotUseCase(
    private val repository: ValueHistoryRepository,
) {
    /** Stores [snapshot], replacing any snapshot of the same day. */
    suspend operator fun invoke(snapshot: ValueSnapshot) = repository.record(snapshot)
}

/**
 * Observes the unlocked achievements.
 *
 * @param repository Achievement repository.
 */
class ObserveAchievementUnlocksUseCase(
    private val repository: AchievementRepository,
) {
    /** Unlocks, oldest first. */
    operator fun invoke(): Flow<List<AchievementUnlock>> = repository.observe()
}

/**
 * Records newly unlocked achievements.
 *
 * @param repository Achievement repository.
 */
class RecordAchievementUnlocksUseCase(
    private val repository: AchievementRepository,
) {
    /** Stores [unlocks]; known identifiers are ignored. */
    suspend operator fun invoke(unlocks: List<AchievementUnlock>) = repository.record(unlocks)
}

/**
 * Marks an achievement notification as shown.
 *
 * @param repository Achievement repository.
 */
class MarkAchievementSeenUseCase(
    private val repository: AchievementRepository,
) {
    /** Marks [id] as seen. */
    suspend operator fun invoke(id: String) = repository.markSeen(id)
}

/**
 * Observes one preference.
 *
 * @param repository Settings repository.
 */
class ObserveSettingUseCase(
    private val repository: SettingsRepository,
) {
    /** Value of [key], null when unset. */
    operator fun invoke(key: String): Flow<String?> = repository.observe(key)
}

/**
 * Stores one preference.
 *
 * @param repository Settings repository.
 */
class SetSettingUseCase(
    private val repository: SettingsRepository,
) {
    /** Stores [value] under [key]. */
    suspend operator fun invoke(
        key: String,
        value: String,
    ) = repository.set(key, value)
}
