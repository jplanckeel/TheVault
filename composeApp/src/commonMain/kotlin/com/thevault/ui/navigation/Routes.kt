package com.thevault.ui.navigation

import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind

/** Navigation routes. */
object Routes {
    /** Dashboard tab. */
    const val DASHBOARD = "dashboard"

    /** Collection tab, with an optional `kind` argument ([ItemKind] name). */
    const val COLLECTION = "collection?kind={kind}"

    /** Wishlist tab. */
    const val WISHLIST = "wishlist"

    /** Settings tab. */
    const val SETTINGS = "settings"

    /** Game detail. */
    const val GAME_DETAIL = "game/{id}"

    /** Console detail. */
    const val CONSOLE_DETAIL = "console/{id}"

    /** Accessory detail. */
    const val ACCESSORY_DETAIL = "accessory/{id}"

    /** Add form, or edit form when `gameId` is set. */
    const val ADD_ITEM = "add?gameId={gameId}"

    /** Collection route, optionally opened on a category. */
    fun collection(kind: ItemKind? = null) = if (kind != null) "collection?kind=${kind.name}" else "collection"

    /** Detail route of a collection entry. */
    fun detail(entry: CollectionEntry) =
        when (entry.kind) {
            ItemKind.Game -> "game/${entry.id}"
            ItemKind.Console -> "console/${entry.id}"
            ItemKind.Accessory -> "accessory/${entry.id}"
        }

    /** Add (or edit, when [gameId] is set) route. */
    fun addItem(gameId: String? = null) = if (gameId != null) "add?gameId=$gameId" else "add"
}
