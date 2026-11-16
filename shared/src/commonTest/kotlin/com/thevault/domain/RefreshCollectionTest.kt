package com.thevault.domain

import com.thevault.data.remote.CatalogRemoteDataSource
import com.thevault.data.remote.dto.CatalogEntryDto
import com.thevault.data.repository.CatalogRepositoryImpl
import com.thevault.domain.model.CopyDetails
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.PricePoint
import com.thevault.domain.model.PriceQuote
import com.thevault.domain.model.PriceSnapshot
import com.thevault.domain.model.Region
import com.thevault.domain.repository.PriceRepository
import com.thevault.domain.repository.SettingsRepository
import com.thevault.domain.usecase.CollectionRefresh
import com.thevault.domain.usecase.RefreshCatalogUseCase
import com.thevault.domain.usecase.RefreshCollectionUseCase
import com.thevault.domain.usecase.RefreshQuotesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RefreshCollectionTest {
    private val game = Game("g", "Zelda", "GameCube", "Action", 0, Region.PAL, CopyDetails(), status = ItemStatus.OWNED)

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

    private class FakePrices(
        private val offline: Boolean = false,
    ) : PriceRepository {
        val requested = mutableListOf<List<String>>()

        override fun observeQuotes() = flowOf(emptyMap<String, List<PriceQuote>>())

        override suspend fun refreshQuotes(itemIds: List<String>): Int {
            if (offline) error("offline")
            requested += itemIds
            return itemIds.size
        }

        override suspend fun history(itemId: String) = emptyList<PricePoint>()

        override fun observeSnapshots() = flowOf(emptyMap<String, List<PriceSnapshot>>())

        override suspend fun replaceImportedPrices(snapshots: List<PriceSnapshot>) = Unit
    }

    private fun remote(offline: Boolean = false) =
        object : CatalogRemoteDataSource {
            override suspend fun lookup(externalIds: List<String>): List<CatalogEntryDto> =
                if (offline) error("offline") else listOf(CatalogEntryDto("g", coverUrl = "https://img/z.jpg"))
        }

    private fun useCase(
        games: FakeGames,
        prices: PriceRepository,
        scheduler: FakeScheduler,
        offline: Boolean = false,
    ): RefreshCollectionUseCase {
        val settings = MemSettings()
        return RefreshCollectionUseCase(
            games,
            FakeConsoles(),
            FakeAccessories(),
            RefreshCatalogUseCase(games, FakeConsoles(), FakeAccessories(), CatalogRepositoryImpl(remote(offline), ""), settings),
            RefreshQuotesUseCase(prices, settings),
            scheduler,
        )
    }

    @Test
    fun pullRefreshPushesThenDownloadsSheetsAndQuotesEveryTime() =
        runTest {
            val games = FakeGames().apply { items.value = mapOf("g" to game) }
            val prices = FakePrices()
            val scheduler = FakeScheduler()
            val refresh = useCase(games, prices, scheduler)

            assertEquals(CollectionRefresh(sheets = 1, quotes = 1), refresh())
            assertEquals(
                "https://img/z.jpg",
                games.items.value
                    .getValue("g")
                    .coverUrl,
            )
            // A second pull is not throttled by the automatic refresh interval.
            assertEquals(CollectionRefresh(sheets = 0, quotes = 1), refresh())
            assertEquals(listOf(listOf("g"), listOf("g")), prices.requested)
            assertEquals(2, scheduler.requests)
        }

    @Test
    fun pullRefreshReportsOfflineServer() =
        runTest {
            val games = FakeGames().apply { items.value = mapOf("g" to game) }
            assertNull(useCase(games, FakePrices(), FakeScheduler(), offline = true)())
            assertNull(useCase(games, FakePrices(offline = true), FakeScheduler())())
        }

    @Test
    fun emptyCollectionIsNotAnError() =
        runTest {
            assertEquals(CollectionRefresh(0, 0), useCase(FakeGames(), FakePrices(), FakeScheduler())())
        }
}
