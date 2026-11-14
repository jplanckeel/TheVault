package com.thevault.ui

import com.thevault.domain.model.ItemCondition
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import com.thevault.domain.model.RemoteGame
import com.thevault.ui.component.Manufacturer
import com.thevault.ui.screen.add.cibShare
import com.thevault.ui.screen.add.isOwned
import com.thevault.ui.screen.add.ownedKeys
import com.thevault.ui.screen.add.preferredRegion
import com.thevault.ui.screen.add.toAmount
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddModelsTest {
    private fun game(
        title: String,
        platform: String,
        condition: ItemCondition = ItemCondition.CIB,
        region: Region = Region.PAL,
    ) = CollectionEntry(
        title,
        ItemKind.Game,
        title,
        platform,
        Manufacturer.Nintendo,
        null,
        condition,
        region,
        ItemStatus.OWNED,
        null,
        null,
        null,
    )

    @Test
    fun flagsOwnedResultsIgnoringCaseAndPunctuation() {
        val keys = ownedKeys(listOf(game("Paper Mario: The Thousand-Year Door", "Nintendo GameCube")))
        assertTrue(RemoteGame("1", "Paper Mario - The Thousand Year Door", platformNames = listOf("Nintendo GameCube")).isOwned(keys))
        assertFalse(RemoteGame("2", "Paper Mario: Sticker Star", platformNames = listOf("Nintendo 3DS")).isOwned(keys))
    }

    @Test
    fun computesHints() {
        val entries = listOf(game("a", "x"), game("b", "x", ItemCondition.LOOSE, Region.NTSC_US), game("c", "x", region = Region.NTSC_US))
        assertEquals(66, cibShare(entries))
        assertEquals(Region.NTSC_US, preferredRegion(entries))
        assertNull(cibShare(emptyList()))
        assertEquals(Region.PAL, preferredRegion(emptyList()))
    }

    @Test
    fun parsesAmounts() {
        assertEquals(12.5, "12,50".toAmount())
        assertNull("".toAmount())
    }
}
