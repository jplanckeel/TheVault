package com.thevault.ui

import com.thevault.domain.model.Components
import com.thevault.domain.model.CopyDetails
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import com.thevault.ui.component.manufacturerOf
import com.thevault.ui.screen.check.quickCheck
import com.thevault.ui.screen.collection.BulkChange
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UsageTest {
    private fun game(
        id: String,
        title: String,
        status: ItemStatus = ItemStatus.OWNED,
    ) = CollectionEntry(id, ItemKind.Game, title, "GameCube", manufacturerOf("GameCube"), null, Components.CIB, Region.PAL, status, null, null, null)

    @Test
    fun bulkChangeKeepsBlankFieldsAndAddsTags() {
        val before = CopyDetails(regionVariant = "UKV", location = "Carton", tags = listOf("rpg"))
        val after = BulkChange(variant = "fah", tags = "à finir, rpg").applyTo(before)
        assertEquals("FAH", after.regionVariant)
        assertEquals("Carton", after.location)
        assertEquals(listOf("rpg", "à finir"), after.tags)
        assertTrue(BulkChange().isEmpty)
    }

    @Test
    fun quickCheckGroupsCopiesAndAnswers() {
        val entries =
            listOf(
                game("1", "The Legend of Zelda: The Wind Waker"),
                game("2", "The Legend of Zelda: The Wind Waker"),
                game("3", "Zelda Four Swords", ItemStatus.WISHLIST),
                game("4", "Mario Kart"),
            )
        val hits = quickCheck("wind zelda", entries)
        assertEquals(1, hits.size)
        assertEquals("Oui, 2 exemplaires", hits.single().verdict)
        assertEquals("Non — dans tes envies", quickCheck("four swords", entries).single().verdict)
        assertTrue(quickCheck("  ", entries).isEmpty())
    }
}
