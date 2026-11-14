package com.thevault.ui

import com.thevault.domain.model.Components
import com.thevault.domain.model.ItemSet
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.PriceGrade
import com.thevault.domain.model.PriceQuote
import com.thevault.domain.model.Region
import com.thevault.domain.model.SetEntry
import com.thevault.ui.component.manufacturerOf
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.withMarket
import com.thevault.ui.screen.sets.completions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SetCompletionTest {
    private fun game(
        title: String,
        status: ItemStatus = ItemStatus.OWNED,
    ) = CollectionEntry(
        title,
        ItemKind.Game,
        title,
        "Nintendo GameCube",
        manufacturerOf("GameCube"),
        null,
        Components.CIB,
        Region.PAL,
        status,
        null,
        null,
        null,
    )

    @Test
    fun matchesTitlesIgnoringCaseAndPunctuation() {
        val set =
            ItemSet(
                "s",
                "Zelda",
                listOf(
                    SetEntry("The Legend of Zelda: The Wind Waker", "Nintendo GameCube"),
                    SetEntry("Zelda Four Swords Adventures", "Nintendo GameCube"),
                    SetEntry("Twilight Princess", "Nintendo GameCube"),
                ),
            )
        val quote = PriceQuote("Twilight Princess", PriceGrade.CIB, 40.0, 30.0, 50.0, 6, Instant.fromEpochSeconds(0))
        val entries =
            listOf(game("the legend of zelda - the wind waker"), game("Twilight Princess", ItemStatus.WISHLIST), game("Mario Kart"))
                .withMarket(mapOf("Twilight Princess" to listOf(quote)))
        val c = completions(listOf(set), entries).single()
        assertEquals(1, c.ownedCount)
        assertEquals(2, c.missing.size)
        assertEquals(40.0, c.estimatedCost)
        assertEquals(1, c.unquoted)
        assertEquals(1f / 3, c.ratio)
    }
}
