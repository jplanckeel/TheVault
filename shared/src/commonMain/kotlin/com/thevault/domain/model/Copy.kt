package com.thevault.domain.model

import kotlinx.datetime.LocalDate

/** Wear grade of one component of a copy. */
enum class Grade {
    /** As new. */
    MINT,

    /** Very good. */
    VERY_GOOD,

    /** Good. */
    GOOD,

    /** Fair, visible wear. */
    FAIR,

    /** Poor, damaged. */
    POOR,
}

/** Completeness of a copy, used to pick its quote. Always derived from [Components]. */
enum class Completeness {
    /** Media only (cartridge, disc, console alone). */
    LOOSE,

    /** Complete in box. */
    CIB,

    /** New or sealed. */
    NEW,
}

/**
 * What a copy is made of, and the wear of each part.
 *
 * @property media The cartridge / disc / console itself.
 * @property box The box.
 * @property manual The manual.
 * @property extras Inserts, posters, bonus discs…
 * @property mediaGrade Wear of the media, null when unknown.
 * @property boxGrade Wear of the box, null when unknown.
 * @property manualGrade Wear of the manual, null when unknown.
 * @property sealed Still sealed.
 * @property digital Digital copy (download code, dematerialised): no physical part.
 */
data class Components(
    val media: Boolean = true,
    val box: Boolean = false,
    val manual: Boolean = false,
    val extras: Boolean = false,
    val mediaGrade: Grade? = null,
    val boxGrade: Grade? = null,
    val manualGrade: Grade? = null,
    val sealed: Boolean = false,
    val digital: Boolean = false,
) {
    /** Completeness derived from the parts: sealed → NEW, boxed → CIB, otherwise LOOSE. */
    val completeness: Completeness
        get() =
            when {
                sealed -> Completeness.NEW
                box -> Completeness.CIB
                else -> Completeness.LOOSE
            }

    /** Whether every physical part is there (media, box and manual). */
    val isComplete: Boolean get() = sealed || (media && box && manual)

    /** Whether every known grade is mint on a complete copy (« Neuf » without seal). */
    val isMintComplete: Boolean
        get() = isComplete && listOfNotNull(mediaGrade, boxGrade, manualGrade).let { it.isNotEmpty() && it.all { g -> g == Grade.MINT } }

    /** Companion presets. */
    companion object {
        /** Media alone. */
        val LOOSE = Components()

        /** Media, box and manual. */
        val CIB = Components(box = true, manual = true)

        /** Sealed copy. */
        val SEALED = Components(box = true, manual = true, sealed = true)

        /** Digital copy. */
        val DIGITAL = Components(media = false, digital = true)
    }
}

/**
 * Release edition.
 */
enum class Edition {
    /** Standard release. */
    STANDARD,

    /** Player's Choice / Nintendo Selects / Classics. */
    BUDGET,

    /** PlayStation Platinum. */
    PLATINUM,

    /** Collector's edition. */
    COLLECTOR,

    /** Limited edition. */
    LIMITED,

    /** Bundle with a console or accessory. */
    BUNDLE,
}

/**
 * Everything that describes one physical copy, shared by games, consoles and accessories.
 *
 * @property components Parts and their wear.
 * @property regionVariant Packaging variant (FAH, UKV, EUR, NOE, USA, JPN…), null when unknown.
 * @property edition Release edition.
 * @property barcode EAN / UPC printed on the box.
 * @property productCode Publisher product code (e.g. DOL-GZLP-FAH).
 * @property shippingCost Shipping paid on purchase.
 * @property acquiredFrom Where it was bought (brocante, Leboncoin, eBay, Vinted, boutique…).
 * @property location Where it is stored.
 * @property tags Free tags.
 * @property loanedTo Borrower, when loaned.
 * @property loanDueAt Expected return date.
 * @property soldPrice Sale price, when sold.
 * @property soldAt Sale date.
 */
data class CopyDetails(
    val components: Components = Components.CIB,
    val regionVariant: String? = null,
    val edition: Edition = Edition.STANDARD,
    val barcode: String? = null,
    val productCode: String? = null,
    val shippingCost: Double? = null,
    val acquiredFrom: String? = null,
    val location: String? = null,
    val tags: List<String> = emptyList(),
    val loanedTo: String? = null,
    val loanDueAt: LocalDate? = null,
    val soldPrice: Double? = null,
    val soldAt: LocalDate? = null,
) {
    /** Total cost of the copy (price + shipping), null when the price is unknown. */
    fun totalCost(purchasePrice: Double?): Double? = purchasePrice?.let { it + (shippingCost ?: 0.0) }
}

/**
 * Legacy single condition stored before components existed (`condition_` column), still
 * written for older readers and the catalogue sync.
 */
val Components.legacyCode: String
    get() =
        when {
            sealed -> "NEW_SEALED"
            box && manual -> "CIB"
            box -> "CIB_NO_MANUAL"
            else -> "LOOSE"
        }

/** Components of a legacy condition code (MINT…POOR become a graded loose media). */
fun componentsOfLegacy(code: String): Components =
    when (code) {
        "LOOSE" -> Components.LOOSE
        "CIB_NO_MANUAL" -> Components(box = true)
        "CIB" -> Components.CIB
        "NEW_SEALED" -> Components.SEALED
        else -> Components(mediaGrade = Grade.entries.find { it.name == code })
    }
