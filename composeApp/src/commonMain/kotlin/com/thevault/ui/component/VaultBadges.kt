package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thevault.domain.model.ItemCondition
import com.thevault.ui.theme.BadgeColors
import com.thevault.ui.theme.FormatBadgeColors
import com.thevault.ui.theme.OnPrimary
import com.thevault.ui.theme.OnSurfaceLight
import com.thevault.ui.theme.Outline
import com.thevault.ui.theme.Primary
import com.thevault.ui.theme.SurfaceContainerLow
import com.thevault.ui.theme.VaultRadius

/**
 * Small rounded badge (platform, format, status).
 *
 * @param text Label.
 * @param colors Badge colors.
 * @param modifier Modifier applied to the badge.
 * @param compact Smaller variant used on covers.
 */
@Composable
fun VaultBadge(
    text: String,
    colors: BadgeColors,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val shape = VaultRadius.badge
    Box(
        modifier =
            modifier
                .height(if (compact) 18.dp else 22.dp)
                .clip(shape)
                .background(colors.container)
                .then(if (colors.border != null) Modifier.border(1.dp, colors.border, shape) else Modifier)
                .padding(horizontal = if (compact) 6.dp else 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = colors.content,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (compact) 10.sp else 11.sp,
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.02.sp),
        )
    }
}

/**
 * Platform badge, tinted by manufacturer, with the short platform name.
 *
 * @param platform Platform name.
 * @param modifier Modifier applied to the badge.
 * @param compact Smaller variant used on covers.
 */
@Composable
fun PlatformBadge(
    platform: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (platform.isBlank()) return
    VaultBadge(shortPlatformName(platform), platformBadgeColors(platform), modifier, compact)
}

/**
 * Format badge (Loose, CIB, Neuf…).
 *
 * @param condition Item condition.
 * @param modifier Modifier applied to the badge.
 * @param compact Smaller variant used on covers.
 */
@Composable
fun FormatBadge(
    condition: ItemCondition,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    VaultBadge(condition.formatLabel, condition.formatColors, modifier, compact)
}

/**
 * Neutral text badge (region, genre…).
 *
 * @param text Label.
 * @param modifier Modifier applied to the badge.
 */
@Composable
fun NeutralBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    if (text.isBlank()) return
    VaultBadge(text, FormatBadgeColors.Other.copy(content = OnSurfaceLight), modifier)
}

/**
 * Filter chip: filled violet when selected, outlined otherwise, with an optional colored dot.
 *
 * @param text Label.
 * @param selected Whether the filter is active.
 * @param onClick Click handler.
 * @param modifier Modifier applied to the chip.
 * @param dot Optional dot color (platform tint).
 */
@Composable
fun VaultFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dot: Color? = null,
) {
    val shape = VaultRadius.full
    Row(
        modifier =
            modifier
                .height(36.dp)
                .clip(shape)
                .background(if (selected) Primary else SurfaceContainerLow)
                .then(if (selected) Modifier else Modifier.border(1.dp, Outline, shape))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (dot != null && !selected) Box(Modifier.size(8.dp).clip(VaultRadius.full).background(dot))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) OnPrimary else OnSurfaceLight,
            maxLines = 1,
        )
    }
}
