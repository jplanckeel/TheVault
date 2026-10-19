package com.thevault.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Material shapes mapped onto [VaultRadius]. */
val VaultShapes =
    Shapes(
        extraSmall = VaultRadius.badge,
        small = VaultRadius.md,
        medium = VaultRadius.button,
        large = VaultRadius.card,
        extraLarge = VaultRadius.xxl,
    )

/** Pill shape — chips. */
val PillShape = VaultRadius.full

/** Default card shape. */
val CardShape = VaultRadius.card

/** Button shape. */
val CtaButtonShape = VaultRadius.button

/** Modal / bottom sheet shape. */
val ModalShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
