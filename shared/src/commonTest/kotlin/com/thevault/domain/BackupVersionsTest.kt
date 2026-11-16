package com.thevault.domain

import com.thevault.data.remote.TheVaultBoVaultDataSource
import com.thevault.data.repository.VaultRepositoryImpl
import com.thevault.domain.model.BackupShrinkException
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BackupVersionsTest {
    private class MemSettings : SettingsRepository {
        val values = MutableStateFlow(mapOf("account.token" to "jwt"))

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
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private val engine =
        MockEngine { request ->
            seen += "${request.method.value} ${request.url.encodedPath}?${request.url.encodedQuery}"
            when {
                request.url.encodedPath.endsWith("/backup/versions") ->
                    respond("""[{"id":7,"item_count":467,"created_at":"2026-11-15T10:00:00Z"}]""", HttpStatusCode.OK, json)
                request.method.value == "PUT" && request.url.parameters["force"] != "1" ->
                    respond("""{"current_count":467,"new_count":0}""", HttpStatusCode.Conflict, json)
                request.method.value == "PUT" -> respond("""{"updated_at":"2026-11-16T10:00:00Z"}""", HttpStatusCode.OK, json)
                else -> respond("""{"exportedAt":"2026-11-15T10:00:00Z","items":[]}""", HttpStatusCode.OK, json)
            }
        }

    private val repo =
        VaultRepositoryImpl(
            TheVaultBoVaultDataSource(
                HttpClient(engine) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
                "https://h/api/v1",
            ),
            MemSettings(),
            "https://h",
        )

    private val empty = VaultContent(emptyList(), emptyList(), emptyList(), emptyList())

    @Test
    fun aShrinkingBackupNeedsConfirmation() =
        runTest {
            val refused = repo.upload(empty).exceptionOrNull()
            assertIs<BackupShrinkException>(refused)
            assertEquals(467, refused.currentCount)
            assertEquals("2026-11-16T10:00:00Z", repo.upload(empty, force = true).getOrThrow())
        }

    @Test
    fun versionsAreListedAndRestorable() =
        runTest {
            val versions = repo.backupVersions().getOrThrow()
            assertEquals(7L, versions.single().id)
            assertEquals(467, versions.single().itemCount)
            repo.download(7L).getOrThrow()
            assertTrue(seen.any { it.startsWith("GET /api/v1/backup?version=7") })
        }
}
