package com.thevault.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thevault.ui.theme.Error
import com.thevault.ui.theme.OnPrimary
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OutlineStrong
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.SurfaceContainerHigh
import com.thevault.ui.theme.VaultRadius

/** Button variants. */
enum class VaultButtonVariant {
    /** Filled violet — the single action color. */
    Primary,

    /** Outlined, transparent background (« Vendu / prêté »). */
    Ghost,

    /** Tonal « Carte haute » background. */
    Subtle,

    /** Destructive action. */
    Danger,
}

/**
 * Button heights: 52dp is the design standard (« Boutons · 52 px, rayon 14 »).
 *
 * @property height Button height.
 * @property horizontalPadding Horizontal content padding.
 * @property iconSize Leading icon size.
 */
enum class VaultButtonSize(
    val height: Dp,
    val horizontalPadding: Dp,
    val iconSize: Dp,
) {
    /** 40dp — inline actions. */
    Small(40.dp, 14.dp, 16.dp),

    /** 44dp — card actions. */
    Medium(44.dp, 14.dp, 18.dp),

    /** 52dp — screen actions. */
    Large(52.dp, 20.dp, 20.dp),
}

/**
 * Vault button.
 *
 * @param text Label.
 * @param onClick Click handler.
 * @param modifier Modifier applied to the button.
 * @param variant Visual variant.
 * @param size Height.
 * @param leadingIcon Optional leading icon.
 * @param enabled Whether the button is enabled.
 */
@Composable
fun VaultButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: VaultButtonVariant = VaultButtonVariant.Primary,
    size: VaultButtonSize = VaultButtonSize.Large,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val (container, content) = variantColors(variant)
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(size.height),
        shape = if (size == VaultButtonSize.Large) VaultRadius.button else VaultRadius.lg,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = container,
                contentColor = content,
                disabledContainerColor = container.copy(alpha = 0.4f),
                disabledContentColor = content.copy(alpha = 0.4f),
            ),
        border = variantBorder(variant),
        contentPadding = PaddingValues(horizontal = size.horizontalPadding),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(size.iconSize))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

private fun variantColors(variant: VaultButtonVariant): Pair<Color, Color> =
    when (variant) {
        VaultButtonVariant.Primary -> Primary to OnPrimary
        VaultButtonVariant.Ghost -> Color.Transparent to OnSurface
        VaultButtonVariant.Subtle -> SurfaceContainerHigh to OnSurface
        VaultButtonVariant.Danger -> Error.copy(alpha = 0.15f) to Error
    }

private fun variantBorder(variant: VaultButtonVariant): BorderStroke? =
    when (variant) {
        VaultButtonVariant.Ghost -> BorderStroke(1.dp, OutlineStrong)
        VaultButtonVariant.Danger -> BorderStroke(1.dp, Error.copy(alpha = 0.3f))
        else -> null
    }
