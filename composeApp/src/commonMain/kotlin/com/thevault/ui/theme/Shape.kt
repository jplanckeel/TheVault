package com.thevault.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Vault shape system.
 * RULE: No corners below 8dp anywhere in the app. No 90° corners.
 */
val VaultShapes =
    Shapes(
        // Minimum 8dp — "DEFAULT" in the design system
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp), // CTAs
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

/** Pill shape — used exclusively for Vault Status Chips */
val PillShape = RoundedCornerShape(50)

/** Default card shape — 8dp corners */
val CardShape = RoundedCornerShape(8.dp)

/** CTA button shape — 12dp corners (md) */
val CtaButtonShape = RoundedCornerShape(12.dp)

/** Modal / bottom sheet shape */
val ModalShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
