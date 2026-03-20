package com.thevault.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Surfaces ───────────────────────────────────────────────────────────────

/** Level 0 — Base background of the Vault */
val Surface = Color(0xFF060E20)

/** Darkest surface, used for sunken input fields */
val SurfaceContainerLowest = Color(0xFF000000)

/** Level 1 — Major content sections */
val SurfaceContainerLow = Color(0xFF091328)

/** Level 2 — Interactive cards and modules */
val SurfaceContainer = Color(0xFF0F1930)

/** Level 2+ — Elevated cards */
val SurfaceContainerHigh = Color(0xFF141F38)

/** Level 3 — Floating pop-overs and dialogs */
val SurfaceContainerHighest = Color(0xFF192540)

/** Glassmorphic navigation bar background */
val SurfaceBright = Color(0xFF1F2B49)

val SurfaceDim = Color(0xFF060E20)
val SurfaceVariant = Color(0xFF192540)

/** Radial aura glow tint behind featured game titles */
val SurfaceTint = Color(0xFFBA9EFF)

// ─── Background ─────────────────────────────────────────────────────────────
val Background = Color(0xFF060E20)
val OnBackground = Color(0xFFDEE5FF)

// ─── Primary ────────────────────────────────────────────────────────────────
/** Violet accent — main brand color */
val Primary = Color(0xFFBA9EFF)
val PrimaryContainer = Color(0xFFAE8DFF)

/** Start color of the CTA gradient */
val PrimaryDim = Color(0xFF8455EF)
val PrimaryFixed = Color(0xFFAE8DFF)
val PrimaryFixedDim = Color(0xFFA27CFF)
val OnPrimary = Color(0xFF39008C)
val OnPrimaryContainer = Color(0xFF2B006E)
val OnPrimaryFixed = Color(0xFF000000)
val OnPrimaryFixedVariant = Color(0xFF370086)
val InversePrimary = Color(0xFF6E3BD7)

// ─── Secondary ──────────────────────────────────────────────────────────────
val Secondary = Color(0xFFC7C3FE)

/** Background of Vault status chips */
val SecondaryContainer = Color(0xFF444173)
val SecondaryDim = Color(0xFFB9B6EF)
val SecondaryFixed = Color(0xFFD0CCFF)
val SecondaryFixedDim = Color(0xFFC2BEF8)
val OnSecondary = Color(0xFF3F3C6E)

/** Text on Vault status chips */
val OnSecondaryContainer = Color(0xFFCFCBFF)
val OnSecondaryFixed = Color(0xFF312E5F)
val OnSecondaryFixedVariant = Color(0xFF4E4B7D)

// ─── Tertiary ───────────────────────────────────────────────────────────────
/** End color of the CTA gradient — used sparingly on critical heat nodes */
val Tertiary = Color(0xFFEC63FF)
val TertiaryContainer = Color(0xFFDE4BF4)
val TertiaryDim = Color(0xFFEC63FF)
val TertiaryFixed = Color(0xFFF487FF)
val TertiaryFixedDim = Color(0xFFEF6EFF)
val OnTertiary = Color(0xFF3D0047)
val OnTertiaryContainer = Color(0xFF19001E)
val OnTertiaryFixed = Color(0xFF300038)
val OnTertiaryFixedVariant = Color(0xFF660075)

// ─── On Surface ─────────────────────────────────────────────────────────────
/** Main text color — NEVER use pure white (#FFFFFF) */
val OnSurface = Color(0xFFDEE5FF)

/** Secondary metadata text */
val OnSurfaceVariant = Color(0xFFA3AAC4)
val InverseOnSurface = Color(0xFF4D556B)
val InverseSurface = Color(0xFFFAF8FF)

// ─── Outlines ────────────────────────────────────────────────────────────────
val Outline = Color(0xFF6D758C)

/** Used for ghost borders at 15% opacity */
val OutlineVariant = Color(0xFF40485D)

// ─── Error ───────────────────────────────────────────────────────────────────
val Error = Color(0xFFFF6E84)
val ErrorContainer = Color(0xFFA70138)
val ErrorDim = Color(0xFFD73357)
val OnError = Color(0xFF490013)
val OnErrorContainer = Color(0xFFFFB2B9)

// ─── Composite / Semantic ────────────────────────────────────────────────────
/** CTA gradient colors (135°): primary_dim → tertiary */
val CtaGradientColors = listOf(PrimaryDim, Tertiary)

/** Neon wash shadow for floating modals */
val NeonWashShadowColor = Color(0xFF8B5CF6).copy(alpha = 0.08f)

/** Aura glow effect behind featured game titles */
val AuraGlowColor = SurfaceTint.copy(alpha = 0.30f)

/** Ghost border color (apply at 15% opacity) */
val GhostBorderColor = OutlineVariant.copy(alpha = 0.15f)
