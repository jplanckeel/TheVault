package com.thevault.domain

import com.thevault.data.mapper.toDomain
import com.thevault.data.mapper.toDto
import com.thevault.data.remote.dto.VaultBackupDto
import com.thevault.domain.model.Accessory
import com.thevault.domain.model.Completeness
import com.thevault.domain.model.Components
import com.thevault.domain.model.Console
import com.thevault.domain.model.CopyDetails
import com.thevault.domain.model.Edition
import com.thevault.domain.model.Game
import com.thevault.domain.model.Grade
import com.thevault.domain.model.ItemSet
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.PlayLog
import com.thevault.domain.model.PlayStatus
import com.thevault.domain.model.Region
import com.thevault.domain.model.SetEntry
import com.thevault.domain.model.VaultContent
import com.thevault.domain.model.WishPrefs
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupMapperTest {
    private val details =
        CopyDetails(
            components = Components(box = true, manual = true, mediaGrade = Grade.GOOD),
            regionVariant = "FAH",
            edition = Edition.COLLECTOR,
            shippingCost = 3.0,
            location = "Étagère",
            tags = listOf("rpg"),
            soldPrice = 40.0,
            soldAt = LocalDate(2026, 1, 2),
            wish = WishPrefs(2, 30.0, Completeness.CIB),
        )

    private val content =
        VaultContent(
            games =
                listOf(
                    Game(
                        "g",
                        "Zelda",
                        "GameCube",
                        "Action",
                        2003,
                        Region.PAL,
                        details,
                        "https://c",
                        ItemStatus.SOLD,
                        20.0,
                        35.0,
                        "note",
                        LocalDate(2021, 3, 24),
                        9,
                        "Nintendo",
                        "Nintendo",
                        null,
                        null,
                        PlayLog(PlayStatus.FINISHED, 1200),
                    ),
                ),
            consoles = listOf(Console("c", "GameCube", "Nintendo", Region.PAL, "Indigo", details, 2001, ItemStatus.OWNED)),
            accessories = listOf(Accessory("a", "Manette", "Controller", "GameCube", details, ItemStatus.WISHLIST)),
            sets = listOf(ItemSet("s", "Zelda", listOf(SetEntry("Wind Waker", "GameCube")))),
            values = mapOf("g" to 35.0),
        )

    @Test
    fun backupRoundTripsThroughJson() {
        val json = Json { ignoreUnknownKeys = true }
        val text = json.encodeToString(VaultBackupDto.serializer(), content.toDto("2026-11-14T10:00:00Z"))
        val back = json.decodeFromString(VaultBackupDto.serializer(), text).toDomain()
        assertEquals(content.copy(values = emptyMap()), back)
        assertTrue(text.contains("\"completeness\":\"CIB\""))
        assertTrue(text.contains("\"value\":35.0"))
    }
}
