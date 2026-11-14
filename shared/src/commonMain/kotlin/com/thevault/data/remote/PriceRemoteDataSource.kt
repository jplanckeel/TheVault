package com.thevault.data.remote

import com.thevault.data.remote.dto.PriceSnapshotDto
import com.thevault.data.remote.dto.QuoteDto
import com.thevault.data.remote.dto.QuotesRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** eBay quotes collected by the backend. */
interface PriceRemoteDataSource {
    /** Latest quotes of [externalIds], with the median of 30 days ago. */
    suspend fun quotes(externalIds: List<String>): List<QuoteDto>

    /** Snapshots of one item over the last year, newest first. */
    suspend fun history(externalId: String): List<PriceSnapshotDto>
}

/**
 * [PriceRemoteDataSource] backed by the TheVault back-office API.
 *
 * @param httpClient HTTP client (expectSuccess, timeouts).
 * @param baseUrl API base URL.
 * @param apiKey Mobile API key.
 */
class TheVaultBoPriceDataSource(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val apiKey: String,
) : PriceRemoteDataSource {
    override suspend fun quotes(externalIds: List<String>): List<QuoteDto> =
        externalIds.chunked(MAX_IDS_PER_REQUEST).flatMap { chunk ->
            httpClient
                .post("$baseUrl/prices/quotes") {
                    expectSuccess = true
                    header("X-API-Key", apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(QuotesRequestDto(chunk))
                }.body<List<QuoteDto>>()
        }

    override suspend fun history(externalId: String): List<PriceSnapshotDto> =
        httpClient
            .get("$baseUrl/prices/history") {
                expectSuccess = true
                header("X-API-Key", apiKey)
                parameter("external_id", externalId)
                parameter("days", HISTORY_DAYS)
            }.body()

    private companion object {
        const val MAX_IDS_PER_REQUEST = 1000
        const val HISTORY_DAYS = 365
    }
}
