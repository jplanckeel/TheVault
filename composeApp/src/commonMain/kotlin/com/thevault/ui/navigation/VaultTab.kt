package com.thevault.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Tab of the floating tab bar (« Accueil, Collection, +, Envies, Profil »).
 *
 * @property route Navigation route.
 * @property label Tab label.
 * @property icon Line icon.
 * @property children Other routes that keep this tab active.
 */
enum class VaultTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val children: Set<String> = emptySet(),
) {
    /** Home. */
    Home(Routes.HOME, "Accueil", Icons.Outlined.Home),

    /** Collection, and the games of a platform. */
    Collection(Routes.COLLECTION, "Collection", Icons.Outlined.GridView, setOf(Routes.PLATFORM)),

    /** Wishlist. */
    Wishlist(Routes.WISHLIST, "Envies", Icons.Outlined.FavoriteBorder),

    /** Profile, achievements and stats. */
    Profile(Routes.PROFILE, "Profil", Icons.Outlined.Person, setOf(Routes.STATS)),
    ;

    /** Companion helpers. */
    companion object {
        /** Tab owning [route], null when the tab bar is hidden there. */
        fun of(route: String?): VaultTab? = entries.find { it.route == route || route in it.children }
    }
}
