package com.thevault.ui.component

import com.thevault.domain.model.IgdbPlatformCatalog
import com.thevault.domain.model.ItemCondition
import com.thevault.ui.theme.BadgeColors
import com.thevault.ui.theme.CoverTints
import com.thevault.ui.theme.FormatBadgeColors
import com.thevault.ui.theme.PlatformBadgeColors

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

/** Badge colors of a platform, one tint per manufacturer. */
fun platformBadgeColors(platform: String): BadgeColors =
    when (manufacturerOf(platform)) {
        Manufacturer.Nintendo -> PlatformBadgeColors.Nintendo
        Manufacturer.Microsoft -> PlatformBadgeColors.Microsoft
        Manufacturer.Sony -> PlatformBadgeColors.Sony
        else -> PlatformBadgeColors.Other
    }

/** Placeholder tint of a cover, per manufacturer. */
fun coverTint(platform: String) =
    when (manufacturerOf(platform)) {
        Manufacturer.Nintendo -> CoverTints.Nintendo
        Manufacturer.Microsoft -> CoverTints.Microsoft
        Manufacturer.Sony -> CoverTints.Sony
        else -> CoverTints.Other
    }

/**
 * Format family of a condition (« Format » in the design).
 *
 * @property label Filter / stats label.
 */
enum class FormatGroup(
    val label: String,
) {
    /** Complete in box (with or without manual). */
    Cib("CIB"),

    /** Cartridge / disc only. */
    Loose("Loose"),

    /** New, sealed or mint. */
    New("Neuf"),

    /** Graded by state (very good, good…). */
    Other("Autres"),
}

/** Format family of a condition. */
val ItemCondition.formatGroup: FormatGroup
    get() =
        when (this) {
            ItemCondition.CIB, ItemCondition.CIB_NO_MANUAL -> FormatGroup.Cib
            ItemCondition.LOOSE -> FormatGroup.Loose
            ItemCondition.NEW_SEALED, ItemCondition.MINT -> FormatGroup.New
            else -> FormatGroup.Other
        }

/** Short format label used in badges ("CIB", "CIB−", "Loose", "Neuf"…). */
val ItemCondition.formatLabel: String
    get() =
        when (this) {
            ItemCondition.CIB -> "CIB"
            ItemCondition.CIB_NO_MANUAL -> "CIB−"
            ItemCondition.LOOSE -> "Loose"
            ItemCondition.NEW_SEALED -> "Neuf"
            ItemCondition.MINT -> "Mint"
            else -> label
        }

/** Badge colors of a condition. */
val ItemCondition.formatColors: BadgeColors
    get() =
        when (formatGroup) {
            FormatGroup.Cib -> FormatBadgeColors.Cib
            FormatGroup.Loose -> FormatBadgeColors.Loose
            FormatGroup.New -> FormatBadgeColors.New
            FormatGroup.Other -> FormatBadgeColors.Other
        }
