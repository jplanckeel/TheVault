package com.thevault.data.remote

import com.thevault.data.remote.dto.BoSearchResultDto
import com.thevault.domain.model.RemoteGame
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter

class TheVaultBoRemoteDataSource(
    private val httpClient: HttpClient,
    private val baseUrl: String = "https://thevault.bananaops.cloud/api/v1",
    private val apiKey: String,
) : GameRemoteDataSource {

    override suspend fun searchGames(query: String): List<RemoteGame> {
        val results: List<BoSearchResultDto> = httpClient.get("$baseUrl/search") {
            header("X-API-Key", apiKey)
            parameter("q", query)
        }.body()

        return results.map { it.toRemoteGame() }
    }
}

private fun BoSearchResultDto.toRemoteGame() = RemoteGame(
    id = id,
    title = title,
    releaseYear = releaseYear.takeIf { it != 0 },
    platformNames = platforms,
    genres = genres,
    coverUrl = coverUrl,
    summary = summary,
    developer = developer,
    publisher = publisher,
)
