package com.thevault.data.remote

import com.thevault.data.remote.dto.RawgSearchResponse
import com.thevault.domain.model.RemoteGame
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class RawgRemoteDataSource(
    private val httpClient: HttpClient,
    // API Key configurée pour RAWG
    private val apiKey: String = "5564a85e6bd34439b7967791e3186afd", 
) : GameRemoteDataSource {

    override suspend fun searchGames(query: String): List<RemoteGame> {
        if (apiKey.isBlank()) {
            return getMockGames(query)
        }

        return try {
            val response = httpClient.get("https://api.rawg.io/api/games") {
                parameter("key", apiKey)
                parameter("search", query)
                parameter("page_size", 10)
            }.body<RawgSearchResponse>()

            response.results.map { dto ->
                val releaseYear = dto.released?.take(4)?.toIntOrNull()

                RemoteGame(
                    id = dto.id.toString(),
                    title = dto.name,
                    releaseYear = releaseYear,
                    platformNames = dto.platforms?.mapNotNull { it.platform?.name } ?: emptyList(),
                    genres = dto.genres?.map { it.name } ?: emptyList(),
                    coverUrl = dto.background_image, // RAWG already provides absolute URLs
                    summary = null // Summary is not provided in search endpoint by default
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getMockGames(query: String): List<RemoteGame> {
        val mockDB = listOf(
            RemoteGame(
                id = "1",
                title = "The Legend of Zelda: Ocarina of Time",
                releaseYear = 1998,
                genres = listOf("Adventure", "RPG"),
                platformNames = listOf("Nintendo 64"),
                coverUrl = "https://media.rawg.io/media/games/cc1/cc196a5076e441cbbe3d4243ab86ee82.jpg"
            ),
            RemoteGame(
                id = "2",
                title = "The Legend of Zelda: Breath of the Wild",
                releaseYear = 2017,
                genres = listOf("Adventure", "Action"),
                platformNames = listOf("Nintendo Switch"),
                coverUrl = "https://media.rawg.io/media/games/cc1/cc196a5076e441cbbe3d4243ab86ee82.jpg"
            ),
            RemoteGame(
                id = "3",
                title = "Super Mario 64",
                releaseYear = 1996,
                genres = listOf("Platformer"),
                platformNames = listOf("Nintendo 64"),
                coverUrl = "https://media.rawg.io/media/games/4e4/4e45c7b3db2da530263f4581bbcb5a3d.jpg"
            ),
            RemoteGame(
                id = "4",
                title = "Final Fantasy VII",
                releaseYear = 1997,
                genres = listOf("RPG"),
                platformNames = listOf("PlayStation"),
                coverUrl = "https://media.rawg.io/media/games/851/851dfbfca15d319fc9b19e9ef973be36.jpg"
            ),
            RemoteGame(
                id = "5",
                title = "Elden Ring",
                releaseYear = 2022,
                genres = listOf("RPG", "Action"),
                platformNames = listOf("PlayStation 5", "PC", "Xbox Series X"),
                coverUrl = "https://media.rawg.io/media/games/5ec/5ecac5cb026ec26a56efcc546364e348.jpg"
            )
        )

        val q = query.lowercase()
        return mockDB.filter { it.title.lowercase().contains(q) }
    }
}
