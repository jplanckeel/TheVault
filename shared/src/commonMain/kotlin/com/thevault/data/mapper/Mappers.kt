package com.thevault.data.mapper

import com.thevault.domain.model.Accessory
import com.thevault.domain.model.Completeness
import com.thevault.domain.model.Components
import com.thevault.domain.model.Console
import com.thevault.domain.model.CopyDetails
import com.thevault.domain.model.Edition
import com.thevault.domain.model.Game
import com.thevault.domain.model.Grade
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.PlayLog
import com.thevault.domain.model.PlayStatus
import com.thevault.domain.model.Region
import com.thevault.domain.model.WishPrefs
import com.thevault.domain.model.legacyCode
import kotlinx.datetime.LocalDate
import com.thevault.database.Accessory as AccessoryEntity
import com.thevault.database.Console as ConsoleEntity
import com.thevault.database.Game as GameEntity

/**
 * Copy columns shared by the game, console and accessory tables, in a single shape so the
 * conversion is written once.
 */
internal data class CopyColumns(
    val hasMedia: Long,
    val hasBox: Long,
    val hasManual: Long,
    val hasExtras: Long,
    val mediaGrade: String?,
    val boxGrade: String?,
    val manualGrade: String?,
    val isSealed: Long,
    val isDigital: Long,
    val regionVariant: String?,
    val edition: String,
    val barcode: String?,
    val productCode: String?,
    val shippingCost: Double?,
    val acquiredFrom: String?,
    val location: String?,
    val loanedTo: String?,
    val loanDueAt: String?,
    val soldPrice: Double?,
    val soldAt: String?,
    val wishPriority: Long?,
    val wishMaxPrice: Double?,
    val wishTarget: String?,
) {
    /** Domain details, with the [tags] loaded separately. */
    fun toDetails(tags: List<String>) =
        CopyDetails(
            components =
                Components(
                    media = hasMedia != 0L,
                    box = hasBox != 0L,
                    manual = hasManual != 0L,
                    extras = hasExtras != 0L,
                    mediaGrade = grade(mediaGrade),
                    boxGrade = grade(boxGrade),
                    manualGrade = grade(manualGrade),
                    sealed = isSealed != 0L,
                    digital = isDigital != 0L,
                ),
            regionVariant = regionVariant,
            edition = Edition.entries.find { it.name == edition } ?: Edition.STANDARD,
            barcode = barcode,
            productCode = productCode,
            shippingCost = shippingCost,
            acquiredFrom = acquiredFrom,
            location = location,
            tags = tags,
            loanedTo = loanedTo,
            loanDueAt = date(loanDueAt),
            soldPrice = soldPrice,
            soldAt = date(soldAt),
            wish = WishPrefs(wishPriority?.toInt(), wishMaxPrice, wishTarget?.let { t -> Completeness.entries.find { it.name == t } }),
        )

    /** Companion helpers. */
    companion object {
        /** Columns of [details]. */
        fun of(details: CopyDetails): CopyColumns {
            val c = details.components
            return CopyColumns(
                c.media.bit,
                c.box.bit,
                c.manual.bit,
                c.extras.bit,
                c.mediaGrade?.name,
                c.boxGrade?.name,
                c.manualGrade?.name,
                c.sealed.bit,
                c.digital.bit,
                details.regionVariant,
                details.edition.name,
                details.barcode,
                details.productCode,
                details.shippingCost,
                details.acquiredFrom,
                details.location,
                details.loanedTo,
                details.loanDueAt?.toString(),
                details.soldPrice,
                details.soldAt?.toString(),
                details.wish.priority?.toLong(),
                details.wish.maxPrice,
                details.wish.target?.name,
            )
        }

        private val Boolean.bit: Long get() = if (this) 1L else 0L
    }
}

private fun grade(value: String?): Grade? = value?.let { v -> Grade.entries.find { it.name == v } }

private fun date(value: String?): LocalDate? = value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/** Region of a stored value; everything unknown is PAL (the collection is PAL). */
private fun region(value: String): Region = Region.entries.find { it.name == value } ?: Region.PAL

private fun status(value: String): ItemStatus = ItemStatus.entries.find { it.name == value } ?: ItemStatus.OWNED

/** Maps a game row; [tags] come from `item_tag`. */
fun GameEntity.toDomain(tags: List<String> = emptyList()): Game =
    Game(
        id = id,
        title = title,
        platform = platform,
        genre = genre,
        releaseYear = release_year.toInt(),
        region = region(region),
        details =
            CopyColumns(
                has_media,
                has_box,
                has_manual,
                has_extras,
                media_grade,
                box_grade,
                manual_grade,
                is_sealed,
                is_digital,
                region_variant,
                edition,
                barcode,
                product_code,
                shipping_cost,
                acquired_from,
                location,
                loaned_to,
                loan_due_at,
                sold_price,
                sold_at,
                wish_priority,
                wish_max_price,
                wish_target,
            ).toDetails(tags),
        coverUrl = cover_url,
        status = status(status),
        purchasePrice = purchase_price,
        currentValue = current_value,
        notes = notes,
        acquiredAt = date(acquired_at),
        rating = rating?.toInt(),
        developer = developer,
        publisher = publisher,
        gameModes = game_modes,
        themes = themes,
        play = PlayLog(play_status?.let { p -> PlayStatus.entries.find { it.name == p } }, play_minutes?.toInt()),
    )

