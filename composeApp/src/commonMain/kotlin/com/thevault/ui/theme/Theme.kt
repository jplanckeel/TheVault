package com.thevault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val VaultColorScheme =
    darkColorScheme(
        primary = Primary,
        onPrimary = OnPrimary,
        primaryContainer = PrimaryContainer,
        onPrimaryContainer = OnPrimaryContainer,
        inversePrimary = InversePrimary,
        secondary = Secondary,
        onSecondary = OnSecondary,
        secondaryContainer = SecondaryContainer,
        onSecondaryContainer = OnSecondaryContainer,
        tertiary = Tertiary,
        onTertiary = OnTertiary,
        tertiaryContainer = TertiaryContainer,
        onTertiaryContainer = OnTertiaryContainer,
        error = Error,
        onError = OnError,
        errorContainer = ErrorContainer,
        onErrorContainer = OnErrorContainer,
        background = Background,
        onBackground = OnBackground,
        surface = Surface,
        onSurface = OnSurface,
        onSurfaceVariant = OnSurfaceVariant,
        surfaceTint = Primary,
        inverseSurface = InverseSurface,
        inverseOnSurface = InverseOnSurface,
        outline = Outline,
        outlineVariant = OutlineVariant,
        surfaceContainerLowest = SurfaceContainerLowest,
        surfaceContainerLow = SurfaceContainerLow,
        surfaceContainer = SurfaceContainer,
        surfaceContainerHigh = SurfaceContainerHigh,
        surfaceContainerHighest = SurfaceContainerHighest,
        surfaceBright = SurfaceBright,
        surfaceDim = SurfaceContainerLowest,
        surfaceVariant = SurfaceVariant,
    )

/** CompositionLocal to expose [VaultSpacing] to all composable children. */
val LocalVaultSpacing = staticCompositionLocalOf { VaultSpacing }

/**
 * TheVault design system theme.
 *
 * Applies the "Premium Security" color scheme, typography and shapes shared
 * with the web app — see design/DESIGN_SYSTEM.md.
 *
 * @param content The composable content to display within the theme.
 */
@Composable
fun VaultTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalVaultSpacing provides VaultSpacing,
    ) {
        MaterialTheme(
            colorScheme = VaultColorScheme,
            typography = vaultTypography(),
            shapes = VaultShapes,
            content = content,
        )
    }
}
