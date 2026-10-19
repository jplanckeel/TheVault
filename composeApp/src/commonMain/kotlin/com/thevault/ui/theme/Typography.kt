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
import thevault.composeapp.generated.resources.plusjakartasans_bold
import thevault.composeapp.generated.resources.plusjakartasans_extrabold
import thevault.composeapp.generated.resources.plusjakartasans_medium
import thevault.composeapp.generated.resources.plusjakartasans_regular
import thevault.composeapp.generated.resources.plusjakartasans_semibold

/** Plus Jakarta Sans — the only typeface of the app. */
@Composable
fun plusJakartaSans(): FontFamily =
    FontFamily(
        Font(Res.font.plusjakartasans_regular, FontWeight.Normal),
        Font(Res.font.plusjakartasans_medium, FontWeight.Medium),
        Font(Res.font.plusjakartasans_semibold, FontWeight.SemiBold),
        Font(Res.font.plusjakartasans_bold, FontWeight.Bold),
        Font(Res.font.plusjakartasans_extrabold, FontWeight.ExtraBold),
    )

/**
 * Typography scale — design/DESIGN_SYSTEM.md §3:
 * Montant 40/800, Titre 26/800, Section 18/800, Corps 15/700, Légende 12/500, Repère 11/700.
 * labelSmall (« Repère ») is meant to be rendered UPPERCASE.
 */
@Composable
fun vaultTypography(): Typography {
    val f = plusJakartaSans()
    return Typography(
        displayLarge = style(f, FontWeight.ExtraBold, 40, 46, -0.03),
        displayMedium = style(f, FontWeight.ExtraBold, 32, 38, -0.03),
        displaySmall = style(f, FontWeight.ExtraBold, 22, 28, -0.02),
        headlineLarge = style(f, FontWeight.ExtraBold, 26, 32, -0.02),
        headlineMedium = style(f, FontWeight.ExtraBold, 22, 28, -0.02),
        headlineSmall = style(f, FontWeight.ExtraBold, 18, 24),
        titleLarge = style(f, FontWeight.Bold, 18, 24),
        titleMedium = style(f, FontWeight.Bold, 15, 20),
        titleSmall = style(f, FontWeight.Bold, 13, 17),
        bodyLarge = style(f, FontWeight.Medium, 15, 22),
        bodyMedium = style(f, FontWeight.Medium, 14, 20),
        bodySmall = style(f, FontWeight.Medium, 12, 16),
        labelLarge = style(f, FontWeight.Bold, 15, 20),
        labelMedium = style(f, FontWeight.SemiBold, 13, 18),
        labelSmall = style(f, FontWeight.Bold, 11, 14, 0.08),
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
    fontFeatureSettings = "tnum",
)

/** Tabular figures for amounts and counters (already enabled on every style). */
val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")
