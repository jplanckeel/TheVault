package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thevault.ui.theme.CardBorderColor
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceMuted
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.SurfaceBright
import com.thevault.ui.theme.SurfaceContainerHighest
import com.thevault.ui.theme.SurfaceContainerLow
import com.thevault.ui.theme.VaultRadius

/**
 * Small uppercase marker (« Repère »), e.g. "VALEUR ESTIMÉE".
 *
 * @param text Marker text.
 * @param modifier Modifier applied to the text.
 */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted, modifier = modifier)
}

/**
 * Segmented control (Jeux · Consoles · Accessoires, Collection · Wishlist…).
 *
 * @param T Option type.
 * @param options Options in display order.
 * @param selected Selected option.
 * @param label Label of an option.
 * @param onSelect Selection callback.
 * @param modifier Modifier applied to the control.
 */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(VaultRadius.button)
            .background(SurfaceContainerLow)
            .border(1.dp, CardBorderColor, VaultRadius.button)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val on = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(VaultRadius.md)
                    .background(if (on) SurfaceBright else SurfaceContainerLow)
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), style = MaterialTheme.typography.labelMedium, color = if (on) OnSurface else OnSurfaceVariant)
            }
        }
    }
}

/**
 * Square icon button (44dp, radius 14) used in headers.
 *
 * @param icon Icon.
 * @param contentDescription Accessibility label.
 * @param onClick Click handler.
 * @param modifier Modifier applied to the button.
 */
@Composable
fun IconSquareButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(44.dp)
            .clip(VaultRadius.button)
            .background(SurfaceContainerLow)
            .border(1.dp, CardBorderColor, VaultRadius.button)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = OnSurface, modifier = Modifier.size(22.dp))
    }
}

/**
 * Root screen header: title (26/800) and an optional trailing summary or action.
 *
 * @param title Title.
 * @param modifier Modifier applied to the header.
 * @param eyebrow Optional marker above the title.
 * @param summary Optional right-aligned summary ("414 jeux · 11 859 €").
 * @param action Optional trailing action.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    summary: String? = null,
    action: @Composable (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) Eyebrow(eyebrow, Modifier.padding(bottom = 4.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge, color = OnSurface)
        }
        if (summary != null) Text(summary, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
        action?.invoke()
    }
}

/**
 * Section title (18/800) with an optional text action on the right ("Tout voir").
 *
 * @param title Title.
 * @param modifier Modifier applied to the header.
 * @param actionLabel Optional action label.
 * @param onAction Action click handler.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = OnSurface, modifier = Modifier.weight(1f))
        if (actionLabel != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = Primary,
                modifier = Modifier.clip(VaultRadius.md).clickable(onClick = onAction).padding(6.dp),
            )
        }
    }
}

/**
 * Progress track (4dp in lists, 6dp in stats).
 *
 * @param fraction Filled fraction, between 0 and 1.
 * @param modifier Modifier applied to the track.
 * @param height Track height.
 */
@Composable
fun ProgressTrack(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(VaultRadius.full)
            .background(SurfaceContainerHighest),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(VaultRadius.full)
                .background(Primary),
        )
    }
}
