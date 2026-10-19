package com.thevault.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import thevault.composeapp.generated.resources.Res
import thevault.composeapp.generated.resources.inter_bold
import thevault.composeapp.generated.resources.inter_medium
import thevault.composeapp.generated.resources.inter_regular
import thevault.composeapp.generated.resources.inter_semibold
import thevault.composeapp.generated.resources.montserrat_bold
import thevault.composeapp.generated.resources.montserrat_extrabold
import thevault.composeapp.generated.resources.montserrat_semibold

/** Montserrat — titles and key figures. */
@Composable
fun montserratFamily(): FontFamily =
    FontFamily(
        Font(Res.font.montserrat_semibold, FontWeight.SemiBold),
        Font(Res.font.montserrat_bold, FontWeight.Bold),
        Font(Res.font.montserrat_extrabold, FontWeight.ExtraBold),
    )

/** Inter — body text and labels. */
@Composable
fun interFamily(): FontFamily =
    FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold),
    )

/**
 * Vault typography scale — see design/DESIGN_SYSTEM.md §3.
 *
 * - display / headline: Montserrat
 * - title / body / label: Inter
 * - labelMedium / labelSmall are meant to be rendered UPPERCASE.
 */
@Composable
fun vaultTypography(): Typography {
    val display = montserratFamily()
    val text = interFamily()
    return Typography(
        displayLarge = style(display, FontWeight.ExtraBold, 32, 40, -0.02),
        displayMedium = style(display, FontWeight.ExtraBold, 28, 36, -0.02),
        displaySmall = style(display, FontWeight.ExtraBold, 26, 32, -0.01),
        headlineLarge = style(display, FontWeight.Bold, 28, 36),
        headlineMedium = style(display, FontWeight.Bold, 24, 32),
        headlineSmall = style(display, FontWeight.SemiBold, 20, 28),
        titleLarge = style(text, FontWeight.SemiBold, 18, 26),
        titleMedium = style(text, FontWeight.SemiBold, 16, 24),
        titleSmall = style(text, FontWeight.SemiBold, 14, 20),
        bodyLarge = style(text, FontWeight.Normal, 16, 24),
        bodyMedium = style(text, FontWeight.Normal, 14, 20),
        bodySmall = style(text, FontWeight.Normal, 12, 16),
        labelLarge = style(text, FontWeight.SemiBold, 14, 20),
        labelMedium = style(text, FontWeight.Bold, 12, 16, 0.05),
        labelSmall = style(text, FontWeight.Bold, 10, 14, 0.1),
    )
}

/** Builds a text style from a size / line height in sp and a letter spacing in em. */
private fun style(
    family: FontFamily,
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    trackingEm: Double = 0.0,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = trackingEm.em,
)

/** Tabular figures for amounts and counters. */
val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")
