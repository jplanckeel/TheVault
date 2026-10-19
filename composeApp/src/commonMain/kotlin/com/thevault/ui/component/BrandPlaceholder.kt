package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.thevault.ui.theme.OnSurfaceLight

/**
 * Placeholder when an item has no cover: dark manufacturer tint with a faded logo.
 *
 * @param platform Platform or manufacturer name.
 * @param modifier Modifier applied to the placeholder (size it from the caller).
 */
@Composable
fun BrandPlaceholder(
    platform: String,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.background(coverTint(platform)), contentAlignment = Alignment.Center) {
        val iconSize = minOf(this.maxWidth, this.maxHeight) * 0.4f
        Box(contentAlignment = Alignment.Center) {
            PlatformIcon(
                platformName = platform,
                manufacturer = platform,
                size = iconSize,
                tint = OnSurfaceLight.copy(alpha = 0.18f),
            )
        }
    }
}
