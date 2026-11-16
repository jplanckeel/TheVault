package com.thevault.data.remote

import com.thevault.data.remote.dto.CatalogEntryDto
import com.thevault.data.remote.dto.CatalogLookupRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** Sheets of the shared catalogue kept by the backend (covers, IGDB facts, admin fixes). */
interface CatalogRemoteDataSource {
    /** Catalogue sheets of the items known by [externalIds]; unknown ids are left out. */
    suspend fun lookup(externalIds: List<String>): List<CatalogEntryDto>
}

/**
 * [CatalogRemoteDataSource] backed by the TheVault back-office API.
 *
 * @param httpClient HTTP client.
 * @param baseUrl API base URL.
 * @param apiKey Mobile API key.
 */
class TheVaultBoCatalogDataSource(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val apiKey: String,
) : CatalogRemoteDataSource {
    override suspend fun lookup(externalIds: List<String>): List<CatalogEntryDto> =
        externalIds.chunked(MAX_IDS_PER_REQUEST).flatMap { chunk ->
            httpClient
                .post("$baseUrl/items/lookup") {
                    expectSuccess = true
                    header("X-API-Key", apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(CatalogLookupRequestDto(chunk))
                }.body<List<CatalogEntryDto>>()
        }

    private companion object {
        const val MAX_IDS_PER_REQUEST = 1000
    }
}
