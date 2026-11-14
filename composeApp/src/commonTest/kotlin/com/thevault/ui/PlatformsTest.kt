package com.thevault.ui

import com.thevault.domain.model.Components
import com.thevault.domain.model.Grade
import com.thevault.ui.component.FormatGroup
import com.thevault.ui.component.Manufacturer
import com.thevault.ui.component.formatGroup
import com.thevault.ui.component.formatLabel
import com.thevault.ui.component.manufacturerOf
import com.thevault.ui.component.parts
import com.thevault.ui.component.shortPlatformName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatformsTest {
    @Test
    fun detectsManufacturers() {
        mapOf(
            "Nintendo GameCube" to Manufacturer.Nintendo,
            "Wii" to Manufacturer.Nintendo,
            "NES/Famicom" to Manufacturer.Nintendo,
            "Nintendo Switch 2" to Manufacturer.Nintendo,
            "Microsoft Xbox 360" to Manufacturer.Microsoft,
            "Xbox Series X" to Manufacturer.Microsoft,
            "Sony PlayStation 4" to Manufacturer.Sony,
            "PS5" to Manufacturer.Sony,
            "Sega Mega Drive" to Manufacturer.Sega,
            "PC" to Manufacturer.Other,
            "Android" to Manufacturer.Other,
        ).forEach { (platform, expected) -> assertEquals(expected, manufacturerOf(platform), platform) }
    }

    @Test
    fun shortensPlatformNames() {
        mapOf(
            "Nintendo GameCube" to "GameCube",
            "Nintendo Switch 2" to "Switch 2",
            "Sony PlayStation 4" to "PS4",
            "PlayStation 5" to "PS5",
            "Microsoft Xbox 360" to "Xbox 360",
            "NES/Famicom" to "NES",
            "Nintendo 64" to "N64",
            "Game Boy Advance" to "GBA",
            "PC" to "PC",
        ).forEach { (platform, expected) -> assertEquals(expected, shortPlatformName(platform), platform) }
    }

    @Test
    fun groupsFormats() {
        assertEquals(FormatGroup.Complete, Components(box = true).formatGroup)
        assertEquals("Sans notice", Components(box = true).formatLabel)
        assertEquals("Complet+", Components.CIB.copy(extras = true).formatLabel)
        assertEquals(
            FormatGroup.New,
            Components.CIB.copy(mediaGrade = Grade.MINT, boxGrade = Grade.MINT, manualGrade = Grade.MINT).formatGroup,
        )
        assertEquals(FormatGroup.Loose, Components.LOOSE.formatGroup)
        assertEquals(FormatGroup.Sealed, Components.SEALED.formatGroup)
        assertEquals(FormatGroup.Digital, Components.DIGITAL.formatGroup)
        assertEquals(listOf("Support", "Boîte", "Notice"), Components.CIB.parts.map { it.name })
        assertTrue(Components.DIGITAL.parts.isEmpty())
    }
}

class FormatDateTest {
    @Test
    fun formatsFrenchDates() {
        assertEquals(
            "24 mars 2021",
            com.thevault.ui.component
                .formatDate(kotlinx.datetime.LocalDate(2021, 3, 24)),
        )
        assertEquals(
            "1 déc. 2025",
            com.thevault.ui.component
                .formatDate(kotlinx.datetime.LocalDate(2025, 12, 1)),
        )
    }
}
