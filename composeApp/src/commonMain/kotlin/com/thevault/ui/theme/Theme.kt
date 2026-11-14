package com.thevault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Shell chosen by the user.
 *
 * @property key Stored value.
 * @property label Settings label.
 */
enum class ThemeMode(
    val key: String,
    val label: String,
) {
    /** Follows the system dark mode. */
    System("system", "Automatique"),

    /** « Coque grise ». */
    Shell("shell", "Coque grise"),

    /** « Black Edition ». */
    Black("black", "Black Edition"),
    ;

    /** Companion helpers. */
    companion object {
        /** Setting key of the shell. */
        const val SETTING_KEY = "ui.theme"

        /** Mode of a stored value, [System] when unknown. */
        fun fromKey(key: String?): ThemeMode = entries.find { it.key == key } ?: System
    }
}

/** Colors of the current shell. */
val LocalVaultColors = staticCompositionLocalOf { ShellColors }

/** Text styles of the design. */
val LocalVaultType = staticCompositionLocalOf<VaultType> { error("VaultTheme is missing") }

/** Shortcut to the design tokens: `Vault.colors.ink`, `Vault.type.displayL`. */
object Vault {
    /** Colors of the current shell. */
    val colors: VaultColors
        @Composable @ReadOnlyComposable
        get() = LocalVaultColors.current

    /** Text styles. */
    val type: VaultType
        @Composable @ReadOnlyComposable
        get() = LocalVaultType.current
}

/**
 * TheVault theme: « Coque grise » or « Black Edition » shell, Archivo / IBM Plex type.
 *
 * @param mode Shell chosen by the user.
 * @param content Themed content.
 */
@Composable
fun VaultTheme(
    mode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val dark =
        when (mode) {
            ThemeMode.System -> isSystemInDarkTheme()
            ThemeMode.Shell -> false
            ThemeMode.Black -> true
        }
    val colors = if (dark) BlackColors else ShellColors
    val type = vaultType()
    CompositionLocalProvider(LocalVaultColors provides colors, LocalVaultType provides type) {
        MaterialTheme(colorScheme = colors.toMaterial(), typography = type.toMaterial(), content = content)
    }
}

/** Material colors for the few Material components kept (dialogs, menus, progress). */
private fun VaultColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = indigo,
        onPrimary = onIndigo,
        primaryContainer = lavender,
        onPrimaryContainer = onLavender,
        secondary = indigoInk,
        background = shell,
        onBackground = ink,
        surface = panel,
        onSurface = ink,
        surfaceVariant = well,
        onSurfaceVariant = inkMuted,
        surfaceContainerLowest = panel,
        surfaceContainerLow = panel,
        surfaceContainer = panel,
        surfaceContainerHigh = panel,
        surfaceContainerHighest = keycap,
        outline = lineStrong,
        outlineVariant = line,
        error = loss,
        scrim = scrim,
    )
}
