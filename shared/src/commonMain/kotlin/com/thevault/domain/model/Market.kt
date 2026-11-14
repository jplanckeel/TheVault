package com.thevault.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Condition family an eBay quote is computed for (`loose`, `cib`, `new` on the backend).
 *
 * @property apiValue Value used by the backend.
 */
enum class PriceGrade(
    val apiValue: String,
) {
    /** Cartridge or disc only. */
    LOOSE("loose"),

    /** Complete in box. */
    CIB("cib"),

    /** New or sealed. */
    NEW("new"),
    ;

    /** Companion helpers. */
    companion object {
        /** Grade of a backend value, or null when unknown. */
        fun fromApi(value: String): PriceGrade? = entries.find { it.apiValue == value.lowercase() }
    }
}

/** Grade whose quote applies to a copy with these components (digital copies have none). */
val Components.priceGrade: PriceGrade?
    get() =
        when {
            digital -> null
            completeness == Completeness.NEW -> PriceGrade.NEW
            completeness == Completeness.CIB -> PriceGrade.CIB
            else -> PriceGrade.LOOSE
        }

/**
 * Latest eBay quote of an item for one grade.
 *
 * @property itemId Local item identifier (the backend `external_id`).
 * @property grade Condition family.
 * @property median Median of the sold / listed prices.
 * @property min Lowest price seen.
 * @property max Highest price seen.
 * @property samples Number of listings the quote was computed from.
 * @property scrapedAt When the quote was computed.
 * @property medianPast Median quoted 30 days earlier, or null when unknown.
 */
data class PriceQuote(
    val itemId: String,
    val grade: PriceGrade,
    val median: Double,
    val min: Double,
    val max: Double,
    val samples: Int,
    val scrapedAt: Instant,
    val medianPast: Double? = null,
) {
    /** Whether enough listings back the quote to use it as the item value. */
    val isReliable: Boolean get() = samples >= MIN_RELIABLE_SAMPLES && median > 0.0

    /** Companion constants. */
    companion object {
        /** Listings needed before a quote replaces the value typed by the user. */
        const val MIN_RELIABLE_SAMPLES = 3
    }
}

/**
 * One point of an item price history.
 *
 * @property at When the price was quoted.
 * @property grade Condition family.
 * @property median Median price.
 */
data class PricePoint(
    val at: Instant,
    val grade: PriceGrade,
    val median: Double,
)

/**
 * Value of the whole collection on one day, recorded on the device.
 *
 * @property day Day of the snapshot.
 * @property value Estimated value of the owned items.
 * @property invested Sum of the known purchase prices.
 */
data class ValueSnapshot(
    val day: LocalDate,
    val value: Double,
    val invested: Double,
)

/**
 * Achievement unlocked on this device.
 *
 * @property id Achievement identifier.
 * @property unlockedAt When it was unlocked.
 * @property seen Whether the unlock notification was shown.
 */
data class AchievementUnlock(
    val id: String,
    val unlockedAt: Instant,
    val seen: Boolean,
)
