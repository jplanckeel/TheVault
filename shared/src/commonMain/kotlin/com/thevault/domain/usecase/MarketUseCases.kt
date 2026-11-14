@file:Suppress("TooGenericExceptionCaught") // Network boundary: any failure means "offline, retry later".

package com.thevault.domain.usecase

import com.thevault.domain.model.PricePoint
import com.thevault.domain.model.PriceQuote
import com.thevault.domain.model.PriceSnapshot
import com.thevault.domain.model.PriceSource
import com.thevault.domain.repository.PriceRepository
import com.thevault.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Observes the cached eBay quotes.
 *
 * @param repository Price repository.
 */
class ObserveQuotesUseCase(
    private val repository: PriceRepository,
) {
    /** Quotes keyed by item identifier. */
    operator fun invoke(): Flow<Map<String, List<PriceQuote>>> = repository.observeQuotes()
}

/**
 * Downloads the quotes of the collection, at most every few hours unless forced.
 *
 * @param repository Price repository.
 * @param settings Stores the time of the last refresh.
 * @param clock Time source.
 */
class RefreshQuotesUseCase(
    private val repository: PriceRepository,
    private val settings: SettingsRepository,
    private val clock: Clock = Clock.System,
) {
    /**
     * Refreshes the quotes of [itemIds].
     *
     * @param itemIds Items to quote.
     * @param force Refreshes even when the cache is recent.
     * @return Number of quotes received, null when skipped or failed.
     */
    suspend operator fun invoke(
        itemIds: List<String>,
        force: Boolean = false,
    ): Int? {
        if (itemIds.isEmpty()) return null
        val last = settings.get(KEY_LAST_REFRESH)?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val now = clock.now()
        if (!force && last != null && now - last < REFRESH_INTERVAL) return null
        return try {
            repository.refreshQuotes(itemIds).also { settings.set(KEY_LAST_REFRESH, now.toString()) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /** Companion constants. */
    companion object {
        /** Setting holding the time of the last successful refresh. */
        const val KEY_LAST_REFRESH = "quotes.last_refresh"

        private val REFRESH_INTERVAL = 6.hours
    }
}

/**
 * Price history of one item.
 *
 * @param repository Price repository.
 */
class GetPriceHistoryUseCase(
    private val repository: PriceRepository,
) {
    /** History of [itemId], oldest first; empty when offline. */
    suspend operator fun invoke(itemId: String): List<PricePoint> =
        try {
            repository.history(itemId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
}

/**
 * Observes every price snapshot (PAL history and imported US prices).
 *
 * @param repository Price repository.
 */
class ObservePriceSnapshotsUseCase(
    private val repository: PriceRepository,
) {
    /** Snapshots keyed by item, oldest first. */
    operator fun invoke(): Flow<Map<String, List<PriceSnapshot>>> = repository.observeSnapshots()
}

/**
 * Stores the US prices of a Gameye import as `gameye_import_us` snapshots.
 *
 * @param repository Price repository.
 * @param clock Time source.
 */
class RecordImportedPricesUseCase(
    private val repository: PriceRepository,
    private val clock: Clock = Clock.System,
) {
    /** Replaces the imported prices with [prices], dated today. */
    suspend operator fun invoke(prices: Map<String, UsPrices>) {
        val today = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        repository.replaceImportedPrices(
            prices.map { (id, p) -> PriceSnapshot(id, today, PriceSource.GAMEYE_US, p.loose, p.cib, p.new) },
        )
    }
}

/**
 * Latest US prices of the last import, keyed by item (for the CSV export).
 *
 * @param repository Price repository.
 */
class GetImportedPricesUseCase(
    private val repository: PriceRepository,
) {
    /** Imported US prices by item. */
    suspend operator fun invoke(): Map<String, UsPrices> =
        repository
            .observeSnapshots()
            .first()
            .mapNotNull { (id, list) ->
                list.lastOrNull { it.source == PriceSource.GAMEYE_US }?.let { id to UsPrices(it.loose, it.cib, it.new) }
            }.toMap()
}
