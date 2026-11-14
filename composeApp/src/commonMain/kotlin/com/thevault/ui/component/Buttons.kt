@file:Suppress("MagicNumber") // Design tokens and geometry of the console mockups.

package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thevault.ui.theme.Vault
import com.thevault.ui.theme.VaultRadius
import com.thevault.ui.theme.faceShadow
import com.thevault.ui.theme.molded

/** Button variants. */
enum class ButtonVariant {
    /** Indigo « A » face with a glow — the main action. */
    Primary,

    /** Lavender — the secondary action. */
    Secondary,

    /** Transparent with indigo text. */
    Ghost,

    /** Destructive. */
    Danger,
}

/**
 * Button heights.
 *
 * @property height Height.
 * @property padding Horizontal padding.
 * @property fontSize Label size.
 */
enum class ButtonSize(
    val height: Dp,
    val padding: Dp,
    val fontSize: TextUnit,
) {
    /** 36dp. */
    Small(36.dp, 16.dp, 13.sp),

    /** 50dp. */
    Medium(50.dp, 24.dp, 15.sp),

    /** 56dp. */
    Large(56.dp, 28.dp, 16.sp),
}

/** Scales a pressable element down while pressed (« clic de touche »). */
@Composable
fun Modifier.pressable(
    interaction: MutableInteractionSource,
    pressedScale: Float = 0.96f,
): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    return this.scale(if (pressed) pressedScale else 1f)
}

/**
 * Pill button.
 *
 * @param text Label.
 * @param onClick Callback.
 * @param modifier Modifier.
 * @param variant Visual variant.
 * @param size Height.
 * @param icon Optional leading icon.
 * @param enabled Whether the button is enabled.
 */
@Composable
fun VaultButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Medium,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = Vault.colors
    val (container, content) =
        when {
            !enabled -> c.well to c.inkMuted
            variant == ButtonVariant.Primary -> c.indigo to c.onIndigo
            variant == ButtonVariant.Secondary -> c.lavender to c.onLavender
            variant == ButtonVariant.Danger -> c.loss.copy(alpha = 0.12f) to c.loss
            else -> Color.Transparent to c.indigoInk
        }
    val interaction = remember { MutableInteractionSource() }
    val shape = VaultRadius.pill
    Row(
        modifier
            .pressable(interaction)
            .then(if (enabled && variant == ButtonVariant.Primary) Modifier.faceShadow(shape) else Modifier)
            .height(size.height)
            .clip(shape)
            .background(container)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = size.padding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        Text(text, style = Vault.type.bodyStrong.copy(fontSize = size.fontSize), color = content, maxLines = 1)
    }
}

/** Icon button variants. */
enum class IconButtonVariant {
    /** Raised panel key. */
    Key,

    /** Transparent. */
    Ghost,

    /** Lavender. */
    Lavender,
}

/**
 * Square icon button (44dp, radius 14).
 *
 * @param icon Icon.
 * @param contentDescription Accessibility label.
 * @param onClick Callback.
 * @param modifier Modifier.
 * @param variant Visual variant.
 * @param size Button size.
 */
@Composable
fun KeyButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: IconButtonVariant = IconButtonVariant.Key,
    size: Dp = 44.dp,
) {
    val c = Vault.colors
    val shape = if (size < 40.dp) VaultRadius.iconSm else VaultRadius.md
    val interaction = remember { MutableInteractionSource() }
    val surface =
        when (variant) {
            IconButtonVariant.Key -> Modifier.molded(shape)
            IconButtonVariant.Ghost -> Modifier.clip(shape)
            IconButtonVariant.Lavender -> Modifier.clip(shape).background(c.lavender)
        }
    Box(
        modifier
            .pressable(interaction, 0.92f)
            .size(size)
            .then(surface)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val tint =
            if (variant ==
                IconButtonVariant.Ghost
            ) {
                c.inkMuted
            } else if (variant == IconButtonVariant.Lavender) {
                c.onLavender
            } else {
                c.ink
            }
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/**
 * Round indigo « A » button (the central « + » of the tab bar).
 *
 * @param icon Icon.
 * @param contentDescription Accessibility label.
 * @param onClick Callback.
 * @param modifier Modifier.
 * @param size Diameter.
 */
@Composable
fun FaceButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    val c = Vault.colors
    val interaction = remember { MutableInteractionSource() }
    val shape = VaultRadius.pill
    Box(
        modifier
            .pressable(interaction, 0.92f)
            .size(size)
            .faceShadow(shape)
            .clip(shape)
            .background(c.indigo)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = c.onIndigo, modifier = Modifier.size(26.dp))
    }
}
