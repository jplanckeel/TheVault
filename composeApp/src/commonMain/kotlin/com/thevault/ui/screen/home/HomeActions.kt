package com.thevault.ui.screen.home

import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind

/**
 * Navigation callbacks of the home screen.
 *
 * @property onSearch Opens the collection search.
 * @property onOpenCollection Opens the collection on a category.
 * @property onOpenRisers Opens every game (« Tout voir »).
 * @property onEntryClick Opens an item detail.
 * @property onAdd Opens the add screen.
 * @property onSync Opens the sync settings (tap on the LED).
 */
data class HomeActions(
    val onSearch: () -> Unit = {},
    val onOpenCollection: (ItemKind) -> Unit = {},
    val onOpenRisers: () -> Unit = {},
    val onEntryClick: (CollectionEntry) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onSync: () -> Unit = {},
)
