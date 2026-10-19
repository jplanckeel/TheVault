package com.thevault.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Vault radius scale — mirrors `--radius-*` in web/src/index.css.
 * RULE: no corner below 8dp, except pills.
 */
object VaultRadius {
    /** 8dp — square chips, small icon buttons. */
    val md = RoundedCornerShape(8.dp)

    /** 12dp — buttons, inputs, list rows. */
    val lg = RoundedCornerShape(12.dp)

    /** 16dp — standard cards. */
    val xl = RoundedCornerShape(16.dp)

    /** 24dp — cover cards, KPI cards, sections. */
    val xxl = RoundedCornerShape(24.dp)

    /** Pill — chips, avatars. */
    val full = RoundedCornerShape(50)
}

/** Material shapes mapped onto [VaultRadius]. */
val VaultShapes =
    Shapes(
        extraSmall = VaultRadius.md,
        small = VaultRadius.md,
        medium = VaultRadius.lg,
        large = VaultRadius.xl,
        extraLarge = VaultRadius.xxl,
    )

/** Pill shape — chips and avatars. */
val PillShape = VaultRadius.full

/** Default card shape. */
val CardShape = VaultRadius.xl

/** Button shape. */
val CtaButtonShape = VaultRadius.lg

/** Modal / bottom sheet shape. */
val ModalShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
