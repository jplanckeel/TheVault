package com.thevault.domain.usecase

import com.thevault.data.remote.GameRemoteDataSource
import com.thevault.domain.repository.GameRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

class SyncVaultWithIgdbUseCase(
    private val gameRepository: GameRepository,
    private val remoteDataSource: GameRemoteDataSource,
) {
    suspend operator fun invoke(): Result<Int> =
        try {
            val games = gameRepository.observeAll().first()
            var updatedCount = 0

            for (game in games) {
                val query = game.title
                if (query.length < 3) continue

                try {
                    val results = remoteDataSource.searchGames(query)
                    val bestMatch =
                        results.firstOrNull {
                            it.title.contains(game.title, ignoreCase = true) || game.title.contains(it.title, ignoreCase = true)
                        } ?: results.firstOrNull()

                    if (bestMatch != null) {
                        val updatedGame =
                            game.copy(
                                coverUrl = game.coverUrl ?: bestMatch.coverUrl,
                                developer = game.developer.takeIf { !it.isNullOrBlank() && it != "N/A" } ?: bestMatch.developer,
                                publisher = game.publisher.takeIf { !it.isNullOrBlank() && it != "N/A" } ?: bestMatch.publisher,
                                genre =
                                    game.genre.takeIf {
                                        it.isNotBlank() && it != "N/A"
                                    } ?: bestMatch.genres.firstOrNull() ?: game.genre,
                                releaseYear = game.releaseYear.takeIf { it != 0 } ?: bestMatch.releaseYear ?: 0,
                                gameModes =
                                    game.gameModes.takeIf { !it.isNullOrBlank() && it != "N/A" }
                                        ?: bestMatch.gameModes.joinToString(", ").takeIf { it.isNotBlank() },
                                themes =
                                    game.themes.takeIf { !it.isNullOrBlank() && it != "N/A" }
                                        ?: bestMatch.themes.joinToString(", ").takeIf { it.isNotBlank() },
                            )

                        if (updatedGame != game) {
                            gameRepository.save(updatedGame)
                            updatedCount++
                        }
                    }
                    delay(350) // Throttle IGDB requests (max 4 per sec)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Continue with next game even if one search fails
                }
            }
            Result.success(updatedCount)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
}
