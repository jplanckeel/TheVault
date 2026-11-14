package com.thevault.domain

import com.thevault.domain.model.Components
import com.thevault.domain.model.Grade
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.usecase.CsvRow
import com.thevault.domain.usecase.GameyeFormat
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameyeFormatTest {
    @Test
    fun parsesAmountsWithCommaAndUnknowns() {
        assertEquals(10.94, GameyeFormat.amount("10,94"))
        assertEquals(20.0, GameyeFormat.amount("20"))
        assertNull(GameyeFormat.amount(" \"-1.0\""))
        assertNull(GameyeFormat.amount("-1.0"))
        assertNull(GameyeFormat.amount("?"))
        assertEquals("10,94", GameyeFormat.formatAmount(10.94))
        assertEquals("26,6", GameyeFormat.formatAmount(26.6))
        assertEquals("20", GameyeFormat.formatAmount(20.0))
        assertEquals("-1.0", GameyeFormat.formatAmount(null))
    }

    @Test
    fun parsesAndWritesFrenchDates() {
        assertEquals(LocalDate(2021, 11, 28), GameyeFormat.date("28 nov. 2021"))
        assertEquals(LocalDate(2020, 12, 23), GameyeFormat.date("23 déc. 2020"))
        assertEquals(LocalDate(2021, 3, 28), GameyeFormat.date("28 mars 2021"))
        assertEquals(LocalDate(2021, 7, 23), GameyeFormat.date("23 juil. 2021"))
        assertEquals(LocalDate(2021, 3, 24), GameyeFormat.date("2021-03-24 10:12:00"))
        assertNull(GameyeFormat.date("bientôt"))
        listOf(LocalDate(2020, 12, 23), LocalDate(2021, 5, 1), LocalDate(2022, 8, 15)).forEach {
            assertEquals(it, GameyeFormat.date(GameyeFormat.formatDate(it)))
        }
    }

    @Test
    fun mapsGradesAndOwnership() {
        assertEquals(Grade.MINT, GameyeFormat.grade("1.0"))
        assertEquals(Grade.VERY_GOOD, GameyeFormat.grade("0.8"))
        assertEquals(Grade.GOOD, GameyeFormat.grade("0.6"))
        assertNull(GameyeFormat.grade("?"))
        Grade.entries.forEach { assertEquals(it, GameyeFormat.grade(GameyeFormat.formatGrade(it))) }
        listOf("CIB", "Loose", "New", "Boxed", "CIB+", "Loose+", "Dématérialisé").forEach {
            assertEquals(it, GameyeFormat.ownership(GameyeFormat.components(it)))
        }
        assertTrue(GameyeFormat.components("Dématérialisé").digital)
    }

    private fun raw(vararg pairs: Pair<Int, String>): List<String> {
        val row = MutableList(GameyeFormat.HEADERS.size) { "" }
        pairs.forEach { (i, v) -> row[i] = v }
        return row
    }

    @Test
    fun interpretsARowAsPalWithGradesPerPart() {
        val c = GameyeFormat.Col
        val row =
            CsvRow.parse(
                raw(
                    c.PLATFORM to "Nintendo GameCube",
                    c.CATEGORY to "Games",
                    c.RECORD_TYPE to "Owned",
                    c.TITLE to "Zelda",
                    c.COUNTRY to "United States of America",
                    c.RELEASE_TYPE to "Official",
                    c.DEVELOPER to "Missing Field",
                    c.CREATED_AT to "23 déc. 2020",
                    c.OWNERSHIP to "Boxed",
                    c.PRICE_LOOSE to "26,6",
                    c.PRICE_CIB to " \"-1.0\"",
                    c.PRICE_PAID to "-1.0",
                    c.ITEM_CONDITION to "0.8",
                    c.BOX_CONDITION to "1.0",
                    c.MANUAL_CONDITION to "0.6",
                    c.TAGS to "rpg, à finir",
                ),
            )
        assertEquals(ItemStatus.OWNED, row.status)
        assertEquals("", row.developer)
        assertNull(row.price)
        assertEquals(26.6, row.usPrices.loose)
        assertNull(row.usPrices.cib)
        assertEquals(Components(box = true, mediaGrade = Grade.VERY_GOOD, boxGrade = Grade.MINT), row.components)
        assertEquals(listOf("rpg", "à finir"), row.tags)
        assertEquals(com.thevault.domain.model.Region.PAL, row.newGame("x").region)
        assertFalse(row.components.manual)
    }
}
