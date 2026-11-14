package com.thevault.ui.navigation

import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind

/** Navigation routes. */
object Routes {
    private const val BYTE_MASK = 0xFF
    private const val ASCII_LIMIT = 0x80
    private const val HEX = 16

    /** Home tab. */
    const val HOME = "home"

    /** Collection tab, with an optional `kind` argument ([ItemKind] name). */
    const val COLLECTION = "collection?kind={kind}"

    /** Games of a platform; no `name` means every game. */
    const val PLATFORM = "platform?name={name}"

    /** Wishlist tab. */
    const val WISHLIST = "wishlist"

    /** Profile tab (level, achievements). */
    const val PROFILE = "profile"

    /** Stats, opened from the profile. */
    const val STATS = "stats"

    /** Settings (CSV import / export, IGDB sync). */
    const val SETTINGS = "settings"

    /** Game detail. */
    const val GAME_DETAIL = "game/{id}"

    /** Console detail. */
    const val CONSOLE_DETAIL = "console/{id}"

    /** Accessory detail. */
    const val ACCESSORY_DETAIL = "accessory/{id}"

    /** Add flow (search + quick confirmation). */
    const val ADD = "add"

    /** Full game form, editing `gameId`. */
    const val EDIT_GAME = "edit?gameId={gameId}"

    /** Collection route, optionally opened on a category. */
    fun collection(kind: ItemKind? = null) = if (kind != null) "collection?kind=${kind.name}" else "collection"

    /** Games of [platform] (null = every game). */
    fun platform(platform: String?) = if (platform != null) "platform?name=${encode(platform)}" else "platform"

    /** Detail route of a collection entry. */
    fun detail(entry: CollectionEntry) =
        when (entry.kind) {
            ItemKind.Game -> "game/${entry.id}"
            ItemKind.Console -> "console/${entry.id}"
            ItemKind.Accessory -> "accessory/${entry.id}"
        }

    /** Full form of game [gameId]. */
    fun editGame(gameId: String) = "edit?gameId=$gameId"

    /** Percent-encodes a query argument (platform names contain spaces and slashes). */
    private fun encode(value: String): String =
        buildString {
            value.encodeToByteArray().forEach { byte ->
                val b = byte.toInt() and BYTE_MASK
                val c = b.toChar()
                val unreserved = b < ASCII_LIMIT && (c.isLetterOrDigit() || c in "-_.~")
                if (unreserved) append(c) else append("%" + b.toString(HEX).uppercase().padStart(2, '0'))
            }
        }
}
