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
