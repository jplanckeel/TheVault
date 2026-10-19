package com.thevault.ui.screen.collection

import com.thevault.domain.model.ItemStatus
import com.thevault.domain.usecase.GetAccessoryByIdUseCase
import com.thevault.domain.usecase.GetConsoleByIdUseCase
import com.thevault.domain.usecase.GetGameByIdUseCase
import com.thevault.domain.usecase.ObserveAccessoriesUseCase
import com.thevault.domain.usecase.ObserveConsolesUseCase
import com.thevault.domain.usecase.ObserveGamesUseCase
import com.thevault.domain.usecase.SaveAccessoryUseCase
import com.thevault.domain.usecase.SaveConsoleUseCase
import com.thevault.domain.usecase.SaveGameUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Every game, console and accessory as a single list of [CollectionEntry].
 */
class VaultEntriesSource(
    private val observeGames: ObserveGamesUseCase,
    private val observeConsoles: ObserveConsolesUseCase,
    private val observeAccessories: ObserveAccessoriesUseCase,
) {
    /** All entries, whatever their status. */
    fun entries(): Flow<List<CollectionEntry>> =
        combine(observeGames(), observeConsoles(), observeAccessories()) { games, consoles, accessories ->
            games.map { it.toEntry() } + consoles.map { it.toEntry() } + accessories.map { it.toEntry() }
        }
}

/**
 * Quick actions on any kind of item (« Je l'ai acheté », « Vendu / prêté »).
 */
class ItemActions(
    private val getGame: GetGameByIdUseCase,
    private val saveGame: SaveGameUseCase,
    private val getConsole: GetConsoleByIdUseCase,
    private val saveConsole: SaveConsoleUseCase,
    private val getAccessory: GetAccessoryByIdUseCase,
    private val saveAccessory: SaveAccessoryUseCase,
) {
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
