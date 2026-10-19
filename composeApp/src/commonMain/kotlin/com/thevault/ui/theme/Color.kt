package com.thevault.ui.theme

import androidx.compose.ui.graphics.Color

// Design system « Premium Security » — source : design/DESIGN_SYSTEM.md
// Toute valeur modifiée ici doit l'être aussi dans web/src/index.css.

// ─── Surfaces ───────────────────────────────────────────────────────────────

/** Page background (`vault-bg`). */
val Background = Color(0xFF0F0F12)

/** Alias of [Background] for Material `surface`. */
val Surface = Background

/** Sunken areas, sidebar (`vault-surface-lowest`). */
val SurfaceContainerLowest = Color(0xFF0E0E11)

/** Default card surface (`vault-surface-low`). */
val SurfaceContainerLow = Color(0xFF1B1B1E)

/** Grouped content inside cards (`vault-surface`). */
val SurfaceContainer = Color(0xFF1F1F22)

/** Inputs and interactive elements (`vault-surface-high`). */
val SurfaceContainerHigh = Color(0xFF2A2A2D)

/** Pop-overs, neutral chips (`vault-surface-highest`). */
val SurfaceContainerHighest = Color(0xFF353438)

/** Glass navigation bar base (`vault-bright`), used at 60 % opacity. */
val SurfaceBright = Color(0xFF39393C)

/** Alias kept for Material `surfaceVariant`. */
val SurfaceVariant = SurfaceContainerHighest

// ─── Accent ─────────────────────────────────────────────────────────────────

/** Lavender accent — the single brand color (`vault-primary`). */
val Primary = Color(0xFFD8B9FF)

/** Hover / pressed primary (`vault-primary-dim`). */
val PrimaryDim = Color(0xFFAE72FF)

/** Deep primary used for avatar gradients (`vault-primary-deep`). */
val PrimaryDeep = Color(0xFF7E28DF)

/** Content on a [Primary] background. */
val OnPrimary = Color(0xFF450086)

/** Material `primaryContainer`, mapped to [PrimaryDim]. */
val PrimaryContainer = PrimaryDim

/** Content on [PrimaryContainer]. */
val OnPrimaryContainer = Color(0xFF3C0076)

/** Material `inversePrimary`, mapped to [PrimaryDeep]. */
val InversePrimary = PrimaryDeep

/** Secondary accent (`vault-secondary`). */
val Secondary = Color(0xFFE2B5FF)

/** Content on [Secondary]. */
val OnSecondary = Color(0xFF451D61)

/** Secondary chips background (`vault-secondary-container`). */
val SecondaryContainer = Color(0xFF60377C)

/** Content on [SecondaryContainer]. */
val OnSecondaryContainer = Color(0xFFD5A6F4)

/** Tertiary accent (`vault-tertiary`). */
val Tertiary = Color(0xFFCFC0E5)

/** Content on [Tertiary]. */
val OnTertiary = Color(0xFF362B48)

/** Material `tertiaryContainer`. */
val TertiaryContainer = Color(0xFF988BAD)

/** Content on [TertiaryContainer]. */
val OnTertiaryContainer = Color(0xFF2F2541)

// ─── Text & outlines ────────────────────────────────────────────────────────

/** Main text color — never use pure white. */
val OnSurface = Color(0xFFE4E1E6)

/** Content on [Background]. */
val OnBackground = OnSurface

/** Secondary text and metadata. */
val OnSurfaceVariant = Color(0xFFCEC2D7)

/** Material `inverseSurface`. */
val InverseSurface = OnSurface

/** Content on [InverseSurface]. */
val InverseOnSurface = Color(0xFF303033)

/** Strong outline (`vault-outline`). */
val Outline = Color(0xFF978DA0)

/** Discreet borders, applied at 30 % opacity. */
val OutlineVariant = Color(0xFF4C4355)

// ─── Semantic ───────────────────────────────────────────────────────────────

/** Positive values and states (`vault-success`). */
val Success = Color(0xFF34D399)

/** Warnings (`vault-warning`). */
val Warning = Color(0xFFFBBF24)

/** Informational accents (`vault-info`). */
val Info = Color(0xFF60A5FA)

/** Errors and destructive actions (`vault-error`). */
val Error = Color(0xFFFFB4AB)

/** Content on [Error]. */
val OnError = Color(0xFF690005)

/** Error container (`vault-error-container`). */
val ErrorContainer = Color(0xFF93000A)

/** Content on [ErrorContainer]. */
val OnErrorContainer = Color(0xFFFFDAD6)

// ─── Price per condition ────────────────────────────────────────────────────

/** Price curve color for loose items. */
val PriceLoose = Info

/** Price curve color for complete-in-box items. */
val PriceCib = Primary

/** Price curve color for new / sealed items. */
val PriceNew = Success

// ─── Brand placeholders ─────────────────────────────────────────────────────

/** Xbox placeholder background. */
val BrandXbox = Color(0xFF107C10)

/** Nintendo placeholder background. */
val BrandNintendo = Color(0xFFE60012)

/** PlayStation placeholder background. */
val BrandPlayStation = Color(0xFF0070CC)

// ─── Composite ──────────────────────────────────────────────────────────────

/** Default card border (`outline-variant` at 30 %). */
val CardBorderColor = OutlineVariant.copy(alpha = 0.3f)

/** Bottom overlay on covers (black 85 % → transparent). */
val CoverScrimColor = Color.Black.copy(alpha = 0.85f)
