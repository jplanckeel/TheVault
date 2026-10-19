package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thevault.ui.theme.VaultRadius

/**
 * Brand logo of a platform (PlayStation, Xbox, Nintendo, Sega…) in a square tinted
 * with the manufacturer colors.
 *
 * @param platform Platform name.
 * @param modifier Modifier applied to the square.
 * @param size Square size; the logo takes about half of it.
 */
@Composable
fun PlatformLogo(
    platform: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    val colors = platformBadgeColors(platform)
    val border = colors.border ?: colors.container
    Box(
        modifier
            .size(size)
            .clip(VaultRadius.button)
            .background(coverTint(platform))
            .border(1.dp, border, VaultRadius.button),
        contentAlignment = Alignment.Center,
    ) {
        PlatformIcon(platform, manufacturerOf(platform).label, size = size * LOGO_RATIO, tint = colors.content)
    }
}

private const val LOGO_RATIO = 0.52f
