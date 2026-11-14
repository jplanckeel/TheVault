@file:Suppress("TooGenericExceptionCaught") // Network boundary: any failure means "offline, retry later".

package com.thevault.domain.usecase

import com.thevault.data.remote.GameRemoteDataSource
import com.thevault.domain.model.RemoteGame
import kotlinx.coroutines.CancellationException

/**
 * Searches games on IGDB through the backend.
 *
 * @param remoteDataSource Search API.
 */
class SearchRemoteGamesUseCase(
    private val remoteDataSource: GameRemoteDataSource,
) {
    /**
     * Searches [query] (3 characters at least).
     *
     * @return The results, or a failure when the network or the backend is unavailable.
     */
    suspend operator fun invoke(query: String): Result<List<RemoteGame>> {
        if (query.length < MIN_QUERY) return Result.success(emptyList())
        return try {
            Result.success(remoteDataSource.searchGames(query))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private companion object {
        const val MIN_QUERY = 3
    }
}
