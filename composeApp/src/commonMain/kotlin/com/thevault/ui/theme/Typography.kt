package com.thevault.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Plus Jakarta Sans font family.
 * TODO: Replace FontFamily.Default with the actual loaded font once resources
 *       are added to composeApp/src/commonMain/composeResources/font/.
 */
val PlusJakartaSans = FontFamily.Default // Will be replaced with actual font

/**
 * Vault typography scale.
 * Source: Stitch design system — "The Radiant Obsidian Vault"
 *
 * - Display: game titles, vault statistics (tight letter spacing)
 * - Headline: category headers (primary color accents)
 * - Body lg: main content workhorse
 * - Body md: secondary metadata (on_surface_variant color)
 * - Label md/sm: ALL-CAPS technical specs (PLATFORM, FPS…)
 */
val VaultTypography =
    Typography(
        // ─── Display — game titles, vault stats ──────────────────────────────────
        displayLarge =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 57.sp,
                lineHeight = 64.sp,
                letterSpacing = (-1.14).sp, // -0.02em
            ),
        displayMedium =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 45.sp,
                lineHeight = 52.sp,
                letterSpacing = (-0.9).sp, // -0.02em
            ),
        displaySmall =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = (-0.36).sp, // -0.01em
            ),
        // ─── Headline — section headers ───────────────────────────────────────────
        headlineLarge =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.32).sp, // -0.01em
            ),
        headlineMedium =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp,
            ),
        // ─── Title — card titles ──────────────────────────────────────────────────
        titleLarge =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.1.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        // ─── Body — main content ──────────────────────────────────────────────────
        bodyLarge =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
            ),
        // ─── Label — ALL-CAPS technical specs ────────────────────────────────────
        labelLarge =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.5.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.6.sp, // +0.05em for technical specs
            ),
        labelSmall =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
    )
