package com.thevault.domain.usecase

import com.thevault.data.remote.GameRemoteDataSource
import com.thevault.domain.model.RemoteGame
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class SearchRemoteGamesUseCase(
    private val remoteDataSource: GameRemoteDataSource,
) {
    suspend operator fun invoke(query: String): List<RemoteGame> {
        if (query.length < 3) return emptyList()
        return remoteDataSource.searchGames(query)
    }
}
