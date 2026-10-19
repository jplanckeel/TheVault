package com.thevault.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Root tab of the bottom bar (« une seule barre opaque à 5 entrées »).
 *
 * @property route Navigation route.
 * @property label Tab label.
 * @property icon Line icon.
 */
enum class VaultTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    /** Home. */
    Home(Routes.HOME, "Accueil", Icons.Outlined.Home),

    /** Collection. */
    Collection(Routes.COLLECTION, "Collection", Icons.Outlined.GridView),

    /** Wishlist. */
    Wishlist(Routes.WISHLIST, "Wishlist", Icons.Outlined.StarOutline),

    /** Stats. */
    Stats(Routes.STATS, "Stats", Icons.Outlined.BarChart),
}
