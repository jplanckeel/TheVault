@file:Suppress("MagicNumber") // Design tokens and geometry of the console mockups.

package com.thevault.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Design « console » — two shells: « Coque grise » (light) and « Black Edition » (dark).
// Source: design/console/README.md (mockups « TheVault — Maquettes app mobile.html »).

/**
 * Color tokens of one shell.
 *
 * @property shell Page background (the console plastic).
 * @property panel Raised surfaces: cards, tab bar, key buttons.
 * @property keycap Selected segment, the lightest surface.
 * @property well Recessed surfaces: segmented tracks, search, chart screen.
 * @property line Hairlines, empty pips.
 * @property lineStrong Strong hairlines, LED off.
 * @property ink Main text.
 * @property inkMuted Secondary text.
 * @property inkFaint Serials, timestamps.
 * @property indigo Single action color (« face A » button, active pips).
 * @property onIndigo Content on [indigo].
 * @property indigoInk Links and chart line.
 * @property indigoSoft Ghost button pressed background.
 * @property lavender Secondary button.
 * @property onLavender Content on [lavender].
 * @property ledPower Red LED: stale data, errors, chart tip.
 * @property ledSync Green LED: fresh data.
 * @property gain Price increase.
 * @property loss Price decrease.
 * @property focus Focus ring.
 * @property scrim Modal scrim.
 * @property platNintendo Nintendo band.
 * @property platSony Sony band.
 * @property platSega Sega band.
 * @property platXbox Microsoft band.
 * @property platOther Other manufacturers band.
 * @property coverBlank Cover placeholder.
 * @property snesA Logo dot A (red).
 * @property snesB Logo dot B (yellow).
 * @property snesX Logo dot X (blue).
 * @property snesY Logo dot Y (green).
 * @property moldedShadow Shadow of raised surfaces (transparent on the dark shell).
 * @property moldedBorder Outline of raised surfaces (transparent on the light shell).
 * @property wellBorder Inner outline of recessed surfaces.
 * @property faceShadow Glow under the indigo buttons.
 * @property floatShadow Shadow of floating elements (tab bar, sheets).
 * @property isDark Whether the shell is dark.
 */
@Immutable
data class VaultColors(
    val shell: Color,
    val panel: Color,
    val keycap: Color,
    val well: Color,
    val line: Color,
    val lineStrong: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val indigo: Color,
    val onIndigo: Color,
    val indigoInk: Color,
    val indigoSoft: Color,
    val lavender: Color,
    val onLavender: Color,
    val ledPower: Color,
    val ledSync: Color,
    val gain: Color,
    val loss: Color,
    val focus: Color,
    val scrim: Color,
    val platNintendo: Color,
    val platSony: Color,
    val platSega: Color,
    val platXbox: Color,
    val platOther: Color,
    val coverBlank: Color,
    val snesA: Color,
    val snesB: Color,
    val snesX: Color,
    val snesY: Color,
    val moldedShadow: Color,
    val moldedBorder: Color,
    val wellBorder: Color,
    val faceShadow: Color,
    val floatShadow: Color,
    val isDark: Boolean,
)

/** « Coque grise » — the light grey shell. */
val ShellColors =
    VaultColors(
        shell = Color(0xFFD6D3CC),
        panel = Color(0xFFEEECE7),
        keycap = Color(0xFFFAF9F6),
        well = Color(0xFFC3BFB7),
        line = Color(0xFFB4B0A7),
        lineStrong = Color(0xFF78746D),
        ink = Color(0xFF1F1D22),
        inkMuted = Color(0xFF4D4A51),
        inkFaint = Color(0xFF59565D),
        indigo = Color(0xFF4A3AA8),
        onIndigo = Color(0xFFFFFFFF),
        indigoInk = Color(0xFF3F3196),
        indigoSoft = Color(0x244A3AA8),
        lavender = Color(0xFFABA3DD),
        onLavender = Color(0xFF1F1D22),
        ledPower = Color(0xFFD9362E),
        ledSync = Color(0xFF2B9A55),
        gain = Color(0xFF114F2B),
        loss = Color(0xFF8A1F17),
        focus = Color(0xFF3F3196),
        scrim = Color(0x661F1D22),
        platNintendo = Color(0xFFD7373D),
        platSony = Color(0xFF2F62E0),
        platSega = Color(0xFF0F8C95),
        platXbox = Color(0xFF3A8F30),
        platOther = Color(0xFF8E6C47),
        coverBlank = Color(0xFF3A3840),
        snesA = Color(0xFFD83A35),
        snesB = Color(0xFFE2B02A),
        snesX = Color(0xFF3466D6),
        snesY = Color(0xFF2E9E5B),
        moldedShadow = Color(0x401F1D22),
        moldedBorder = Color.Transparent,
        wellBorder = Color(0x0F1F1D22),
        faceShadow = Color(0x804A3AA8),
        floatShadow = Color(0x661F1D22),
        isDark = false,
    )

/** « Black Edition » — the dark shell. */
val BlackColors =
    VaultColors(
        shell = Color(0xFF16151A),
        panel = Color(0xFF211F27),
        keycap = Color(0xFF2D2B34),
        well = Color(0xFF0E0D11),
        line = Color(0xFF35343C),
        lineStrong = Color(0xFF6B6974),
        ink = Color(0xFFECEAF0),
        inkMuted = Color(0xFFA9A6B1),
        inkFaint = Color(0xFF8D8A96),
        indigo = Color(0xFF6E5CE6),
        onIndigo = Color(0xFFFFFFFF),
        indigoInk = Color(0xFFA99CFF),
        indigoSoft = Color(0x2E6E5CE6),
        lavender = Color(0xFF3D3756),
        onLavender = Color(0xFFECEAF0),
        ledPower = Color(0xFFFF4D42),
        ledSync = Color(0xFF41D17A),
        gain = Color(0xFF5BD68A),
        loss = Color(0xFFFF7B6E),
        focus = Color(0xFFA99CFF),
        scrim = Color(0xA6000000),
        platNintendo = Color(0xFFE5484D),
        platSony = Color(0xFF5B8CFF),
        platSega = Color(0xFF33C2CC),
        platXbox = Color(0xFF6CCB5F),
        platOther = Color(0xFFC7A37C),
        coverBlank = Color(0xFF2A2930),
        snesA = Color(0xFFF0524C),
        snesB = Color(0xFFF3C649),
        snesX = Color(0xFF5B8CFF),
        snesY = Color(0xFF4CC27A),
        moldedShadow = Color.Transparent,
        moldedBorder = Color(0x0DFFFFFF),
        wellBorder = Color(0x0AFFFFFF),
        faceShadow = Color(0x996E5CE6),
        floatShadow = Color(0xCC000000),
        isDark = true,
    )

/** Fixed colors of the overlays drawn on covers and of the achievement toast (same in both shells). */
object OverlayColors {
    /** Condition tag background on covers. */
    val TagBackground = Color(0xCC16151A)

    /** Text on cover overlays. */
    val TagContent = Color(0xFFECEAF0)

    /** Empty pip on covers. */
    val PipOff = Color(0x33FFFFFF)

    /** Lit pip on covers and toast accents. */
    val PipOn = Color(0xFFA99CFF)

    /** Title printed on placeholder covers. */
    val CoverTitle = Color(0xFFF4F2ED)

    /** Title strip of placeholder covers. */
    val CoverStrip = Color(0xD916151A)

    /** Toast background. */
    val Toast = Color(0xF216151A)

    /** Toast medal. */
    val ToastMedal = Color(0xFF6E5CE6)
}
