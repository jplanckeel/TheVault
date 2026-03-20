package com.thevault.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thevault.ui.theme.OnSurfaceVariant
import org.jetbrains.compose.resources.painterResource
import thevault.composeapp.generated.resources.Res
import thevault.composeapp.generated.resources.logo_playstation
import thevault.composeapp.generated.resources.logo_xbox

/**
 * Displays the appropriate brand icon for a given platform name and manufacturer.
 *
 * Sony/Microsoft → PNG resources (logo_playstation.png / logo_xbox.png).
 * Nintendo/Sega  → ImageVector drawn paths.
 * Fallback       → generic gamepad icon.
 *
 * @param platformName The platform name (e.g. "PlayStation 5").
 * @param manufacturer The manufacturer name (e.g. "Sony").
 * @param size Icon size in dp.
 * @param tint Icon tint color.
 */
@Composable
fun PlatformIcon(
    platformName: String,
    manufacturer: String,
    size: Dp = 28.dp,
    tint: Color = OnSurfaceVariant,
) {
    val key = manufacturer.lowercase()
    val platformKey = platformName.lowercase()

    val modifier = Modifier.size(size)

    when {
        key.contains("sony") || platformKey.contains("playstation") || platformKey.startsWith("ps") ->
            Image(
                painter = painterResource(Res.drawable.logo_playstation),
                contentDescription = platformName,
                modifier = modifier,
                colorFilter = ColorFilter.tint(tint),
            )

        key.contains("microsoft") || platformKey.contains("xbox") ->
            Image(
                painter = painterResource(Res.drawable.logo_xbox),
                contentDescription = platformName,
                modifier = modifier,
                colorFilter = ColorFilter.tint(tint),
            )

        key.contains("nintendo") || platformKey.contains("nintendo") ||
            platformKey.contains("game boy") || platformKey.contains("gameboy") ||
            platformKey.contains("wii") || platformKey.contains("switch") ||
            platformKey.contains("gamecube") || platformKey.contains("famicom") ->
            Icon(
                imageVector = BrandIcons.Nintendo(tint),
                contentDescription = platformName,
                modifier = modifier,
                tint = tint,
            )

        key.contains("sega") || platformKey.contains("sega") ||
            platformKey.contains("dreamcast") || platformKey.contains("saturn") ||
            platformKey.contains("mega drive") || platformKey.contains("genesis") ->
            Icon(
                imageVector = BrandIcons.Sega(tint),
                contentDescription = platformName,
                modifier = modifier,
                tint = tint,
            )

        else ->
            Icon(
                imageVector = BrandIcons.Gamepad(tint),
                contentDescription = platformName,
                modifier = modifier,
                tint = tint,
            )
    }
}
