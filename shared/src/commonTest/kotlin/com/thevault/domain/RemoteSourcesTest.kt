package com.thevault.domain

import com.thevault.data.remote.TheVaultBoPriceDataSource
import com.thevault.data.remote.TheVaultBoRemoteDataSource
import com.thevault.data.remote.TheVaultBoSyncDataSource
import com.thevault.data.remote.toCatalogUrl
import com.thevault.domain.model.SyncOutcome
import com.thevault.domain.model.SyncPayload
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RemoteSourcesTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun client(handler: MockRequestHandler) =
        HttpClient(MockEngine(handler)) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

    private val payload = SyncPayload("g", "game", "Zelda", "GameCube", "PAL", "CIB", "OWNED", coverUrl = "//images.igdb.com/c.jpg")

    @Test
    fun syncOutcomesFollowTheStatus() =
        runTest {
            suspend fun outcome(status: HttpStatusCode) =
                TheVaultBoSyncDataSource(client { respondError(status) }, "https://h/api/v1", "k").push(payload)
            assertEquals(SyncOutcome.Rejected, outcome(HttpStatusCode.Gone))
            assertEquals(SyncOutcome.Rejected, outcome(HttpStatusCode.BadRequest))
            assertEquals(SyncOutcome.Failed, outcome(HttpStatusCode.TooManyRequests))
            assertEquals(SyncOutcome.Failed, outcome(HttpStatusCode.InternalServerError))
            val ok =
                TheVaultBoSyncDataSource(
                    client { req ->
                        assertEquals("k", req.headers["X-API-Key"])
                        respond("{}", HttpStatusCode.Created, json)
                    },
                    "https://h/api/v1",
                    "k",
                )
            assertEquals(SyncOutcome.Synced, ok.push(payload))
            val down = TheVaultBoSyncDataSource(client { error("offline") }, "https://h/api/v1", "k")
            assertEquals(SyncOutcome.Failed, down.push(payload))
        }

    @Test
    fun coverUrlsAreMadeAbsolute() {
        assertEquals("https://images.igdb.com/c.jpg", "//images.igdb.com/c.jpg".toCatalogUrl())
        assertEquals("http://x/y.png", "http://x/y.png".toCatalogUrl())
        assertEquals("", "/covers/a.png".toCatalogUrl())
    }

    @Test
    fun quotesAreRequestedInChunksAndHistoryIsRead() =
        runTest {
            var calls = 0
            val source =
                TheVaultBoPriceDataSource(
                    client { req ->
                        calls++
                        if (req.url.encodedPath.endsWith("/history")) {
                            assertEquals("g1", req.url.parameters["external_id"])
                            respond(
                                """[{"condition":"cib","price_median":20,"scraped_at":"2026-11-01T00:00:00Z"}]""",
                                HttpStatusCode.OK,
                                json,
                            )
                        } else {
                            respond(
                                """[{"external_id":"g1","condition":"cib","price_median":20,"scraped_at":"2026-11-01T00:00:00Z"}]""",
                                HttpStatusCode.OK,
                                json,
                            )
                        }
                    },
                    "https://h/api/v1",
                    "k",
                )
            assertEquals(3, source.quotes(List(2500) { "g$it" }).size)
            assertEquals(3, calls)
            assertEquals(20.0, source.history("g1").single().priceMedian)
        }

    @Test
    fun searchMapsTheBackendResults() =
        runTest {
            val source =
                TheVaultBoRemoteDataSource(
                    client {
                        respond(
                            """[{"id":"1","title":"Zelda","release_year":0,"platforms":["GameCube"],"genres":["Action"],"cover_url":"c"}]""",
                            HttpStatusCode.OK,
                            json,
                        )
                    },
                    "https://h/api/v1",
                    "k",
                )
            val result = source.searchGames("zelda").single()
            assertEquals("Zelda", result.title)
            assertEquals(null, result.releaseYear)
            assertTrue("GameCube" in result.platformNames)
        }
}
