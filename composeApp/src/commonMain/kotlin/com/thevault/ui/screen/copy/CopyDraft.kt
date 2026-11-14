package com.thevault.ui.screen.copy

import com.thevault.domain.model.Completeness
import com.thevault.domain.model.Components
import com.thevault.domain.model.CopyDetails
import com.thevault.domain.model.Edition
import com.thevault.domain.model.WishPrefs
import com.thevault.ui.screen.additem.toAmount
import kotlinx.datetime.LocalDate

/** Region variants offered as shortcuts (the collection is PAL). */
val RegionVariants = listOf("FAH", "EUR", "UKV", "NOE", "FRA", "ITA", "ESP", "HOL")

/** Purchase sources offered as shortcuts. */
val PurchaseSources = listOf("Brocante", "Leboncoin", "eBay", "Vinted", "Boutique", "Cadeau")

/**
 * Editable copy of [CopyDetails] plus the purchase price and date, with numbers and dates
 * kept as typed until saved.
 *
 * @property components Parts and grades.
 * @property edition Edition.
 * @property regionVariant Region variant.
 * @property productCode Product code.
 * @property price Price paid.
 * @property shipping Shipping paid.
 * @property acquiredAt Purchase date (JJ/MM/AAAA).
 * @property acquiredFrom Purchase source.
 * @property location Storage place.
 * @property tags Tags, comma separated.
 * @property loanedTo Borrower.
 * @property loanDueAt Return date (JJ/MM/AAAA).
 * @property soldPrice Sale price.
 * @property soldAt Sale date (JJ/MM/AAAA).
 * @property wishPriority Wishlist priority (1 high – 3 low).
 * @property wishMax Wishlist max price.
 * @property wishTarget Wanted completeness.
 * @property base Details not edited by the form (barcode…), kept on save.
 */
data class CopyDraft(
    val components: Components = Components.CIB,
    val edition: Edition = Edition.STANDARD,
    val regionVariant: String = "",
    val productCode: String = "",
    val price: String = "",
    val shipping: String = "",
    val acquiredAt: String = "",
    val acquiredFrom: String = "",
    val location: String = "",
    val tags: String = "",
    val loanedTo: String = "",
    val loanDueAt: String = "",
    val soldPrice: String = "",
    val soldAt: String = "",
    val wishPriority: Int? = null,
    val wishMax: String = "",
    val wishTarget: Completeness? = null,
    val base: CopyDetails = CopyDetails(),
) {
    /** Details to save. */
    fun toDetails(): CopyDetails =
        base.copy(
            components = components,
            edition = edition,
            regionVariant = regionVariant.trim().uppercase().ifBlank { null },
            productCode = productCode.trim().ifBlank { null },
            shippingCost = shipping.toAmount(),
            acquiredFrom = acquiredFrom.trim().ifBlank { null },
            location = location.trim().ifBlank { null },
            tags =
                tags
                    .split(',', ';')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct(),
            loanedTo = loanedTo.trim().ifBlank { null },
            loanDueAt = parseDate(loanDueAt),
            soldPrice = soldPrice.toAmount(),
            soldAt = parseDate(soldAt),
            wish = WishPrefs(wishPriority, wishMax.toAmount(), wishTarget),
        )

    /** Purchase price to save. */
    val purchasePrice: Double? get() = price.toAmount()

    /** Purchase date to save. */
    val purchaseDate: LocalDate? get() = parseDate(acquiredAt)

    /** Whether every typed date is valid. */
    val datesValid: Boolean get() = listOf(acquiredAt, loanDueAt, soldAt).all { it.isBlank() || parseDate(it) != null }

    /** Companion helpers. */
    companion object {
        /** Draft of saved [details], purchase [price] and [date]. */
        fun of(
            details: CopyDetails,
            price: Double?,
            date: LocalDate?,
        ) = CopyDraft(
            components = details.components,
            edition = details.edition,
            regionVariant = details.regionVariant.orEmpty(),
            productCode = details.productCode.orEmpty(),
            price = price.plain(),
            shipping = details.shippingCost.plain(),
            acquiredAt = formatDate(date),
            acquiredFrom = details.acquiredFrom.orEmpty(),
            location = details.location.orEmpty(),
            tags = details.tags.joinToString(", "),
            loanedTo = details.loanedTo.orEmpty(),
            loanDueAt = formatDate(details.loanDueAt),
            soldPrice = details.soldPrice.plain(),
            soldAt = formatDate(details.soldAt),
            wishPriority = details.wish.priority,
            wishMax = details.wish.maxPrice.plain(),
            wishTarget = details.wish.target,
            base = details,
        )

        /** `12/03/2024` (or `2024-03-12`) → date; null when blank or invalid. */
        fun parseDate(text: String): LocalDate? {
            val t = text.trim()
            if (t.isEmpty()) return null
            runCatching { return LocalDate.parse(t) }
            val parts = t.split('/', '.', '-').mapNotNull { it.trim().toIntOrNull() }
            if (parts.size != DATE_PARTS) return null
            val year = if (parts[2] < CENTURY) parts[2] + YEAR_2000 else parts[2]
            return runCatching { LocalDate(year, parts[1], parts[0]) }.getOrNull()
        }

        /** Date as `12/03/2024`, empty when null. */
        fun formatDate(date: LocalDate?): String =
            date?.let { "${it.day.toString().padStart(2, '0')}/${(it.month.ordinal + 1).toString().padStart(2, '0')}/${it.year}" }.orEmpty()

        private fun Double?.plain(): String =
            when {
                this == null -> ""
                this % 1.0 == 0.0 -> toLong().toString()
                else -> toString().replace('.', ',')
            }

        private const val DATE_PARTS = 3
        private const val CENTURY = 100
        private const val YEAR_2000 = 2000
    }
}

/** Toggles a part, keeping the parts consistent (sealed implies complete, digital has no parts). */
fun Components.toggle(part: String): Components =
    when (part) {
        "media" -> copy(media = !media, digital = false)
        "box" -> copy(box = !box, digital = false, sealed = sealed && !box)
        "manual" -> copy(manual = !manual, digital = false, sealed = sealed && !manual)
        "extras" -> copy(extras = !extras)
        "sealed" -> if (sealed) copy(sealed = false) else copy(media = true, box = true, manual = true, sealed = true, digital = false)
        "digital" -> if (digital) Components.LOOSE else Components.DIGITAL
        else -> this
    }
