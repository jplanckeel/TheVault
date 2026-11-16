package com.thevault.ui.screen.collection

import com.thevault.domain.model.ItemStatus
import com.thevault.domain.usecase.DeleteAccessoryUseCase
import com.thevault.domain.usecase.DeleteConsoleUseCase
import com.thevault.domain.usecase.DeleteGameUseCase
import com.thevault.domain.usecase.GetAccessoryByIdUseCase
import com.thevault.domain.usecase.GetConsoleByIdUseCase
import com.thevault.domain.usecase.GetGameByIdUseCase
import com.thevault.domain.usecase.ObserveAccessoriesUseCase
import com.thevault.domain.usecase.ObserveConsolesUseCase
import com.thevault.domain.usecase.ObserveGamesUseCase
import com.thevault.domain.usecase.ObserveItemPhotosUseCase
import com.thevault.domain.usecase.ObservePriceSnapshotsUseCase
import com.thevault.domain.usecase.ObserveQuotesUseCase
import com.thevault.domain.usecase.SaveAccessoryUseCase
import com.thevault.domain.usecase.SaveConsoleUseCase
import com.thevault.domain.usecase.SaveGameUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Every game, console and accessory as a single list of [CollectionEntry].
 *
 * @param observeGames Games.
 * @param observeConsoles Consoles.
 * @param observeAccessories Accessories.
 * @param observeQuotes Cached eBay quotes.
 * @param observeSnapshots Price history and imported US prices.
 * @param observePhotos Photos of the copies, shown instead of the catalogue cover.
 */
@Suppress("LongParameterList")
class VaultEntriesSource(
    private val observeGames: ObserveGamesUseCase,
    private val observeConsoles: ObserveConsolesUseCase,
    private val observeAccessories: ObserveAccessoriesUseCase,
    private val observeQuotes: ObserveQuotesUseCase,
    private val observeSnapshots: ObservePriceSnapshotsUseCase,
    private val observePhotos: ObserveItemPhotosUseCase,
) {
    /** All entries, whatever their status, valued with the eBay quotes and imported US prices. */
    fun entries(): Flow<List<CollectionEntry>> =
        combine(observeGames(), observeConsoles(), observeAccessories(), observeQuotes(), observeSnapshots()) { g, c, a, quotes, snaps ->
            (g.map { it.toEntry() } + c.map { it.toEntry() } + a.map { it.toEntry() }).withMarket(quotes, snaps)
        }.combine(observePhotos()) { entries, photos ->
            if (photos.isEmpty()) entries else entries.map { e -> photos[e.id]?.let { e.copy(coverUrl = it, hasPhoto = true) } ?: e }
        }
}

/**
 * Quick actions on any kind of item (« Je l'ai acheté », « Vendu / prêté »).
 */
@Suppress("LongParameterList")
class ItemActions(
    private val getGame: GetGameByIdUseCase,
    private val saveGame: SaveGameUseCase,
    private val getConsole: GetConsoleByIdUseCase,
    private val saveConsole: SaveConsoleUseCase,
    private val getAccessory: GetAccessoryByIdUseCase,
    private val saveAccessory: SaveAccessoryUseCase,
    private val deleteGame: DeleteGameUseCase,
    private val deleteConsole: DeleteConsoleUseCase,
    private val deleteAccessory: DeleteAccessoryUseCase,
) {
    /** Permanently deletes an item. */
    suspend fun delete(
        kind: ItemKind,
        id: String,
    ) {
        when (kind) {
            ItemKind.Game -> deleteGame(id)
            ItemKind.Console -> deleteConsole(id)
            ItemKind.Accessory -> deleteAccessory(id)
        }
    }

    /**
     * Applies [change] to the copy details of the items [ids] (bulk edit).
     *
     * @param kind Item category.
     * @param ids Item identifiers.
     * @param change Edit of the details.
     */
    suspend fun editDetails(
        kind: ItemKind,
        ids: Collection<String>,
        change: (com.thevault.domain.model.CopyDetails) -> com.thevault.domain.model.CopyDetails,
    ) {
        ids.forEach { id ->
            when (kind) {
                ItemKind.Game -> getGame(id)?.let { saveGame(it.copy(details = change(it.details))) }
                ItemKind.Console -> getConsole(id)?.let { saveConsole(it.copy(details = change(it.details))) }
                ItemKind.Accessory -> getAccessory(id)?.let { saveAccessory(it.copy(details = change(it.details))) }
            }
        }
    }

    /**
     * Changes the status of an item.
     *
     * @param kind Item category.
     * @param id Item identifier.
     * @param status New status.
     */
    suspend fun setStatus(
        kind: ItemKind,
        id: String,
        status: ItemStatus,
    ) {
        when (kind) {
            ItemKind.Game -> getGame(id)?.let { saveGame(it.copy(status = status)) }
            ItemKind.Console -> getConsole(id)?.let { saveConsole(it.copy(status = status)) }
            ItemKind.Accessory -> getAccessory(id)?.let { saveAccessory(it.copy(status = status)) }
        }
    }
}
