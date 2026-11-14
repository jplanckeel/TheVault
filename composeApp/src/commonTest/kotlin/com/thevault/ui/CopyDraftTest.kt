package com.thevault.ui

import com.thevault.domain.model.Components
import com.thevault.domain.model.CopyDetails
import com.thevault.domain.model.Edition
import com.thevault.ui.screen.copy.CopyDraft
import com.thevault.ui.screen.copy.toggle
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CopyDraftTest {
    @Test
    fun parsesFrenchDates() {
        assertEquals(LocalDate(2024, 3, 12), CopyDraft.parseDate("12/03/2024"))
        assertEquals(LocalDate(2024, 3, 12), CopyDraft.parseDate("12.03.24"))
        assertEquals(LocalDate(2024, 3, 12), CopyDraft.parseDate("2024-03-12"))
        assertNull(CopyDraft.parseDate("31/02/2024"))
        assertEquals("05/01/2026", CopyDraft.formatDate(LocalDate(2026, 1, 5)))
    }

    @Test
    fun roundTripsDetails() {
        val details =
            CopyDetails(
                components = Components(box = true),
                edition = Edition.PLATINUM,
                regionVariant = "FAH",
                shippingCost = 4.5,
                acquiredFrom = "Vinted",
                location = "Carton bleu",
                tags = listOf("rpg", "à finir"),
                barcode = "123",
            )
        val draft = CopyDraft.of(details, 12.5, LocalDate(2024, 3, 12))
        assertEquals("12,5", draft.price)
        assertEquals("4,5", draft.shipping)
        assertEquals(details, draft.toDetails())
        assertEquals(12.5, draft.purchasePrice)
        assertEquals(LocalDate(2024, 3, 12), draft.purchaseDate)
        assertFalse(draft.copy(soldAt = "hier").datesValid)
        assertEquals(listOf("a", "b"), draft.copy(tags = " a ; b, a ").toDetails().tags)
        assertEquals("UKV", draft.copy(regionVariant = " ukv ").toDetails().regionVariant)
    }

    @Test
    fun togglesKeepPartsConsistent() {
        val sealed = Components.LOOSE.toggle("sealed")
        assertTrue(sealed.sealed && sealed.box && sealed.manual)
        assertFalse(sealed.toggle("box").sealed)
        assertTrue(Components.CIB.toggle("digital").digital)
        assertFalse(Components.DIGITAL.toggle("digital").digital)
        assertTrue(Components.DIGITAL.toggle("box").box)
    }
}
