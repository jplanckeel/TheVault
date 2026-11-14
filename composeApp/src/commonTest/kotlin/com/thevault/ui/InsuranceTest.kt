package com.thevault.ui

import com.thevault.domain.model.Components
import com.thevault.domain.model.CopyDetails
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import com.thevault.ui.component.PriceMask
import com.thevault.ui.component.formatEuro
import com.thevault.ui.component.manufacturerOf
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.settings.insuranceCsv
import com.thevault.ui.screen.settings.insuranceLines
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InsuranceTest {
    private fun e(
        id: String,
        title: String,
        value: Double?,
        paid: Double?,
        status: ItemStatus = ItemStatus.OWNED,
    ) = CollectionEntry(
        id,
        ItemKind.Game,
        title,
        "GameCube",
        manufacturerOf("GameCube"),
        null,
        Components.CIB,
        Region.PAL,
        status,
        value,
        paid,
        null,
        details = CopyDetails(shippingCost = 2.0),
    )

    @Test
    fun inventoryListsOwnedItemsWithTotals() {
        val lines =
            insuranceLines(
                listOf(e("a", "Zelda; edition", 40.0, 20.0), e("b", "Pikmin", 10.0, null), e("c", "Sold", 99.0, 1.0, ItemStatus.SOLD)),
            )
        assertEquals(listOf("Zelda; edition", "Pikmin"), lines.map { it.title })
        val csv = insuranceCsv(lines)
        assertTrue(csv.contains("\"Zelda; edition\";GameCube;Jeu;Complet;;22,00;40,00;Valeur saisie"))
        assertTrue(csv.trim().endsWith(";;;;Total;22,00;50,00;"))
    }

    @Test
    fun priceMaskHidesAmounts() {
        PriceMask.hidden = true
        try {
            assertEquals("••• €", formatEuro(1234.0))
        } finally {
            PriceMask.hidden = false
        }
        assertEquals("1 234 €", formatEuro(1234.0))
    }
}
