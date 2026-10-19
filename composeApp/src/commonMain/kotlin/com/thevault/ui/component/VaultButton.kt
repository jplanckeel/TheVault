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
import com.thevault.ui.theme.OutlineVariant
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.SurfaceContainerHigh
import com.thevault.ui.theme.VaultRadius

/** Visual variants shared with `Button.tsx`. */
enum class VaultButtonVariant {
    /** Filled lavender — main call to action. */
    Primary,

    /** Outlined, transparent background. */
    Ghost,

    /** Tonal `surface-high` background. */
    Subtle,

    /** Destructive action. */
    Danger,
}

/**
 * Height scale shared with `Button.tsx` (32 / 40 / 48).
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
    /** 32dp — dense toolbars. */
    Small(32.dp, 12.dp, 16.dp),

    /** 40dp — default. */
    Medium(40.dp, 16.dp, 18.dp),

    /** 48dp — form submit buttons. */
    Large(48.dp, 24.dp, 20.dp),
}

/**
 * Vault button — filled lavender (primary), outlined (ghost), tonal (subtle) or destructive (danger).
 *
 * @param text Button label.
 * @param onClick Click handler.
 * @param modifier Modifier applied to the button.
 * @param variant Visual variant.
 * @param size Height scale.
 * @param leadingIcon Optional icon displayed before the label.
 * @param enabled Whether the button is enabled.
 */
@Composable
fun VaultButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: VaultButtonVariant = VaultButtonVariant.Primary,
    size: VaultButtonSize = VaultButtonSize.Medium,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val (container, content) = variantColors(variant)
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(size.height),
        shape = VaultRadius.lg,
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
        Text(text, style = MaterialTheme.typography.labelLarge)
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
        VaultButtonVariant.Ghost -> BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.6f))
        VaultButtonVariant.Danger -> BorderStroke(1.dp, Error.copy(alpha = 0.3f))
        else -> null
    }
