package com.thevault.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Root tab of the bottom bar.
 *
 * @property route Navigation route.
 * @property label Tab label.
 * @property icon Unselected icon.
 * @property selectedIcon Selected icon.
 */
enum class VaultTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    /** Collection overview. */
    Dashboard(Routes.DASHBOARD, "Dashboard", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),

    /** Owned items. */
    Collection(Routes.COLLECTION, "Collection", Icons.Outlined.Inventory2, Icons.Filled.Inventory2),

    /** Wanted items. */
    Wishlist(Routes.WISHLIST, "Wishlist", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),

    /** Import / export and sync. */
    Settings(Routes.SETTINGS, "Réglages", Icons.Outlined.Settings, Icons.Filled.Settings),
}