/** Maps a console row. */
fun ConsoleEntity.toDomain(tags: List<String> = emptyList()): Console =
    Console(
        id = id,
        name = name,
        manufacturer = manufacturer,
        region = region(region),
        color = color,
        details =
            CopyColumns(
                has_media,
                has_box,
                has_manual,
                has_extras,
                media_grade,
                box_grade,
                manual_grade,
                is_sealed,
                is_digital,
                region_variant,
                edition,
                barcode,
                product_code,
                shipping_cost,
                acquired_from,
                location,
                loaned_to,
                loan_due_at,
                sold_price,
                sold_at,
                wish_priority,
                wish_max_price,
                wish_target,
            ).toDetails(tags),
        productionYear = production_year.toInt(),
        status = status(status),
        purchasePrice = purchase_price,
        currentValue = current_value,
        imageUrl = image_url,
        notes = notes,
        acquiredAt = date(acquired_at),
    )

/** Maps an accessory row. */
fun AccessoryEntity.toDomain(tags: List<String> = emptyList()): Accessory =
    Accessory(
        id = id,
        name = name,
        type = type,
        compatibleConsole = compatible_console,
        details =
            CopyColumns(
                has_media,
                has_box,
                has_manual,
                has_extras,
                media_grade,
                box_grade,
                manual_grade,
                is_sealed,
                is_digital,
                region_variant,
                edition,
                barcode,
                product_code,
                shipping_cost,
                acquired_from,
                location,
                loaned_to,
                loan_due_at,
                sold_price,
                sold_at,
                wish_priority,
                wish_max_price,
                wish_target,
            ).toDetails(tags),
        status = status(status),
        purchasePrice = purchase_price,
        currentValue = current_value,
        imageUrl = image_url,
        notes = notes,
        acquiredAt = date(acquired_at),
    )

private val CopyDetails.collectorBit get() = if (edition == Edition.COLLECTOR) 1L else 0L

private val CopyDetails.limitedBit get() = if (edition == Edition.LIMITED) 1L else 0L

/** Maps a game to its row. */
fun Game.toEntity(
    createdAt: String,
    updatedAt: String,
): GameEntity {
    val c = CopyColumns.of(details)
    return GameEntity(
        id,
        title,
        platform,
        genre,
        releaseYear.toLong(),
        region.name,
        details.components.legacyCode,
        coverUrl,
        status.name,
        purchasePrice,
        currentValue,
        notes,
        acquiredAt?.toString(),
        createdAt,
        updatedAt,
        rating?.toLong(),
        developer,
        publisher,
        gameModes,
        themes,
        details.collectorBit,
        details.limitedBit,
        c.hasMedia,
        c.hasBox,
        c.hasManual,
        c.hasExtras,
        c.mediaGrade,
        c.boxGrade,
        c.manualGrade,
        c.isSealed,
        c.isDigital,
        c.regionVariant,
        c.edition,
        c.barcode,
        c.productCode,
        c.shippingCost,
        c.acquiredFrom,
        c.location,
        c.loanedTo,
        c.loanDueAt,
        c.soldPrice,
        c.soldAt,
        c.wishPriority,
        c.wishMaxPrice,
        c.wishTarget,
        play.status?.name,
        play.minutes?.toLong(),
    )
}

/** Maps a console to its row. */
fun Console.toEntity(
    createdAt: String,
    updatedAt: String,
): ConsoleEntity {
    val c = CopyColumns.of(details)
    return ConsoleEntity(
        id,
        name,
        manufacturer,
        region.name,
        color,
        details.components.legacyCode,
        productionYear.toLong(),
        status.name,
        purchasePrice,
        currentValue,
        imageUrl,
        notes,
        acquiredAt?.toString(),
        createdAt,
        updatedAt,
        details.collectorBit,
        details.limitedBit,
        c.hasMedia,
        c.hasBox,
        c.hasManual,
        c.hasExtras,
        c.mediaGrade,
        c.boxGrade,
        c.manualGrade,
        c.isSealed,
        c.isDigital,
        c.regionVariant,
        c.edition,
        c.barcode,
        c.productCode,
        c.shippingCost,
        c.acquiredFrom,
        c.location,
        c.loanedTo,
        c.loanDueAt,
        c.soldPrice,
        c.soldAt,
        c.wishPriority,
        c.wishMaxPrice,
        c.wishTarget,
    )
}

/** Maps an accessory to its row. */
fun Accessory.toEntity(
    createdAt: String,
    updatedAt: String,
): AccessoryEntity {
    val c = CopyColumns.of(details)
    return AccessoryEntity(
        id,
        name,
        type,
        compatibleConsole,
        details.components.legacyCode,
        status.name,
        purchasePrice,
        currentValue,
        imageUrl,
        notes,
        acquiredAt?.toString(),
        createdAt,
        updatedAt,
        details.collectorBit,
        details.limitedBit,
        c.hasMedia,
        c.hasBox,
        c.hasManual,
        c.hasExtras,
        c.mediaGrade,
        c.boxGrade,
        c.manualGrade,
        c.isSealed,
        c.isDigital,
        c.regionVariant,
        c.edition,
        c.barcode,
        c.productCode,
        c.shippingCost,
        c.acquiredFrom,
        c.location,
        c.loanedTo,
        c.loanDueAt,
        c.soldPrice,
        c.soldAt,
        c.wishPriority,
        c.wishMaxPrice,
        c.wishTarget,
    )
}

/** Current time as stored in the `created_at` / `updated_at` columns. */
internal fun nowTimestamp(): String =
    kotlin.time.Clock.System
        .now()
        .toString()
