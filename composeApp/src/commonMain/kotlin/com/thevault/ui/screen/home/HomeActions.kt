package com.thevault.ui.screen.home

import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind

/**
 * Navigation callbacks of the home screen.
 *
 * @property onSearch Opens the collection search.
 * @property onSettings Opens the settings.
 * @property onOpenCollection Opens the collection on a category.
 * @property onOpenPlatform Opens the games of a platform.
 * @property onOpenStats Opens the stats.
 * @property onEntryClick Opens an item detail.
 * @property onAdd Opens the add screen.
 */
data class HomeActions(
    val onSearch: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onOpenCollection: (ItemKind) -> Unit = {},
    val onOpenPlatform: (String) -> Unit = {},
    val onOpenStats: () -> Unit = {},
    val onEntryClick: (CollectionEntry) -> Unit = {},
    val onAdd: () -> Unit = {},
)
