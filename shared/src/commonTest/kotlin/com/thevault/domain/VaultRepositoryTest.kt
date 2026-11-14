package com.thevault.domain

import com.thevault.data.remote.TheVaultBoVaultDataSource
import com.thevault.data.repository.VaultRepositoryImpl
import com.thevault.domain.model.ShareScope
import com.thevault.domain.model.VaultContent
import com.thevault.domain.repository.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VaultRepositoryTest {
    private class MemSettings : SettingsRepository {
        val values = MutableStateFlow<Map<String, String>>(emptyMap())

        override fun observe(key: String) = values.map { it[key] }

        override suspend fun get(key: String) = values.value[key]

        override suspend fun set(
            key: String,
            value: String,
        ) {
            values.value = values.value + (key to value)
        }
    }

    private val seen = mutableListOf<String>()

    private val engine =
        MockEngine { request ->
            seen += "${request.method.value} ${request.url.encodedPath} ${request.headers[HttpHeaders.Authorization].orEmpty()}"
            val json = headersOf(HttpHeaders.ContentType, "application/json")
            when (request.url.encodedPath) {
                "/api/v1/auth/login" -> respond("""{"token":"jwt","user":{"email":"a@b.c","name":"Jé"}}""", HttpStatusCode.OK, json)
                "/api/v1/backup" -> respond("""{"updated_at":"2026-11-14T10:00:00Z"}""", HttpStatusCode.OK, json)
                "/api/v1/shares" ->
                    respond(
                        """{"token":"tok","scope":"wishlist","title":"Envies","hide_prices":true}""",
                        HttpStatusCode.Created,
                        json,
                    )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }

    private fun repo(settings: MemSettings) =
        VaultRepositoryImpl(
            TheVaultBoVaultDataSource(
                HttpClient(engine) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
                "https://host/api/v1",
            ),
            settings,
            "https://host",
        )

    @Test
    fun loginKeepsTheSessionAndCallsAreAuthenticated() =
        runTest {
            val settings = MemSettings()
            val repo = repo(settings)
            assertTrue(repo.upload(VaultContent(emptyList(), emptyList(), emptyList(), emptyList())).isFailure)
            assertEquals("Jé", repo.login("a@b.c", "pw").getOrThrow().name)
            assertEquals("a@b.c", repo.observeAccount().first()?.email)
            assertEquals("2026-11-14T10:00:00Z", repo.upload(VaultContent(emptyList(), emptyList(), emptyList(), emptyList())).getOrThrow())
            val link = repo.createShare(ShareScope.WISHLIST, "Envies", true).getOrThrow()
            assertEquals("https://host/share/tok", link.url)
            assertTrue(seen.contains("PUT /api/v1/backup Bearer jwt"))
            repo.logout()
            assertNull(repo.observeAccount().first())
        }
}
