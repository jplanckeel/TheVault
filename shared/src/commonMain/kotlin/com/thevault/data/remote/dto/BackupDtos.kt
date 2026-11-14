package com.thevault.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Backup of the whole collection, stored as JSON on the backend. The public share page
 * only reads `kind`, `title`, `platform`, `status`, `coverUrl`, `completeness`, `edition`
 * and `value`.
 *
 * @property version Format version.
 * @property exportedAt ISO time of the backup.
 * @property items Every item.
 * @property sets Completion lists.
 */
@Serializable
data class VaultBackupDto(
    val version: Int = 1,
    val exportedAt: String,
    val items: List<BackupItemDto>,
    val sets: List<BackupSetDto> = emptyList(),
)

/**
 * One item of a backup (games, consoles and accessories share the shape).
 *
 * @property id Identifier.
 * @property kind `game`, `console` or `accessory`.
 * @property title Title or name.
 * @property platform Platform, console name or compatible console.
 * @property status Status name.
 * @property coverUrl Cover.
 * @property completeness Derived completeness (for the share page).
 * @property edition Edition name.
 * @property value Effective value at backup time (for the share page).
 * @property region Region name.
 * @property genre Genre (games).
 * @property releaseYear Release / production year.
 * @property manufacturer Manufacturer (consoles).
 * @property color Color (consoles).
 * @property type Accessory type.
 * @property purchasePrice Price paid.
 * @property currentValue Manual value.
 * @property notes Notes.
 * @property acquiredAt ISO date.
 * @property rating Rating.
 * @property developer Developer.
 * @property publisher Publisher.
 * @property gameModes Game modes.
 * @property themes Themes.
 * @property details Copy details.
 * @property play Play tracking.
 */
@Serializable
data class BackupItemDto(
    val id: String,
    val kind: String,
    val title: String,
    val platform: String,
    val status: String,
    val coverUrl: String? = null,
    val completeness: String,
    val edition: String,
    val value: Double? = null,
    val region: String,
    val genre: String = "",
    val releaseYear: Int = 0,
    val manufacturer: String = "",
    val color: String = "",
    val type: String = "",
    val purchasePrice: Double? = null,
    val currentValue: Double? = null,
    val notes: String? = null,
    val acquiredAt: String? = null,
    val rating: Int? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val gameModes: String? = null,
    val themes: String? = null,
    val details: BackupDetailsDto,
    val play: BackupPlayDto = BackupPlayDto(),
)

/**
 * Copy details of a backed-up item.
 *
 * @property media Media present.
 * @property box Box present.
 * @property manual Manual present.
 * @property extras Extras present.
 * @property mediaGrade Media grade.
 * @property boxGrade Box grade.
 * @property manualGrade Manual grade.
 * @property sealed Sealed.
 * @property digital Digital.
 * @property regionVariant Region variant.
 * @property barcode Barcode.
 * @property productCode Product code.
 * @property shippingCost Shipping.
 * @property acquiredFrom Source.
 * @property location Storage.
 * @property tags Tags.
 * @property loanedTo Borrower.
 * @property loanDueAt Return date.
 * @property soldPrice Sale price.
 * @property soldAt Sale date.
 * @property wishPriority Wish priority.
 * @property wishMaxPrice Wish max price.
 * @property wishTarget Wanted completeness.
 */
@Serializable
data class BackupDetailsDto(
    val media: Boolean = true,
    val box: Boolean = false,
    val manual: Boolean = false,
    val extras: Boolean = false,
    val mediaGrade: String? = null,
    val boxGrade: String? = null,
    val manualGrade: String? = null,
    val sealed: Boolean = false,
    val digital: Boolean = false,
    val regionVariant: String? = null,
    val barcode: String? = null,
    val productCode: String? = null,
    val shippingCost: Double? = null,
    val acquiredFrom: String? = null,
    val location: String? = null,
    val tags: List<String> = emptyList(),
    val loanedTo: String? = null,
    val loanDueAt: String? = null,
    val soldPrice: Double? = null,
    val soldAt: String? = null,
    val wishPriority: Int? = null,
    val wishMaxPrice: Double? = null,
    val wishTarget: String? = null,
)

/**
 * Play tracking of a backed-up game.
 *
 * @property status Status name.
 * @property minutes Minutes played.
 */
@Serializable
data class BackupPlayDto(
    val status: String? = null,
    val minutes: Int? = null,
)

/**
 * Completion list of a backup.
 *
 * @property id Identifier.
 * @property name Name.
 * @property entries Titles as `title` → `platform` pairs.
 */
@Serializable
data class BackupSetDto(
    val id: String,
    val name: String,
    val entries: List<List<String>>,
)
