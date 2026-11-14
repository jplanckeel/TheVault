package com.thevault.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Raised molded surface: soft shadow on the light shell, faint outline on the dark one. */
@Composable
fun Modifier.molded(
    shape: Shape,
    color: androidx.compose.ui.graphics.Color = Vault.colors.panel,
): Modifier {
    val c = Vault.colors
    return this
        .shadow(if (c.isDark) 0.dp else 1.5.dp, shape, clip = false, ambientColor = c.moldedShadow, spotColor = c.moldedShadow)
        .clip(shape)
        .background(color)
        .border(1.dp, c.moldedBorder, shape)
}

/** Recessed surface (segmented tracks, search field, chart screen). */
@Composable
fun Modifier.well(shape: Shape): Modifier {
    val c = Vault.colors
    return this.clip(shape).background(c.well).border(1.dp, c.wellBorder, shape)
}

/** Indigo glow of the « A » buttons. */
@Composable
fun Modifier.faceShadow(shape: Shape): Modifier {
    val c = Vault.colors
    return this.shadow(10.dp, shape, clip = false, ambientColor = c.faceShadow, spotColor = c.faceShadow)
}

/** Shadow of floating elements (tab bar, sheet, toast). */
@Composable
fun Modifier.floatShadow(shape: Shape): Modifier {
    val c = Vault.colors
    return this.shadow(14.dp, shape, clip = false, ambientColor = c.floatShadow, spotColor = c.floatShadow)
}
