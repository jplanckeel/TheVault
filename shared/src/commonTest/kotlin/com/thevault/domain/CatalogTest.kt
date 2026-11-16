package com.thevault.domain

import com.thevault.data.remote.CatalogRemoteDataSource
import com.thevault.data.remote.dto.CatalogEntryDto
import com.thevault.data.repository.CatalogRepositoryImpl
import com.thevault.data.repository.resolveCover
import com.thevault.domain.model.Accessory
import com.thevault.domain.model.CatalogSheet
import com.thevault.domain.model.Console
import com.thevault.domain.model.CopyDetails
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import com.thevault.domain.model.withCatalog
import com.thevault.domain.repository.SettingsRepository
import com.thevault.domain.usecase.RefreshCatalogUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

class CatalogTest {
    private val game =
        Game(
            "g",
            "Zelda",
            "GameCube",
            "Action",
            0,
            Region.PAL,
            CopyDetails(),
            status = ItemStatus.OWNED,
            purchasePrice = 20.0,
            developer = "Local",
        )

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

    @Test
    fun catalogFactsReplaceOnlyFilledFields() {
        val merged =
            game.withCatalog(
                CatalogSheet("g", coverUrl = "https://c/z.jpg", genres = listOf("Adventure", "Action"), releaseYear = 2003),
            )
        assertEquals("https://c/z.jpg", merged.coverUrl)
        assertEquals("Adventure", merged.genre)
        assertEquals(2003, merged.releaseYear)
        assertEquals("Local", merged.developer)
        assertEquals(20.0, merged.purchasePrice)
        val console = Console("c", "GameCube", "Nintendo", Region.PAL, "Indigo", productionYear = 0, status = ItemStatus.OWNED)
        assertEquals(2001, console.withCatalog(CatalogSheet("c", releaseYear = 2001)).productionYear)
        val accessory = Accessory("a", "Manette", "Controller", "GameCube", status = ItemStatus.OWNED)
        assertEquals("https://c/a.jpg", accessory.withCatalog(CatalogSheet("a", coverUrl = "https://c/a.jpg")).imageUrl)
    }

    @Test
    fun coversAreMadeAbsolute() {
        assertEquals("https://images.igdb.com/c.jpg", resolveCover("//images.igdb.com/c.jpg", "https://vault"))
        assertEquals("https://vault/covers/x.webp", resolveCover("/covers/x.webp", "https://vault/"))
        assertEquals("http://x/y", resolveCover("http://x/y", ""))
        assertNull(resolveCover("  ", "https://vault"))
        assertNull(resolveCover("/covers/x.webp", ""))
    }

    @Test
    fun refreshAppliesSheetsAndIsThrottled() =
        runTest {
            var calls = 0
            val remote =
                object : CatalogRemoteDataSource {
                    override suspend fun lookup(externalIds: List<String>): List<CatalogEntryDto> {
                        calls++
                        return listOf(
                            CatalogEntryDto(
                                "g",
                                coverUrl = "//img/z.jpg",
                                genres = "Adventure, Action",
                                developer = " ",
                                releaseYear = 2003,
                            ),
                        )
                    }
                }
            val games = FakeGames().apply { items.value = mapOf("g" to game) }
            val sync = FakeSync()
            val clock =
                object : Clock {
                    override fun now() = Instant.parse("2026-11-15T10:00:00Z")
                }
            val refresh =
                RefreshCatalogUseCase(
                    games,
                    FakeConsoles(),
                    FakeAccessories(),
                    CatalogRepositoryImpl(remote, "https://vault"),
                    MemSettings(),
                    clock,
                )
            assertEquals(1, refresh())
            assertEquals(
                "https://img/z.jpg",
                games.items.value
                    .getValue("g")
                    .coverUrl,
            )
            assertEquals(
                "Local",
                games.items.value
                    .getValue("g")
                    .developer,
            )
            assertNull(refresh())
            assertEquals(0, refresh(force = true))
            assertEquals(2, calls)
            assertEquals(emptyList(), sync.dirty)
        }

    @Test
    fun refreshFailsSoftlyWhenOffline() =
        runTest {
            val remote =
                object : CatalogRemoteDataSource {
                    override suspend fun lookup(externalIds: List<String>): List<CatalogEntryDto> = error("offline")
                }
            val games = FakeGames().apply { items.value = mapOf("g" to game) }
            assertNull(
                RefreshCatalogUseCase(
                    games,
                    FakeConsoles(),
                    FakeAccessories(),
                    CatalogRepositoryImpl(remote, ""),
                    MemSettings(),
                )(force = true),
            )
        }
}
