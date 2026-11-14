@file:Suppress("MagicNumber") // Design tokens and geometry of the console mockups.

package com.thevault.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import thevault.composeapp.generated.resources.Res
import thevault.composeapp.generated.resources.archivo_bold
import thevault.composeapp.generated.resources.archivo_bold_expanded
import thevault.composeapp.generated.resources.archivo_extrabold
import thevault.composeapp.generated.resources.archivo_extrabold_expanded
import thevault.composeapp.generated.resources.ibmplexmono_medium
import thevault.composeapp.generated.resources.ibmplexmono_semibold
import thevault.composeapp.generated.resources.ibmplexsans_medium
import thevault.composeapp.generated.resources.ibmplexsans_regular
import thevault.composeapp.generated.resources.ibmplexsans_semibold

/**
 * Text styles of the console design (design/console/README.md).
 *
 * @property displayXl 40/44 Archivo 800 — vault value.
 * @property displayL 28/32 Archivo 800 — screen titles.
 * @property displayM 18/24 Archivo 800 — section titles, platform names.
 * @property engrave 10/12 Archivo Expanded 700, uppercase — engraved labels.
 * @property title 16/22 Plex Sans 600.
 * @property body 15/22 Plex Sans 400.
 * @property bodyStrong 15/22 Plex Sans 600 — list titles.
 * @property label 13/18 Plex Sans 600 — chips, segments, links.
 * @property caption 13/18 Plex Sans 400 — metadata.
 * @property small 12/16 Plex Sans 500 — tab labels, stat labels.
 * @property monoPrice 15/20 Plex Mono 600 — prices.
 * @property monoTag 11/14 Plex Mono 500 — serials, platforms, timestamps.
 * @property display Archivo family (normal width).
 * @property mono Plex Mono family.
 */
@Immutable
data class VaultType(
    val displayXl: TextStyle,
    val displayL: TextStyle,
    val displayM: TextStyle,
    val engrave: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val small: TextStyle,
    val monoPrice: TextStyle,
    val monoTag: TextStyle,
    val display: FontFamily,
    val mono: FontFamily,
)

/** Builds the text styles with the bundled fonts. */
@Composable
fun vaultType(): VaultType {
    val archivo =
        FontFamily(
            Font(Res.font.archivo_bold, FontWeight.Bold),
            Font(Res.font.archivo_extrabold, FontWeight.ExtraBold),
        )
    val archivoWide =
        FontFamily(
            Font(Res.font.archivo_bold_expanded, FontWeight.Bold),
            Font(Res.font.archivo_extrabold_expanded, FontWeight.ExtraBold),
        )
    val plex =
        FontFamily(
            Font(Res.font.ibmplexsans_regular, FontWeight.Normal),
            Font(Res.font.ibmplexsans_medium, FontWeight.Medium),
            Font(Res.font.ibmplexsans_semibold, FontWeight.SemiBold),
        )
    val mono =
        FontFamily(
            Font(Res.font.ibmplexmono_medium, FontWeight.Medium),
            Font(Res.font.ibmplexmono_semibold, FontWeight.SemiBold),
        )
    return VaultType(
        displayXl = style(archivo, FontWeight.ExtraBold, 40, 44, (-1.2).sp, tabular = true),
        displayL = style(archivo, FontWeight.ExtraBold, 28, 32, (-0.8).sp),
        displayM = style(archivo, FontWeight.ExtraBold, 18, 24, (-0.3).sp),
        engrave = style(archivoWide, FontWeight.Bold, 10, 12, 1.6.sp),
        title = style(plex, FontWeight.SemiBold, 16, 22),
        body = style(plex, FontWeight.Normal, 15, 22),
        bodyStrong = style(plex, FontWeight.SemiBold, 15, 22),
        label = style(plex, FontWeight.SemiBold, 13, 18),
        caption = style(plex, FontWeight.Normal, 13, 18),
        small = style(plex, FontWeight.Medium, 12, 16),
        monoPrice = style(mono, FontWeight.SemiBold, 15, 20),
        monoTag = style(mono, FontWeight.Medium, 11, 14, 0.4.sp),
        display = archivo,
        mono = mono,
    )
}

/** Material typography mapped onto [VaultType] (dialogs, menus, text fields). */
fun VaultType.toMaterial(): Typography =
    Typography(
        displayLarge = displayXl,
        displayMedium = displayL,
        displaySmall = displayL,
        headlineLarge = displayL,
        headlineMedium = displayL.copy(fontSize = 22.sp, lineHeight = 28.sp),
        headlineSmall = displayM,
        titleLarge = displayM,
        titleMedium = title,
        titleSmall = label,
        bodyLarge = body,
        bodyMedium = caption.copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = caption,
        labelLarge = bodyStrong,
        labelMedium = label,
        labelSmall = small,
    )

private fun style(
    family: FontFamily,
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    tracking: TextUnit = 0.sp,
    tabular: Boolean = false,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking,
    fontFeatureSettings = if (tabular) "tnum" else null,
)
