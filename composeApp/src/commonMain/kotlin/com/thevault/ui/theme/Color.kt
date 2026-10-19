package com.thevault.ui.theme

import androidx.compose.ui.graphics.Color

// Design « Nuit profonde, un seul violet » — source : design/DESIGN_SYSTEM.md
// (maquettes : « TheVault — Refonte mobile.html »).

// ─── Surfaces ───────────────────────────────────────────────────────────────

/** Page background (« Fond »). */
val Background = Color(0xFF060E20)

/** Alias of [Background] for Material `surface`. */
val Surface = Background

/** Bottom navigation bar and pressed rows. */
val SurfaceContainerLowest = Color(0xFF0B1530)

/** Cards, tiles, inputs (« Carte »). */
val SurfaceContainerLow = Color(0xFF0D1730)

/** Alias kept for Material `surfaceContainer`. */
val SurfaceContainer = SurfaceContainerLow

/** Nested blocks inside cards (« Carte haute »). */
val SurfaceContainerHigh = Color(0xFF141F38)

/** Progress tracks, neutral badges, row separators. */
val SurfaceContainerHighest = Color(0xFF1A2547)

/** Selected segment of a segmented control. */
val SurfaceBright = Color(0xFF22305A)

/** Alias kept for Material `surfaceVariant`. */
val SurfaceVariant = SurfaceContainerHighest

// ─── Action (the single violet) ─────────────────────────────────────────────

/** Action color: primary buttons, active tab, links (« Action »). */
val Primary = Color(0xFFBA9EFF)

/** Pressed / hover action color. */
val PrimaryDim = Color(0xFFCBB6FF)

/** Content on [Primary] (« Texte sur action »). */
val OnPrimary = Color(0xFF1C0A47)

/** Material `primaryContainer`: selected value tiles. */
val PrimaryContainer = Color(0xFF2A2152)

/** Content on [PrimaryContainer]. */
val OnPrimaryContainer = Color(0xFFCDBBFF)

/** Material `inversePrimary`. */
val InversePrimary = Color(0xFF6A3FE0)

/** Highlight banner background (missing purchase prices…). */
val HighlightContainer = Color(0xFF1B1840)

/** Highlight banner border. */
val HighlightBorder = Color(0xFF3A2F7A)

/** Highlight banner secondary text. */
val OnHighlightVariant = Color(0xFFB9B2E0)

/** Material `secondary`. */
val Secondary = Color(0xFFCDBBFF)

/** Content on [Secondary]. */
val OnSecondary = OnPrimary

/** Material `secondaryContainer`. */
val SecondaryContainer = PrimaryContainer

/** Content on [SecondaryContainer]. */
val OnSecondaryContainer = OnPrimaryContainer

/** Material `tertiary`. */
val Tertiary = Color(0xFF7FE8CB)

/** Content on [Tertiary]. */
val OnTertiary = Color(0xFF0B2B25)

/** Material `tertiaryContainer`. */
val TertiaryContainer = Color(0xFF123B36)

/** Content on [TertiaryContainer]. */
val OnTertiaryContainer = Tertiary

// ─── Text & outlines ────────────────────────────────────────────────────────

/** Main text (« Texte ») — never pure white. */
val OnSurface = Color(0xFFDEE5FF)

/** Content on [Background]. */
val OnBackground = OnSurface

/** Secondary text (« Texte doux »). */
val OnSurfaceVariant = Color(0xFF9AA3C2)

/** Eyebrows, tab labels, field keys. */
val OnSurfaceMuted = Color(0xFF8E97B8)

/** Text on neutral badges and covers. */
val OnSurfaceLight = Color(0xFFC9D1F0)

/** Material `inverseSurface`. */
val InverseSurface = OnSurface

/** Content on [InverseSurface]. */
val InverseOnSurface = Background

/** Outline of chips, covers and inputs. */
val Outline = Color(0xFF26335E)

/** Card border (« Bordure »). */
val OutlineVariant = Color(0xFF1E2A4D)

/** Border of outlined (secondary) buttons. */
val OutlineStrong = Color(0xFF2E3B66)

// ─── Semantic ───────────────────────────────────────────────────────────────

/** Gain over the purchase price (« Plus-value »). */
val Success = Color(0xFF6FE3C1)

/** Loss against the purchase price (« Moins-value »). */
val Error = Color(0xFFFF9F7F)

/** Warnings. */
val Warning = Color(0xFFFFC58F)

/** Informational accents. */
val Info = Color(0xFFB5CBFF)

/** Content on [Error]. */
val OnError = Color(0xFF3A1208)

/** Destructive container. */
val ErrorContainer = Color(0xFF3A1820)

/** Content on [ErrorContainer]. */
val OnErrorContainer = Color(0xFFFFB3B7)

// ─── Composite ──────────────────────────────────────────────────────────────

/** Default card border. */
val CardBorderColor = OutlineVariant

/** Bottom overlay on covers. */
val CoverScrimColor = Color.Black.copy(alpha = 0.55f)
