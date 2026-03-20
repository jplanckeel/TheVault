package com.thevault.data.remote

import com.thevault.domain.model.RemoteGame

interface GameRemoteDataSource {
    suspend fun searchGames(query: String): List<RemoteGame>
}
