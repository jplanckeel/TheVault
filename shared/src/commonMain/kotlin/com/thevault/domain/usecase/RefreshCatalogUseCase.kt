package com.thevault.domain.usecase

import com.thevault.domain.model.withCatalog
import com.thevault.domain.repository.AccessoryRepository
import com.thevault.domain.repository.CatalogRepository
import com.thevault.domain.repository.ConsoleRepository
import com.thevault.domain.repository.GameRepository
import com.thevault.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Downloads the backend catalogue sheets of every item (cover, genre, year, studio) and
 * applies them locally. Items are saved without being marked for sync: the values
 * already come from the server.
 */
class RefreshCatalogUseCase(
    private val games: GameRepository,
    private val consoles: ConsoleRepository,
    private val accessories: AccessoryRepository,
    private val catalog: CatalogRepository,
    private val settings: SettingsRepository,
    private val clock: Clock = Clock.System,
) {
    /**
     * Refreshes the catalogue facts of the collection.
     *
     * @param force Refreshes even when the last refresh is recent.
     * @return Number of items updated, null when skipped or when the server is unreachable.
     */
    suspend operator fun invoke(force: Boolean = false): Int? {
        val now = clock.now()
        val last = settings.get(KEY_LAST_REFRESH)?.let { runCatching { Instant.parse(it) }.getOrNull() }
        if (!force && last != null && now - last < REFRESH_INTERVAL) return null
        return try {
            apply().also { settings.set(KEY_LAST_REFRESH, now.toString()) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun apply(): Int {
        val allGames = games.observeAll().first()
        val allConsoles = consoles.observeAll().first()
        val allAccessories = accessories.observeAll().first()
        val sheets = catalog.lookup(allGames.map { it.id } + allConsoles.map { it.id } + allAccessories.map { it.id })
        var updated = 0
        for (game in allGames) {
            val next = sheets[game.id]?.let(game::withCatalog) ?: continue
            if (next != game) games.save(next).also { updated++ }
        }
        for (console in allConsoles) {
            val next = sheets[console.id]?.let(console::withCatalog) ?: continue
            if (next != console) consoles.save(next).also { updated++ }
        }
        for (accessory in allAccessories) {
            val next = sheets[accessory.id]?.let(accessory::withCatalog) ?: continue
            if (next != accessory) accessories.save(next).also { updated++ }
        }
        return updated
    }

    /** Companion constants. */
    companion object {
        /** Setting holding the time of the last refresh. */
        const val KEY_LAST_REFRESH = "catalog.last_refresh"

        /** Minimum time between two automatic refreshes. */
        val REFRESH_INTERVAL = 12.hours
    }
}
