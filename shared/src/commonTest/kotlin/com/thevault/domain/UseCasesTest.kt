package com.thevault.domain

import com.thevault.data.remote.GameRemoteDataSource
import com.thevault.data.sync.BackgroundSyncScheduler
import com.thevault.domain.model.Accessory
import com.thevault.domain.model.Components
import com.thevault.domain.model.Console
import com.thevault.domain.model.CopyDetails
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemSet
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.PriceGrade
import com.thevault.domain.model.PricePoint
import com.thevault.domain.model.PriceSnapshot
import com.thevault.domain.model.PriceSource
import com.thevault.domain.model.Region
import com.thevault.domain.model.RemoteGame
import com.thevault.domain.model.SetEntry
import com.thevault.domain.model.ShareScope
import com.thevault.domain.model.SyncOutcome
import com.thevault.domain.model.toSyncPayload
import com.thevault.domain.repository.PriceRepository
import com.thevault.domain.usecase.BackupVaultUseCase
import com.thevault.domain.usecase.DeleteAccessoryUseCase
import com.thevault.domain.usecase.DeleteConsoleUseCase
import com.thevault.domain.usecase.DeleteGameUseCase
import com.thevault.domain.usecase.DeleteSetUseCase
import com.thevault.domain.usecase.EditSetEntryUseCase
import com.thevault.domain.usecase.EditShareUseCase
import com.thevault.domain.usecase.GetAccessoryByIdUseCase
import com.thevault.domain.usecase.GetConsoleByIdUseCase
import com.thevault.domain.usecase.GetGameByIdUseCase
import com.thevault.domain.usecase.GetPriceHistoryUseCase
import com.thevault.domain.usecase.ObserveAccessoriesUseCase
import com.thevault.domain.usecase.ObserveConsolesUseCase
import com.thevault.domain.usecase.ObserveGamesUseCase
import com.thevault.domain.usecase.ObserveSetsUseCase
import com.thevault.domain.usecase.RestoreVaultUseCase
import com.thevault.domain.usecase.SaveAccessoryUseCase
import com.thevault.domain.usecase.SaveConsoleUseCase
import com.thevault.domain.usecase.SaveGameUseCase
import com.thevault.domain.usecase.SaveSetUseCase
import com.thevault.domain.usecase.SearchGamesUseCase
import com.thevault.domain.usecase.SearchRemoteGamesUseCase
import com.thevault.domain.usecase.ShareLinksUseCase
import com.thevault.domain.usecase.SignInUseCase
import com.thevault.domain.usecase.SyncPendingItemsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UseCasesTest {
    private val game = Game("g", "Zelda", "GameCube", "Action", 2003, Region.PAL, CopyDetails(Components.CIB), status = ItemStatus.OWNED)
    private val console = Console("c", "GameCube", "Nintendo", Region.PAL, "Indigo", productionYear = 2001, status = ItemStatus.OWNED)
    private val accessory = Accessory("a", "Manette", "Controller", "GameCube", status = ItemStatus.WISHLIST)

    @Test
    fun savesMarkTheItemDirtyAndRequestASync() =
        runTest {
            val games = FakeGames()
            val consoles = FakeConsoles()
            val accessories = FakeAccessories()
            val sync = FakeSync()
            val scheduler = FakeScheduler()
            SaveGameUseCase(games, sync, scheduler)(game)
            SaveConsoleUseCase(consoles, sync, scheduler)(console)
            SaveAccessoryUseCase(accessories, sync, scheduler)(accessory)
            assertEquals(listOf("g", "c", "a"), sync.dirty)
            assertEquals(3, scheduler.requests)
            assertEquals(game, GetGameByIdUseCase(games)("g"))
            assertEquals(console, GetConsoleByIdUseCase(consoles)("c"))
            assertEquals(accessory, GetAccessoryByIdUseCase(accessories)("a"))
            assertEquals(listOf(game), ObserveGamesUseCase(games)().first())
            assertEquals(listOf(console), ObserveConsolesUseCase(consoles)().first())
            assertEquals(listOf(accessory), ObserveAccessoriesUseCase(accessories)().first())
            assertEquals(listOf(game), SearchGamesUseCase(games)("zel").first())
            DeleteGameUseCase(games)("g")
            DeleteConsoleUseCase(consoles)("c")
            DeleteAccessoryUseCase(accessories)("a")
            assertNull(games.getById("g"))
            assertNull(consoles.getById("c"))
            assertNull(accessories.getById("a"))
        }

    @Test
    fun pendingItemsArePushedUntilTheNetworkFails() =
        runTest {
            val games = FakeGames().apply { items.value = (1..5).associate { "g$it" to game.copy(id = "g$it") } }
            val consoles = FakeConsoles().apply { items.value = mapOf("c" to console) }
            val accessories = FakeAccessories().apply { items.value = mapOf("a" to accessory) }
            val sync = FakeSync().apply { synced.value = setOf("g1") }
            val report = SyncPendingItemsUseCase(games, consoles, accessories, sync)()
            assertEquals(6, report.synced)
            assertEquals(0, report.failed)
            assertTrue("g1" !in sync.pushed)
            sync.outcome = SyncOutcome.Failed
            sync.synced.value = emptySet()
            val offline = SyncPendingItemsUseCase(games, consoles, accessories, sync)()
            assertEquals(0, offline.synced)
            assertEquals(7, offline.failed)
        }

    @Test
    fun payloadsDescribeEachKind() {
        assertEquals("CIB", game.toSyncPayload().condition)
        assertEquals("console", console.toSyncPayload().type)
        assertEquals("GameCube", console.toSyncPayload().platform)
        assertEquals("OTHER", accessory.toSyncPayload().region)
    }

    @Test
    fun schedulerRunsTheSyncInTheBackground() {
        val dispatcher = StandardTestDispatcher()
        val scope = TestScope(dispatcher)
        val sync = FakeSync()
        val games = FakeGames().apply { items.value = mapOf("g" to game) }
        val scheduler = BackgroundSyncScheduler(scope, SyncPendingItemsUseCase(games, FakeConsoles(), FakeAccessories(), sync))
        scheduler.request()
        scheduler.request()
        scope.advanceUntilIdle()
        assertEquals(
            1,
            scheduler.status.value.lastReport
                ?.synced,
        )
        assertEquals(false, scheduler.status.value.running)
        assertEquals(listOf("g"), sync.pushed)
    }

    @Test
    fun remoteSearchAndHistoryFailSoftly() =
        runTest {
            val ok =
                object : GameRemoteDataSource {
                    override suspend fun searchGames(query: String) =
                        listOf(RemoteGame("1", "Zelda", null, listOf("GameCube"), emptyList(), null, null, null, null))
                }
            val broken =
                object : GameRemoteDataSource {
                    override suspend fun searchGames(query: String): List<RemoteGame> = error("offline")
                }
            assertEquals(emptyList(), SearchRemoteGamesUseCase(ok)("ze").getOrThrow())
            assertEquals("Zelda", SearchRemoteGamesUseCase(ok)("zelda").getOrThrow().single().title)
            assertTrue(SearchRemoteGamesUseCase(broken)("zelda").isFailure)
            val prices =
                object : PriceRepository {
                    override fun observeQuotes() = MutableStateFlow(emptyMap<String, List<com.thevault.domain.model.PriceQuote>>())

                    override suspend fun refreshQuotes(itemIds: List<String>) = 0

                    override suspend fun history(itemId: String): List<PricePoint> = error("offline")

                    override fun observeSnapshots(): Flow<Map<String, List<PriceSnapshot>>> = MutableStateFlow(emptyMap())

                    override suspend fun replaceImportedPrices(snapshots: List<PriceSnapshot>) = Unit
                }
            assertEquals(emptyList(), GetPriceHistoryUseCase(prices)("g"))
        }

    @Test
    fun setsAndBackupsGoThroughTheirRepositories() =
        runTest {
            val sets = FakeSets()
            SaveSetUseCase(sets)(ItemSet("s", "Zelda"))
            EditSetEntryUseCase(sets)("s", SetEntry("Wind Waker", "GameCube"), add = true)
            assertEquals(
                1,
                ObserveSetsUseCase(sets)()
                    .first()
                    .single()
                    .entries.size,
            )
            EditSetEntryUseCase(sets)("s", SetEntry("Wind Waker", "GameCube"), add = false)
            assertTrue(
                sets.sets.value
                    .getValue("s")
                    .entries
                    .isEmpty(),
            )

            val vault = FakeVault()
            val games = FakeGames().apply { items.value = mapOf("g" to game) }
            assertTrue(RestoreVaultUseCase(FakeGames(), FakeConsoles(), FakeAccessories(), sets, vault)().isFailure)
            BackupVaultUseCase(games, FakeConsoles(), FakeAccessories(), sets, vault)(mapOf("g" to 30.0)).getOrThrow()
            assertEquals(30.0, vault.stored?.values?.get("g"))
            val restored = FakeGames()
            assertEquals(1, RestoreVaultUseCase(restored, FakeConsoles(), FakeAccessories(), FakeSets(), vault)().getOrThrow())
            assertEquals(game, restored.getById("g"))
            DeleteSetUseCase(sets)("s")
            assertTrue(sets.sets.value.isEmpty())

            assertEquals("x@y.z", SignInUseCase(vault)("x@y.z", "pw").getOrThrow()?.email)
            assertNull(SignInUseCase(vault)(null).getOrThrow())
            assertTrue(ShareLinksUseCase(vault)().getOrThrow().isEmpty())
            assertEquals("u/t", EditShareUseCase(vault)(ShareScope.COLLECTION, "t", false).getOrThrow()?.url)
            assertNull(EditShareUseCase(vault)(ShareScope.COLLECTION, "", false, revoke = "t").getOrThrow())
        }

    @Test
    fun snapshotsGiveThePriceOfAGrade() {
        val s = PriceSnapshot("g", kotlinx.datetime.LocalDate(2026, 1, 1), PriceSource.GAMEYE_US, 1.0, 2.0, 3.0)
        assertEquals(listOf(1.0, 2.0, 3.0), PriceGrade.entries.map { s.price(it) })
        assertEquals(PriceSource.EBAY_PAL, PriceSource.fromKey("ebay_pal"))
        assertNull(PriceSource.fromKey("other"))
    }
}
