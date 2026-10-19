> ⚠️ **Obsolète** — le thème « Radiant Obsidian Vault » n'est plus utilisé. La référence est `design/DESIGN_SYSTEM.md` (thème « Premium Security », commun au web et au mobile).

# TheVault — Compose Theme Reference
> Ce fichier décrit la structure des fichiers de thème à créer dans
> `composeApp/src/commonMain/kotlin/com/thevault/ui/theme/`
>
> **Source** : Stitch MCP — Project `10534585831624995760` "The Radiant Obsidian Vault"

---

## Structure des fichiers de thème

```
ui/theme/
├── Color.kt          # Toutes les couleurs du design system
├── Typography.kt     # Échelle typographique Plus Jakarta Sans
├── Shape.kt          # Coins et formes
├── Spacing.kt        # Tokens d'espacement
└── Theme.kt          # MaterialTheme + ColorScheme assemblé
```

---

## Color.kt

```kotlin
package com.thevault.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Surfaces ───────────────────────────────────────────────────────────────
val Surface = Color(0xFF060E20)
val SurfaceContainerLowest = Color(0xFF000000)
val SurfaceContainerLow = Color(0xFF091328)
val SurfaceContainer = Color(0xFF0F1930)
val SurfaceContainerHigh = Color(0xFF141F38)
val SurfaceContainerHighest = Color(0xFF192540)
val SurfaceBright = Color(0xFF1F2B49)
val SurfaceDim = Color(0xFF060E20)
val SurfaceVariant = Color(0xFF192540)
val SurfaceTint = Color(0xFFBA9EFF)

// ─── Background ─────────────────────────────────────────────────────────────
val Background = Color(0xFF060E20)
val OnBackground = Color(0xFFDEE5FF)

// ─── Primary ────────────────────────────────────────────────────────────────
val Primary = Color(0xFFBA9EFF)
val PrimaryContainer = Color(0xFFAE8DFF)
val PrimaryDim = Color(0xFF8455EF)
val PrimaryFixed = Color(0xFFAE8DFF)
val PrimaryFixedDim = Color(0xFFA27CFF)
val OnPrimary = Color(0xFF39008C)
val OnPrimaryContainer = Color(0xFF2B006E)
val OnPrimaryFixed = Color(0xFF000000)
val OnPrimaryFixedVariant = Color(0xFF370086)
val InversePrimary = Color(0xFF6E3BD7)

// ─── Secondary ──────────────────────────────────────────────────────────────
val Secondary = Color(0xFFC7C3FE)
val SecondaryContainer = Color(0xFF444173)
val SecondaryDim = Color(0xFFB9B6EF)
val SecondaryFixed = Color(0xFFD0CCFF)
val SecondaryFixedDim = Color(0xFFC2BEF8)
val OnSecondary = Color(0xFF3F3C6E)
val OnSecondaryContainer = Color(0xFFCFCBFF)
val OnSecondaryFixed = Color(0xFF312E5F)
val OnSecondaryFixedVariant = Color(0xFF4E4B7D)

// ─── Tertiary ───────────────────────────────────────────────────────────────
val Tertiary = Color(0xFFEC63FF)
val TertiaryContainer = Color(0xFFDE4BF4)
val TertiaryDim = Color(0xFFEC63FF)
val TertiaryFixed = Color(0xFFF487FF)
val TertiaryFixedDim = Color(0xFFEF6EFF)
val OnTertiary = Color(0xFF3D0047)
val OnTertiaryContainer = Color(0xFF19001E)
val OnTertiaryFixed = Color(0xFF300038)
val OnTertiaryFixedVariant = Color(0xFF660075)

// ─── Surface Text ────────────────────────────────────────────────────────────
val OnSurface = Color(0xFFDEE5FF)
val OnSurfaceVariant = Color(0xFFA3AAC4)
val InverseOnSurface = Color(0xFF4D556B)
val InverseSurface = Color(0xFFFAF8FF)

// ─── Outlines ────────────────────────────────────────────────────────────────
val Outline = Color(0xFF6D758C)
val OutlineVariant = Color(0xFF40485D)

// ─── Error ───────────────────────────────────────────────────────────────────
val Error = Color(0xFFFF6E84)
val ErrorContainer = Color(0xFFA70138)
val ErrorDim = Color(0xFFD73357)
val OnError = Color(0xFF490013)
val OnErrorContainer = Color(0xFFFFB2B9)

// ─── Gradients ───────────────────────────────────────────────────────────────
/** Gradient principal pour les CTAs (135°) */
val CtaGradientColors = listOf(PrimaryDim, Tertiary)

/** Glow ambient pour les modals flottantes */
val NeonWashShadowColor = Color(0xFF8B5CF6).copy(alpha = 0.08f)

/** Aura effect derrière les featured game titles */
val AuraGlowColor = SurfaceTint.copy(alpha = 0.30f)
```

---

## Typography.kt

```kotlin
package com.thevault.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Plus Jakarta Sans — chargé depuis les ressources ou Google Fonts
val PlusJakartaSans = FontFamily.Default // À remplacer par la vraie font

val VaultTypography = Typography(
    // Display — titres de jeux, stats vault
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.02).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.02).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.01).sp,
    ),
    // Headline — headers de catégorie
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.01).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    // Title — titres de cards
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    // Body — contenu principal
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    // Label — specs techniques ALL-CAPS
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.05.sp, // +0.05em pour les specs techniques
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)
```

---

## Shape.kt

```kotlin
package com.thevault.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val VaultShapes = Shapes(
    // Jamais de coins < 8dp dans TheVault
    extraSmall = RoundedCornerShape(8.dp),   // DEFAULT minimum
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),      // "md" pour les CTAs
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Pill shape — pour les Status Chips */
val PillShape = RoundedCornerShape(50)

/** Card shape — DEFAULT 8dp */
val CardShape = RoundedCornerShape(8.dp)

/** CTA Button shape — md 12dp */
val CtaButtonShape = RoundedCornerShape(12.dp)
```

---

## Spacing.kt

```kotlin
package com.thevault.ui.theme

import androidx.compose.ui.unit.dp

/** Échelle d'espacement — spacingScale = 2 (base 8dp) */
object VaultSpacing {
    val spacing1 = 4.dp
    val spacing2 = 8.dp
    val spacing3 = 12.dp
    val spacing4 = 16.dp
    val spacing5 = 20.dp
    val spacing6 = 24.dp
    val spacing8 = 32.dp
    val spacing10 = 40.dp
    val spacing12 = 48.dp
    val spacing16 = 64.dp    // Espacement entre sections majeures
    val spacing20 = 80.dp    // Espacement galerie ultra-aéré
}
```

---

## Theme.kt

```kotlin
package com.thevault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VaultColorScheme = darkColorScheme(
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
    surfaceTint = SurfaceTint,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    outline = Outline,
    outlineVariant = OutlineVariant,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
)

@Composable
fun VaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VaultColorScheme,
        typography = VaultTypography,
        shapes = VaultShapes,
        content = content,
    )
}
```
