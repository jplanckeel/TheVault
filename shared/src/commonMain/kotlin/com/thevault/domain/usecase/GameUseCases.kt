package com.thevault.domain.usecase

import com.thevault.domain.model.Game
import com.thevault.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to observe all games in the user collection.
 *
 * @property repository The [GameRepository] to fetch data from.
 */
class ObserveGamesUseCase(
    private val repository: GameRepository,
) {
    /**
     * Executes the use case.
     *
     * @return A [Flow] emitting the current list of [Game] on every change.
     */
    operator fun invoke(): Flow<List<Game>> = repository.observeAll()
}

/**
 * Use case to retrieve a single game by its ID.
 *
 * @property repository The [GameRepository] to fetch data from.
 */
class GetGameByIdUseCase(
    private val repository: GameRepository,
) {
    /**
     * Executes the use case.
     *
     * @param id The unique identifier of the game.
     * @return The [Game] if found, or null.
     */
    suspend operator fun invoke(id: String): Game? = repository.getById(id)
}

/**
 * Use case to save (insert or update) a game in the collection.
 *
 * @property repository The [GameRepository] to persist data to.
 */
class SaveGameUseCase(
    private val repository: GameRepository,
) {
    /**
     * Executes the use case.
     *
     * @param game The [Game] to save.
     */
    suspend operator fun invoke(game: Game): Unit = repository.save(game)
}

/**
 * Use case to permanently delete a game from the collection.
 *
 * @property repository The [GameRepository] to delete data from.
 */
class DeleteGameUseCase(
    private val repository: GameRepository,
) {
    /**
     * Executes the use case.
     *
     * @param id The unique identifier of the game to delete.
     */
    suspend operator fun invoke(id: String): Unit = repository.delete(id)
}

/**
 * Use case to search games by title or platform.
 *
 * @property repository The [GameRepository] to search data from.
 */
class SearchGamesUseCase(
    private val repository: GameRepository,
) {
    /**
     * Executes the use case.
     *
     * @param query The search query string (case-insensitive).
     * @return A [Flow] emitting matching [Game]s.
     */
    operator fun invoke(query: String): Flow<List<Game>> = repository.search(query)
}
