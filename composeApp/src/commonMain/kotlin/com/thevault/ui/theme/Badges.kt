package com.thevault.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colors of a badge (platform or format).
 *
 * @property container Background.
 * @property content Text.
 * @property border Inner border, or null.
 */
data class BadgeColors(
    val container: Color,
    val content: Color,
    val border: Color? = null,
)

/** One tint per manufacturer — « une teinte par constructeur, le nom fait la différence ». */
object PlatformBadgeColors {
    /** Nintendo. */
    val Nintendo = BadgeColors(Color(0xFF3A1820), Color(0xFFFFB3B7), Color(0xFF5E2430))

    /** Microsoft / Xbox. */
    val Microsoft = BadgeColors(Color(0xFF12301C), Color(0xFFA9EDB3), Color(0xFF1F4D2C))

    /** Sony / PlayStation. */
    val Sony = BadgeColors(Color(0xFF15244A), Color(0xFFB5CBFF), Color(0xFF24397A))

    /** Any other manufacturer. */
    val Other = BadgeColors(Color(0xFF1A2547), Color(0xFFC9D1F0), Color(0xFF26335E))
}

/** Format badges (ex « état / format »). */
object FormatBadgeColors {
    /** Loose. */
    val Loose = BadgeColors(Color(0xFF3A2A1C), Color(0xFFFFC58F))

    /** Complete in box (CIB, CIB+, CIB−). */
    val Cib = BadgeColors(Color(0xFF2A2152), Color(0xFFCDBBFF))

    /** New / sealed / mint. */
    val New = BadgeColors(Color(0xFF123B36), Color(0xFF7FE8CB))

    /** Other conditions. */
    val Other = BadgeColors(Color(0xFF1A2547), Color(0xFFC9D1F0))
}

/** Dark tints used as cover placeholders, per manufacturer. */
object CoverTints {
    /** Nintendo. */
    val Nintendo = Color(0xFF3E1B22)

    /** Microsoft / Xbox. */
    val Microsoft = Color(0xFF173A23)

    /** Sony / PlayStation. */
    val Sony = Color(0xFF1F2C52)

    /** Other manufacturers. */
    val Other = Color(0xFF2B2A63)
}
