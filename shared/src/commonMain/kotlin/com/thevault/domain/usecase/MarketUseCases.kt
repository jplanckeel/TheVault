@file:Suppress("TooGenericExceptionCaught") // Network boundary: any failure means "offline, retry later".

package com.thevault.domain.usecase

import com.thevault.domain.model.PricePoint
import com.thevault.domain.model.PriceQuote
import com.thevault.domain.repository.PriceRepository
import com.thevault.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
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
