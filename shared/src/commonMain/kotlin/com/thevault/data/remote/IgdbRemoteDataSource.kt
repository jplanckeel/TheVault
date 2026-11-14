package com.thevault.data.remote

import com.thevault.data.remote.dto.IgdbGameDto
import com.thevault.data.remote.dto.TwitchTokenResponse
import com.thevault.domain.model.RemoteGame
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class IgdbRemoteDataSource(
    private val httpClient: HttpClient,
    private val fallbackDataSource: GameRemoteDataSource,
    private val clientId: String,
    private val clientSecret: String,
) : GameRemoteDataSource {
    private var cachedToken: String? = null
    private val tokenMutex = Mutex()

    override suspend fun searchGames(query: String): List<RemoteGame> {
        try {
            val token = getValidToken() ?: return fallbackDataSource.searchGames(query)

            val igdbQuery =
                """
                fields name, first_release_date, genres.name, platforms.name, cover.url, summary, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, game_modes.name, themes.name;
                search "$query";
                where version_parent = null;
                limit 15;
                """.trimIndent()

            val response: List<IgdbGameDto> =
                httpClient
                    .post("https://api.igdb.com/v4/games") {
                        header("Client-ID", clientId)
                        header("Authorization", "Bearer $token")
                        setBody(igdbQuery)
                    }.body()

            val mappedResults =
                response.map { dto ->
                    val releaseYear =
                        dto.first_release_date?.let { timestamp ->
                            Instant.fromEpochSeconds(timestamp).toLocalDateTime(TimeZone.UTC).year
                        }

                    RemoteGame(
                        id = dto.id.toString(),
                        title = dto.name,
                        releaseYear = releaseYear,
                        platformNames = dto.platforms?.map { it.name } ?: emptyList(),
                        genres = dto.genres?.map { it.name } ?: emptyList(),
                        coverUrl =
                            dto.cover?.url?.let { originalUrl ->
                                if (originalUrl.startsWith("//")) {
                                    "https:$originalUrl".replace("t_thumb", "t_cover_big")
                                } else {
                                    originalUrl
                                }
                            },
                        summary = dto.summary,
                        developer =
                            dto.involved_companies
                                ?.firstOrNull { it.developer }
                                ?.company
                                ?.name,
                        publisher =
                            dto.involved_companies
                                ?.firstOrNull { it.publisher }
                                ?.company
                                ?.name,
                        gameModes = dto.game_modes?.map { it.name } ?: emptyList(),
                        themes = dto.themes?.map { it.name } ?: emptyList(),
                    )
                }

            if (mappedResults.isEmpty()) {
                return fallbackDataSource.searchGames(query)
            }

            return mappedResults
        } catch (e: Exception) {
            // Log fallback or network error here
            return fallbackDataSource.searchGames(query)
        }
    }

    private suspend fun getValidToken(): String? {
        if (cachedToken != null) return cachedToken

        tokenMutex.withLock {
            if (cachedToken != null) return cachedToken

            try {
                // Fetch new access token from Twitch
                val tokenResponse: TwitchTokenResponse =
                    httpClient
                        .post(
                            "https://id.twitch.tv/oauth2/token?client_id=$clientId&client_secret=$clientSecret&grant_type=client_credentials",
                        ).body()

                cachedToken = tokenResponse.access_token
                return cachedToken
            } catch (e: Exception) {
                return null
            }
        }
    }
}
