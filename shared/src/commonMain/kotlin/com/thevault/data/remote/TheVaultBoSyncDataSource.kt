@file:Suppress("TooGenericExceptionCaught") // Network boundary: any failure means "offline, retry later".

package com.thevault.data.remote

import com.thevault.data.remote.dto.SyncItemDto
import com.thevault.domain.model.SyncOutcome
import com.thevault.domain.model.SyncPayload
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException

/** Pushes items to the shared catalogue. */
interface ItemSyncRemoteDataSource {
    /** Pushes [payload] and tells whether to retry later. */
    suspend fun push(payload: SyncPayload): SyncOutcome
}

/**
 * [ItemSyncRemoteDataSource] backed by POST /items.
 *
 * @param httpClient HTTP client (expectSuccess, timeouts).
 * @param baseUrl API base URL.
 * @param apiKey Mobile API key.
 */
class TheVaultBoSyncDataSource(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val apiKey: String,
) : ItemSyncRemoteDataSource {
    override suspend fun push(payload: SyncPayload): SyncOutcome =
        try {
            httpClient.post("$baseUrl/items") {
                expectSuccess = true
                header("X-API-Key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(payload.toDto())
            }
            SyncOutcome.Synced
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            // 400 (invalid item) and 410 (deleted by an admin) will never succeed.
            val permanent = e.response.status == HttpStatusCode.Gone || e.response.status == HttpStatusCode.BadRequest
            if (permanent) SyncOutcome.Rejected else SyncOutcome.Failed
        } catch (_: Exception) {
            SyncOutcome.Failed
        }
}

private fun SyncPayload.toDto() =
    SyncItemDto(
        externalId = externalId,
        type = type,
        title = title,
        platform = platform,
        region = region,
        condition = condition,
        status = status,
        coverUrl = coverUrl.toCatalogUrl(),
        genres = genres,
        developer = developer,
        publisher = publisher,
        releaseYear = releaseYear,
    )

/** The backend only accepts absolute http(s) cover URLs; IGDB returns protocol-relative ones. */
internal fun String.toCatalogUrl(): String =
    when {
        startsWith("//") -> "https:$this"
        startsWith("https://") || startsWith("http://") -> this
        else -> ""
    }
