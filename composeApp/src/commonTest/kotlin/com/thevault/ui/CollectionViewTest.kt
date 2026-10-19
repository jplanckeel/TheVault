package com.thevault.ui

import com.thevault.domain.model.ItemCondition
import com.thevault.domain.model.ItemStatus
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.CollectionFilters
import com.thevault.ui.screen.collection.CollectionSort
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.buildCollectionView
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CollectionViewTest {
    private fun entry(
        id: String,
        kind: ItemKind = ItemKind.Game,
        platform: String = "PS2",
        value: Double? = null,
        acquiredAt: LocalDate? = null,
    ) = CollectionEntry(id, kind, "Title $id", platform, null, ItemCondition.CIB, ItemStatus.OWNED, value, acquiredAt)

    private val entries =
        listOf(
            entry("a", platform = "PS2", value = 30.0, acquiredAt = LocalDate(2024, 1, 1)),
            entry("b", platform = "PS2", value = 10.0, acquiredAt = LocalDate(2025, 6, 1)),
            entry("c", platform = "Switch", value = null, acquiredAt = null),
            entry("d", kind = ItemKind.Console, platform = "Sony", value = 100.0),
        )

    @Test
    fun keepsOnlySelectedKindAndCountsAllKinds() {
        val view = buildCollectionView(entries, CollectionFilters(kind = ItemKind.Game))
        assertEquals(listOf("b", "a", "c"), view.entries.map { it.id })
        assertEquals(mapOf(ItemKind.Game to 3, ItemKind.Console to 1, ItemKind.Accessory to 0), view.countByKind)
        assertEquals(4, view.totalCount)
        assertEquals(140.0, view.totalValue)
    }

    @Test
    fun listsPlatformsByFrequency() {
        val view = buildCollectionView(entries, CollectionFilters())
        assertEquals(listOf("PS2", "Switch"), view.platforms)
    }

    @Test
    fun filtersByPlatformAndQuery() {
        val byPlatform = buildCollectionView(entries, CollectionFilters(platform = "switch"))
        assertEquals(listOf("c"), byPlatform.entries.map { it.id })
        val byQuery = buildCollectionView(entries, CollectionFilters(query = " title a "))
        assertEquals(listOf("a"), byQuery.entries.map { it.id })
    }

    @Test
    fun sortsByValueWithUnknownLast() {
        val view = buildCollectionView(entries, CollectionFilters(sort = CollectionSort.Value))
        assertEquals(listOf("a", "b", "c"), view.entries.map { it.id })
    }
}
