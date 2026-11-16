package com.thevault.domain.model

/**
 * Everything a backup holds.
 *
 * @property games Games.
 * @property consoles Consoles.
 * @property accessories Accessories.
 * @property sets Completion lists.
 * @property values Effective value of each item at backup time (shown by share links).
 */
data class VaultContent(
    val games: List<Game>,
    val consoles: List<Console>,
    val accessories: List<Accessory>,
    val sets: List<ItemSet>,
    val values: Map<String, Double?> = emptyMap(),
)

/**
 * Account signed in on the device.
 *
 * @property email Email.
 * @property name Display name.
 */
data class Account(
    val email: String,
    val name: String,
)

/** Scope of a share link. */
enum class ShareScope(
    val key: String,
) {
    /** Owned items. */
    COLLECTION("collection"),

    /** Wishlist. */
    WISHLIST("wishlist"),
}

/**
 * Share link.
 *
 * @property token Token.
 * @property scope Scope.
 * @property title Title.
 * @property hidePrices Whether values are hidden.
 * @property url Public URL.
 */
data class ShareLink(
    val token: String,
    val scope: ShareScope,
    val title: String,
    val hidePrices: Boolean,
    val url: String,
)

/**
 * One backup saved on the account.
 *
 * @property id Version identifier.
 * @property itemCount Number of items it holds.
 * @property createdAt When it was saved (ISO-8601).
 */
data class BackupVersion(
    val id: Long,
    val itemCount: Int,
    val createdAt: String,
)

/**
 * The server refused a backup that would lose more than half of the saved items
 * (an empty or new phone); it is sent again with `force` once the user confirms.
 *
 * @property currentCount Items of the backup kept on the server.
 * @property newCount Items of the refused backup.
 */
class BackupShrinkException(
    val currentCount: Int,
    val newCount: Int,
) : Exception("La sauvegarde du serveur contient $currentCount objets, celle-ci $newCount.")
