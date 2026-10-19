package com.thevault.ui.screen.collection

/**
 * Callbacks of a collection grid.
 *
 * @property onKindSelected Category selected.
 * @property onQueryChanged Search query changed.
 * @property onPlatformSelected Platform chip selected (null = all).
 * @property onSortSelected Sort order changed.
 * @property onEntryClick Entry tapped.
 * @property onAddItem "Add" CTA of the empty state.
 */
data class CollectionActions(
    val onKindSelected: (ItemKind) -> Unit = {},
    val onQueryChanged: (String) -> Unit = {},
    val onPlatformSelected: (String?) -> Unit = {},
    val onSortSelected: (CollectionSort) -> Unit = {},
    val onEntryClick: (CollectionEntry) -> Unit = {},
    val onAddItem: () -> Unit = {},
)
