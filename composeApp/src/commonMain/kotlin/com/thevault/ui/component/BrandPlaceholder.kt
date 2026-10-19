package com.thevault.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.thevault.ui.theme.BrandNintendo
import com.thevault.ui.theme.BrandPlayStation
import com.thevault.ui.theme.BrandXbox
import com.thevault.ui.theme.OnSurface
import com.thevault.ui.theme.OnSurfaceVariant
import com.thevault.ui.theme.SurfaceContainerHighest

private val xboxPattern = Regex("xbox|microsoft", RegexOption.IGNORE_CASE)
private val nintendoPattern =
    Regex("""nintendo|\bnes\b|snes|n64|gamecube|wii|switch|game ?boy|gba|\b3?ds\b""", RegexOption.IGNORE_CASE)
private val playStationPattern =
    Regex("""playstation|sony|\bps[1-5p]?\b|psp|vita""", RegexOption.IGNORE_CASE)

/**
 * Brand background color for a platform, or null when unknown.
 * Same detection rules as `BrandPlaceholder.tsx` (design/DESIGN_SYSTEM.md §2).
 */
fun brandColorFor(platform: String): Color? =
    when {
        xboxPattern.containsMatchIn(platform) -> BrandXbox
        playStationPattern.containsMatchIn(platform) -> BrandPlayStation
        nintendoPattern.containsMatchIn(platform) -> BrandNintendo
        else -> null
    }

/**
 * Fallback artwork when an item has no cover: manufacturer logo in white on the brand color.
 *
 * @param platform Platform or manufacturer name used for detection.
 * @param modifier Modifier applied to the placeholder (size it from the caller).
 */
@Composable
fun BrandPlaceholder(
    platform: String,
    modifier: Modifier = Modifier,
) {
    val brand = brandColorFor(platform)
    BoxWithConstraints(
        modifier = modifier.background(brand ?: SurfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        val iconSize = minOf(this.maxWidth, this.maxHeight) * 0.5f
        Box(contentAlignment = Alignment.Center) {
            PlatformIcon(
                platformName = platform,
                manufacturer = platform,
                size = iconSize,
                tint = if (brand != null) OnSurface else OnSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}
