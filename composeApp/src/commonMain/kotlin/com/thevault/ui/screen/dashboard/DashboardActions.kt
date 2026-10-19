package com.thevault.ui.screen.dashboard

import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind

/**
 * Navigation callbacks of the dashboard.
 *
 * @property onOpenCollection Opens the collection on a given category.
 * @property onOpenWishlist Opens the wishlist.
 * @property onEntryClick Opens an item detail.
 */
data class DashboardActions(
    val onOpenCollection: (ItemKind) -> Unit = {},
    val onOpenWishlist: () -> Unit = {},
    val onEntryClick: (CollectionEntry) -> Unit = {},
)
