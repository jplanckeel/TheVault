package com.thevault.domain.usecase

import com.thevault.domain.model.SyncOutcome
import com.thevault.domain.model.SyncPayload
import com.thevault.domain.model.SyncReport
import com.thevault.domain.model.toSyncPayload
import com.thevault.domain.repository.AccessoryRepository
import com.thevault.domain.repository.CatalogSyncRepository
import com.thevault.domain.repository.ConsoleRepository
import com.thevault.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * State of the background synchronisation.
 *
 * @property running Whether a run is in progress.
 * @property lastReport Result of the last run, null before the first one.
 */
data class SyncStatus(
    val running: Boolean = false,
    val lastReport: SyncReport? = null,
)

/** Runs [SyncPendingItemsUseCase] in the background, outside of any screen. */
interface SyncScheduler {
    /** Current state. */
    val status: StateFlow<SyncStatus>

    /** Asks for a run soon; requests made during a run trigger one more run. */
    fun request()
}

/**
 * Pushes every item changed since its last push to the shared catalogue, so that the
 * backend collects its eBay quotes. Stops after a few failures in a row (offline).
 *
 * @param games Game repository.
 * @param consoles Console repository.
 * @param accessories Accessory repository.
 * @param sync Catalogue sync repository.
 */
class SyncPendingItemsUseCase(
    private val games: GameRepository,
    private val consoles: ConsoleRepository,
    private val accessories: AccessoryRepository,
    private val sync: CatalogSyncRepository,
) {
    /** Pushes the pending items and reports what happened. */
    suspend operator fun invoke(): SyncReport {
        val synced = sync.observeSyncedIds().first()
        val pending = allPayloads().filter { it.externalId !in synced }
        var ok = 0
        var failedInARow = 0
        for (payload in pending) {
            when (sync.push(payload)) {
                SyncOutcome.Synced, SyncOutcome.Rejected -> {
                    ok++
                    failedInARow = 0
                }
                SyncOutcome.Failed -> failedInARow++
            }
            if (failedInARow >= MAX_FAILURES_IN_A_ROW) break
        }
        return SyncReport(synced = ok, failed = pending.size - ok)
    }

    private suspend fun allPayloads(): List<SyncPayload> =
        games.observeAll().first().map { it.toSyncPayload() } +
            consoles.observeAll().first().map { it.toSyncPayload() } +
            accessories.observeAll().first().map { it.toSyncPayload() }

    private companion object {
        const val MAX_FAILURES_IN_A_ROW = 3
    }
}

/**
 * Observes the identifiers already pushed to the catalogue.
 *
 * @param sync Catalogue sync repository.
 */
class ObserveSyncedIdsUseCase(
    private val sync: CatalogSyncRepository,
) {
    /** Pushed identifiers. */
    operator fun invoke(): Flow<Set<String>> = sync.observeSyncedIds()
}
