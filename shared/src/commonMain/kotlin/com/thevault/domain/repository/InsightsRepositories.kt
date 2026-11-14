package com.thevault.domain.repository

import com.thevault.domain.model.AchievementUnlock
import com.thevault.domain.model.PricePoint
import com.thevault.domain.model.PriceQuote
import com.thevault.domain.model.SyncOutcome
import com.thevault.domain.model.SyncPayload
import com.thevault.domain.model.ValueSnapshot
import kotlinx.coroutines.flow.Flow

/** eBay quotes collected by the backend, cached on the device. */
interface PriceRepository {
    /** Cached quotes, keyed by item identifier. */
    fun observeQuotes(): Flow<Map<String, List<PriceQuote>>>

    /**
     * Downloads the quotes of [itemIds] and replaces the cache.
     *
     * @return Number of quotes received.
     */
    suspend fun refreshQuotes(itemIds: List<String>): Int

    /** Price history of [itemId], oldest first. */
    suspend fun history(itemId: String): List<PricePoint>
}

/** Daily value of the collection, recorded on the device. */
interface ValueHistoryRepository {
    /** Every snapshot, oldest first. */
    fun observe(): Flow<List<ValueSnapshot>>

    /** Records (or replaces) the snapshot of its day. */
    suspend fun record(snapshot: ValueSnapshot)
}

/** Achievements unlocked on the device. */
interface AchievementRepository {
    /** Every unlock, oldest first. */
    fun observe(): Flow<List<AchievementUnlock>>

    /** Records new unlocks; already known identifiers are ignored. */
    suspend fun record(unlocks: List<AchievementUnlock>)

    /** Marks the unlock notification of [id] as shown. */
    suspend fun markSeen(id: String)
}

/** Small key / value preferences (theme, last refresh…). */
interface SettingsRepository {
    /** Value of [key], null when unset. */
    fun observe(key: String): Flow<String?>

    /** Current value of [key], null when unset. */
    suspend fun get(key: String): String?

    /** Stores [value] under [key]. */
    suspend fun set(
        key: String,
        value: String,
    )
}

/** Push of the collection to the shared catalogue, with a local queue of pending items. */
interface CatalogSyncRepository {
    /** Identifiers already pushed since their last local change. */
    fun observeSyncedIds(): Flow<Set<String>>

    /** Flags [itemId] as changed locally (to push again). */
    suspend fun markDirty(itemId: String)

    /** Pushes one item and records the outcome. */
    suspend fun push(payload: SyncPayload): SyncOutcome
}
