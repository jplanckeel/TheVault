package com.thevault.domain.repository

import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing [Game] entities.
 * All implementations must reside in the data layer.
 */
interface GameRepository {
    /**
     * Observes all games in the collection as a live stream.
     *
     * @return A [Flow] emitting the current list of games on every change.
     */
    fun observeAll(): Flow<List<Game>>

    /**
     * Retrieves a single game by its identifier.
     *
     * @param id The unique identifier of the game.
     * @return The [Game] if found, or null otherwise.
     */
    suspend fun getById(id: String): Game?

    /**
     * Observes games filtered by their status.
     *
     * @param status The [ItemStatus] to filter by.
     * @return A [Flow] emitting the filtered list.
     */
    fun observeByStatus(status: ItemStatus): Flow<List<Game>>

    /**
     * Inserts or replaces a game in the collection.
     *
     * @param game The [Game] to save.
     */
    suspend fun save(game: Game)

    /**
     * Permanently removes a game from the collection.
     *
     * @param id The unique identifier of the game to delete.
     */
    suspend fun delete(id: String)

    /**
     * Searches games by title or platform (case-insensitive).
     *
     * @param query The search query string.
     * @return A [Flow] emitting matching games.
     */
    fun search(query: String): Flow<List<Game>>
}
