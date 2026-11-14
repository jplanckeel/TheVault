package com.thevault.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.thevault.domain.model.IgdbPlatformCatalog
import com.thevault.domain.model.ItemCondition
import com.thevault.ui.theme.Vault

/**
 * Console manufacturer, used to group platforms and tint their badges.
 *
 * @property label Display name.
 * @property order Display order of the groups in the collection.
 */
enum class Manufacturer(
    val label: String,
    val order: Int,
) {
    /** Nintendo. */
    Nintendo("Nintendo", 0),

    /** Microsoft. */
    Microsoft("Microsoft", 1),

    /** Sony. */
    Sony("Sony", 2),

    /** Sega. */
    Sega("Sega", 3),

    /** Any other manufacturer. */
    Other("Autres", 4),
}

private val nintendoPattern =
    Regex(
        """nintendo|\bnes\b|famicom|snes|\bn64\b|gamecube|\bwii\b|wii u|switch|game ?boy|\bgb[ac]?\b|\bds\b|\b3ds\b|virtual boy""",
        RegexOption.IGNORE_CASE,
    )
private val microsoftPattern = Regex("""xbox|microsoft""", RegexOption.IGNORE_CASE)
private val sonyPattern = Regex("""playstation|sony|\bps[1-5]\b|\bpsp\b|vita""", RegexOption.IGNORE_CASE)
private val segaPattern = Regex("""sega|mega ?drive|genesis|dreamcast|saturn|game gear""", RegexOption.IGNORE_CASE)

private val computerPattern = Regex("""^(pc|windows|pc \(microsoft windows\)|mac|macos|linux)$""", RegexOption.IGNORE_CASE)

/**
 * Manufacturer of a platform (e.g. "Nintendo GameCube", "Wii", "Xbox 360", "PS5").
 * Computers are not console makers: "PC" stays [Manufacturer.Other] even though the
 * IGDB catalog files it under Microsoft.
 */
fun manufacturerOf(platform: String): Manufacturer =
    if (computerPattern.matches(platform.trim())) {
        Manufacturer.Other
    } else {
        catalogManufacturer(platform)
    }

private fun catalogManufacturer(platform: String): Manufacturer =
    when (IgdbPlatformCatalog.find(platform)?.manufacturer) {
        "Nintendo" -> Manufacturer.Nintendo
        "Microsoft" -> Manufacturer.Microsoft
        "Sony" -> Manufacturer.Sony
        "Sega" -> Manufacturer.Sega
        else ->
            when {
                microsoftPattern.containsMatchIn(platform) -> Manufacturer.Microsoft
                sonyPattern.containsMatchIn(platform) -> Manufacturer.Sony
                segaPattern.containsMatchIn(platform) -> Manufacturer.Sega
                nintendoPattern.containsMatchIn(platform) -> Manufacturer.Nintendo
                else -> Manufacturer.Other
            }
    }

private val explicitShortNames =
    mapOf(
        "nes/famicom" to "NES",
        "nintendo entertainment system" to "NES",
        "super nintendo entertainment system" to "SNES",
        "super nintendo" to "SNES",
        "nintendo 64" to "N64",
        "game boy advance" to "GBA",
        "game boy color" to "GBC",
        "nintendo game boy advance" to "GBA",
        "nintendo game boy color" to "GBC",
        "playstation" to "PS1",
        "sony playstation" to "PS1",
        "playstation portable" to "PSP",
        "playstation vita" to "PS Vita",
    )

/**
 * Short platform name shown in badges: "Nintendo GameCube" → "GameCube",
 * "Sony PlayStation 4" → "PS4", "Microsoft Xbox Series X" → "Xbox Series X".
 */
fun shortPlatformName(platform: String): String {
    val trimmed = platform.trim()
    explicitShortNames[trimmed.lowercase()]?.let { return it }
    Regex("""(?i)^(?:sony\s+)?playstation\s+(\d)$""").find(trimmed)?.let { return "PS${it.groupValues[1]}" }
    return trimmed
        .replace(Regex("""(?i)^(nintendo|microsoft|sony|sega)\s+"""), "")
        .ifBlank { trimmed }
}

/** Band color of a manufacturer (« familles constructeur »). */
val Manufacturer.color: Color
    @Composable @ReadOnlyComposable
    get() =
        when (this) {
            Manufacturer.Nintendo -> Vault.colors.platNintendo
            Manufacturer.Microsoft -> Vault.colors.platXbox
            Manufacturer.Sony -> Vault.colors.platSony
            Manufacturer.Sega -> Vault.colors.platSega
            Manufacturer.Other -> Vault.colors.platOther
        }

/** Band color of a platform. */
@Composable
@ReadOnlyComposable
fun platformColor(platform: String): Color = manufacturerOf(platform).color

/**
 * Condition family, as shown by the 4-pip tag and the filters.
 *
 * @property label Display label.
 * @property pips Lit pips out of 4.
 */
enum class FormatGroup(
    val label: String,
    val pips: Int,
) {
    /** Cartridge / disc only. */
    Loose("Loose", 1),

    /** Complete in box (with or without manual). */
    Complete("Complet", 2),

    /** New / mint. */
    New("Neuf", 3),

    /** Sealed. */
    Sealed("Scellé", 4),

    /** Graded by wear (« Très bon »…). */
    Other("Autres", 0),
}

/** Condition family of a condition. */
val ItemCondition.formatGroup: FormatGroup
    get() =
        when (this) {
            ItemCondition.LOOSE -> FormatGroup.Loose
            ItemCondition.CIB, ItemCondition.CIB_NO_MANUAL -> FormatGroup.Complete
            ItemCondition.MINT -> FormatGroup.New
            ItemCondition.NEW_SEALED -> FormatGroup.Sealed
            else -> FormatGroup.Other
        }

/** Short label of the condition tag ("Loose", "Complet", "Sans notice", "Scellé"…). */
val ItemCondition.formatLabel: String
    get() =
        if (this == ItemCondition.CIB_NO_MANUAL) "Sans notice" else formatGroup.takeIf { it != FormatGroup.Other }?.label ?: label

/** Lit pips of the condition tag. */
val ItemCondition.pips: Int get() = formatGroup.pips

/**
 * Parts of the copy: cartridge, box, manual — derived from the condition, null when the
 * condition does not tell (wear grades).
 */
val ItemCondition.parts: List<Pair<String, Boolean>>?
    get() =
        when (this) {
            ItemCondition.LOOSE -> listOf("Cartouche" to true, "Boîte" to false, "Notice" to false)
            ItemCondition.CIB_NO_MANUAL -> listOf("Cartouche" to true, "Boîte" to true, "Notice" to false)
            ItemCondition.CIB, ItemCondition.MINT, ItemCondition.NEW_SEALED ->
                listOf("Cartouche" to true, "Boîte" to true, "Notice" to true)
            else -> null
        }
