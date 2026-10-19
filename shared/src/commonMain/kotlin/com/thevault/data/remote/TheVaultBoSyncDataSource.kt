package com.thevault.data.remote

import com.thevault.domain.model.Game
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface ItemSyncRemoteDataSource {
    suspend fun syncItem(game: Game)
}

class TheVaultBoSyncDataSource(
    private val httpClient: HttpClient,
    private val baseUrl: String = "https://thevault.bananaops.cloud/api/v1",
    private val apiKey: String,
) : ItemSyncRemoteDataSource {

    override suspend fun syncItem(game: Game) {
        httpClient.post("$baseUrl/items") {
            header("X-API-Key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(game.toSyncDto())
        }
    }
}

@Serializable
private data class SyncItemDto(
    @SerialName("external_id") val externalId: String,
    val type: String = "game",
    val title: String,
    val platform: String,
    val region: String,
    val condition: String,
    val status: String,
    @SerialName("cover_url") val coverUrl: String = "",
    val summary: String = "",
    val genres: String = "",
    val developer: String = "",
    val publisher: String = "",
    @SerialName("release_year") val releaseYear: Int = 0,
)

private fun Game.toSyncDto() = SyncItemDto(
    externalId = id,
    title = title,
    platform = platform,
    region = region.name,
    condition = condition.name,
    status = status.name,
    coverUrl = coverUrl ?: "",
    genres = genre,
    developer = developer ?: "",
    publisher = publisher ?: "",
    releaseYear = releaseYear,
)
