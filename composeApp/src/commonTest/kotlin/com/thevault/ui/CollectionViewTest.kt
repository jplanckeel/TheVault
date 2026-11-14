package com.thevault.ui

import com.thevault.domain.model.Components
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import com.thevault.ui.component.FormatGroup
import com.thevault.ui.component.Manufacturer
import com.thevault.ui.component.manufacturerOf
import com.thevault.ui.screen.collection.AccessoryFilter
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.CollectionFilters
import com.thevault.ui.screen.collection.GameFilters
import com.thevault.ui.screen.collection.GameSort
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.buildCollectionState
import com.thevault.ui.screen.collection.buildHomeSummary
import com.thevault.ui.screen.collection.buildStatsSummary
import com.thevault.ui.screen.collection.filterGames
import com.thevault.ui.screen.collection.manufacturerGroups
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CollectionViewTest {
    private fun entry(
        id: String,
        platform: String = "Nintendo GameCube",
        kind: ItemKind = ItemKind.Game,
        value: Double? = null,
        paid: Double? = null,
        condition: Components = Components.CIB,
        status: ItemStatus = ItemStatus.OWNED,
        date: LocalDate? = null,
        subtype: String = "",
    ) = CollectionEntry(
        id,
        kind,
        "Title $id",
        platform,
        manufacturerOf(platform),
        null,
        condition,
        Region.PAL,
        status,
        value,
        paid,
        date,
        subtype,
    )

    private val games =
        listOf(
            entry("a", value = 30.0, date = LocalDate(2024, 1, 1)),
            entry("b", value = 250.0, condition = Components.LOOSE, date = LocalDate(2025, 6, 1)),
            entry("c", platform = "Microsoft Xbox 360", value = 10.0),
            entry("d", platform = "Wii"),
        )

    @Test
    fun groupsPlatformsByManufacturer() {
        val groups = manufacturerGroups(games)
        assertEquals(listOf(Manufacturer.Nintendo, Manufacturer.Microsoft), groups.map { it.manufacturer })
        val nintendo = groups.first()
        assertEquals(listOf("Nintendo GameCube", "Wii"), nintendo.platforms.map { it.platform })
        assertEquals(280.0, nintendo.platforms.first().value)
        assertEquals(3, nintendo.count)
    }

    @Test
    fun filtersAndSortsGames() {
        val gc = GameFilters(platform = "Nintendo GameCube")
        assertEquals(listOf("b", "a"), filterGames(games, gc).map { it.id })
        assertEquals(listOf("a"), filterGames(games, gc.copy(format = FormatGroup.Complete)).map { it.id })
        assertEquals(listOf("b", "a"), filterGames(games, gc.copy(sort = GameSort.Recent)).map { it.id })
        assertEquals(listOf("c"), filterGames(games, GameFilters(query = "title c")).map { it.id })
    }

    @Test
    fun filtersAccessoriesByControllerType() {
        val accessories =
            listOf(
                entry("pad", kind = ItemKind.Accessory, subtype = "Manette"),
                entry("disc", kind = ItemKind.Accessory, subtype = "Lecteur"),
            )
        val controllers = buildCollectionState(accessories, CollectionFilters(accessoryFilter = AccessoryFilter.Controllers))
        assertEquals(listOf("pad"), controllers.accessories.map { it.id })
        val others = buildCollectionState(accessories, CollectionFilters(accessoryFilter = AccessoryFilter.Others))
        assertEquals(listOf("disc"), others.accessories.map { it.id })
    }

    @Test
    fun homeSummaryIgnoresWishlistAndSoldItems() {
        val all = games + entry("w", status = ItemStatus.WISHLIST, value = 999.0) + entry("s", status = ItemStatus.SOLD, value = 500.0)
        val home = buildHomeSummary(all)
        assertEquals(4, home.totalCount)
        assertEquals(290.0, home.totalValue)
        assertEquals(3, home.platformCount)
        assertEquals(listOf("b", "a", "c"), home.topValue.map { it.id })
        assertEquals("Nintendo GameCube", home.platforms.first().platform)
    }

    @Test
    fun statsCountFormatsAndPurchases() {
        val stats = buildStatsSummary(games + entry("e", paid = 20.0, condition = Components.SEALED))
        assertEquals(3, stats.formats[FormatGroup.Complete])
        assertEquals(1, stats.formats[FormatGroup.Loose])
        assertEquals(1, stats.formats[FormatGroup.Sealed])
        assertEquals(1, stats.purchases.filled)
        assertEquals(4, stats.purchases.missing)
        assertEquals(20.0, stats.purchases.invested)
    }
}
