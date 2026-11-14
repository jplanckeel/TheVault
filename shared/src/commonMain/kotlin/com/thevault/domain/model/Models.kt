package com.thevault.domain.model

import kotlinx.datetime.LocalDate

/**
 * Represents the ownership/availability status of any collectible item.
 */
enum class ItemStatus {
    OWNED,
    WISHLIST,
    SOLD,
    LOANED,
}

/**
 * Represents the geographical region of a collectible item.
 */
enum class Region {
    PAL,
    NTSC_US,
    NTSC_JP,
    OTHER,
}

/**
 * Represents a video game in the user's collection.
 *
 * @property id Unique identifier of the game.
 * @property title Official title of the game.
 * @property platform Platform the game was released on (e.g., "PlayStation 5").
 * @property genre Main genre of the game.
 * @property releaseYear Year of release.
 * @property region Geographical region.
 * @property details Parts, wear, purchase, storage, loan and sale of the copy.
 * @property coverUrl URL of the game cover image, or null if unknown.
 * @property status Current ownership status.
 * @property purchasePrice Price paid at acquisition, or null if unknown.
 * @property currentValue Estimated current market value, or null if unknown.
 * @property notes Optional personal notes about the game.
 * @property acquiredAt Date when the item was acquired.
 */
data class Game(
    val id: String,
    val title: String,
    val platform: String,
    val genre: String,
    val releaseYear: Int,
    val region: Region,
    val details: CopyDetails = CopyDetails(),
    val coverUrl: String? = null,
    val status: ItemStatus,
    val purchasePrice: Double? = null,
    val currentValue: Double? = null,
    val notes: String? = null,
    val acquiredAt: LocalDate? = null,
    val rating: Int? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val gameModes: String? = null,
    val themes: String? = null,
)

/**
 * Represents a gaming console in the user's collection.
 *
 * @property id Unique identifier of the console.
 * @property name Official name of the console.
 * @property manufacturer Manufacturer of the console (e.g., "Sony", "Nintendo").
 * @property region Geographical region variant.
 * @property color Color/edition of the console.
 * @property details Parts, wear, purchase, storage, loan and sale of the copy.
 * @property productionYear Year the console was produced.
 * @property status Current ownership status.
 * @property purchasePrice Price paid at acquisition, or null if unknown.
 * @property currentValue Estimated current market value, or null if unknown.
 * @property imageUrl URL of the console image, or null if unknown.
 * @property notes Optional personal notes.
 * @property acquiredAt Date when the item was acquired.
 */
data class Console(
    val id: String,
    val name: String,
    val manufacturer: String,
    val region: Region,
    val color: String,
    val details: CopyDetails = CopyDetails(),
    val productionYear: Int,
    val status: ItemStatus,
    val purchasePrice: Double? = null,
    val currentValue: Double? = null,
    val imageUrl: String? = null,
    val notes: String? = null,
    val acquiredAt: LocalDate? = null,
)

/**
 * Represents a gaming accessory in the user's collection.
 *
 * @property id Unique identifier of the accessory.
 * @property name Official name of the accessory.
 * @property type Type of accessory (e.g., "Controller", "Memory Card", "Cable").
 * @property compatibleConsole Name of the compatible console(s).
 * @property details Parts, wear, purchase, storage, loan and sale of the copy.
 * @property status Current ownership status.
 * @property purchasePrice Price paid at acquisition, or null if unknown.
 * @property currentValue Estimated current market value, or null if unknown.
 * @property imageUrl URL of the accessory image, or null if unknown.
 * @property notes Optional personal notes.
 * @property acquiredAt Date when the item was acquired.
 */
data class Accessory(
    val id: String,
    val name: String,
    val type: String,
    val compatibleConsole: String,
    val details: CopyDetails = CopyDetails(),
    val status: ItemStatus,
    val purchasePrice: Double? = null,
    val currentValue: Double? = null,
    val imageUrl: String? = null,
    val notes: String? = null,
    val acquiredAt: LocalDate? = null,
)

/**
 * Summary statistics of the entire vault collection.
 *
 * @property totalGames Total number of owned games.
 * @property totalConsoles Total number of owned consoles.
 * @property totalAccessories Total number of owned accessories.
 * @property totalWishlist Total number of wishlist items.
 * @property estimatedValue Estimated total market value of the collection.
 * @property totalInvested Total amount invested in the collection.
 */
data class VaultStats(
    val totalGames: Int,
    val totalConsoles: Int,
    val totalAccessories: Int,
    val totalWishlist: Int,
    val estimatedValue: Double,
    val totalInvested: Double,
) {
    /** Estimated profit/loss compared to purchase prices. */
    val estimatedReturn: Double get() = estimatedValue - totalInvested
}
