package com.thevault.domain.usecase

import com.thevault.domain.repository.AccessoryRepository
import com.thevault.domain.repository.ConsoleRepository
import com.thevault.domain.repository.GameRepository
import kotlinx.coroutines.flow.first

/**
 * Result of a manual refresh of the collection.
 *
 * @property sheets Items whose catalogue sheet (cover, genre, year, studio) changed.
 * @property quotes eBay quotes received.
 */
data class CollectionRefresh(
    val sheets: Int,
    val quotes: Int,
)

/**
 * Manual refresh of the collection from the backend (pull to refresh): pushes the pending
 * items, then downloads the catalogue sheets and the eBay quotes of every item, ignoring
 * the minimum interval of the automatic refreshes.
 *
 * @param games Game repository.
 * @param consoles Console repository.
 * @param accessories Accessory repository.
 * @param refreshCatalog Downloads the catalogue sheets.
 * @param refreshQuotes Downloads the eBay quotes.
 * @param scheduler Pushes the items not yet known by the backend.
 */
@Suppress("LongParameterList")
class RefreshCollectionUseCase(
    private val games: GameRepository,
    private val consoles: ConsoleRepository,
    private val accessories: AccessoryRepository,
    private val refreshCatalog: RefreshCatalogUseCase,
    private val refreshQuotes: RefreshQuotesUseCase,
    private val scheduler: SyncScheduler,
) {
    /**
     * Refreshes the collection.
     *
     * @return What changed, or null when the server is unreachable.
     */
    suspend operator fun invoke(): CollectionRefresh? {
        scheduler.request()
        val ids =
            games.observeAll().first().map { it.id } +
                consoles.observeAll().first().map { it.id } +
                accessories.observeAll().first().map { it.id }
        val sheets = refreshCatalog(force = true) ?: return null
        val quotes = if (ids.isEmpty()) 0 else refreshQuotes(ids, force = true) ?: return null
        return CollectionRefresh(sheets, quotes)
    }
}
