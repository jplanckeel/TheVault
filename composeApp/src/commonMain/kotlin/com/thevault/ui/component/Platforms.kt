package com.thevault.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.thevault.domain.model.Components
import com.thevault.domain.model.Grade
import com.thevault.domain.model.IgdbPlatformCatalog
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
 * Condition family, as shown by the 4-pip tag and the filters, derived from the parts.
 *
 * @property label Display label.
 * @property pips Lit pips out of 4.
 */
enum class FormatGroup(
    val label: String,
    val pips: Int,
) {
    /** Media only. */
    Loose("Loose", 1),

    /** Boxed (with or without manual). */
    Complete("Complet", 2),

    /** Complete with every part graded mint. */
    New("Neuf", 3),

    /** Sealed. */
    Sealed("Scellé", 4),

    /** Digital copy. */
    Digital("Démat.", 0),
}

/** Condition family of a copy. */
val Components.formatGroup: FormatGroup
    get() =
        when {
            digital -> FormatGroup.Digital
            sealed -> FormatGroup.Sealed
            isMintComplete -> FormatGroup.New
            box -> FormatGroup.Complete
            else -> FormatGroup.Loose
        }

/** Short label of the condition tag ("Loose", "Complet", "Sans notice", "Complet+"…). */
val Components.formatLabel: String
    get() =
        when {
            box && !manual && !sealed -> "Sans notice"
            else -> formatGroup.label + if (extras && formatGroup != FormatGroup.Sealed) "+" else ""
        }

/** Lit pips of the condition tag. */
val Components.pips: Int get() = formatGroup.pips

/**
 * One physical part of a copy for the « Mon exemplaire » panel.
 *
 * @property name Part name.
 * @property present Whether it is there.
 * @property grade Its wear, when known.
 */
data class CopyPart(
    val name: String,
    val present: Boolean,
    val grade: Grade? = null,
)

/** Parts of a copy (none for a digital copy); extras only when present. */
val Components.parts: List<CopyPart>
    get() =
        if (digital) {
            emptyList()
        } else {
            listOfNotNull(
                CopyPart("Support", media, mediaGrade),
                CopyPart("Boîte", box, boxGrade),
                CopyPart("Notice", manual, manualGrade),
                CopyPart("Extras", true).takeIf { extras },
            )
        }
