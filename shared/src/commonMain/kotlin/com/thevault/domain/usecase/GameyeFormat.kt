package com.thevault.domain.usecase

import com.thevault.domain.model.Completeness
import com.thevault.domain.model.Components
import com.thevault.domain.model.Grade
import com.thevault.domain.model.ItemStatus
import kotlinx.datetime.LocalDate
import kotlin.math.roundToLong

/** Columns and value formats of the Gameye CSV export. */
object GameyeFormat {
    /** Header of the export, in column order. */
    val HEADERS =
        listOf(
            "Platform",
            "Category",
            "UserRecordType",
            "Title",
            "Country",
            "ReleaseType",
            "Publisher",
            "Developer",
            "Genre",
            "CreatedAt",
            "Ownership",
            "PriceLoose",
            "PriceCIB",
            "PriceNew",
            "YourPrice",
            "PricePaid",
            "ItemCondition",
            "BoxCondition",
            "ManualCondition",
            "Notes",
            "Tags",
            "metacritic",
        )

    /** Column indexes. */
    object Col {
        const val PLATFORM = 0
        const val CATEGORY = 1
        const val RECORD_TYPE = 2
        const val TITLE = 3
        const val COUNTRY = 4
        const val RELEASE_TYPE = 5
        const val PUBLISHER = 6
        const val DEVELOPER = 7
        const val GENRE = 8
        const val CREATED_AT = 9
        const val OWNERSHIP = 10
        const val PRICE_LOOSE = 11
        const val PRICE_CIB = 12
        const val PRICE_NEW = 13
        const val YOUR_PRICE = 14
        const val PRICE_PAID = 15
        const val ITEM_CONDITION = 16
        const val BOX_CONDITION = 17
        const val MANUAL_CONDITION = 18
        const val NOTES = 19
        const val TAGS = 20
    }

    /** Gameye placeholder for an empty text field. */
    private const val MISSING = "Missing Field"

    private val fullMonths = setOf("mars", "mai", "juin", "août")

    private val months = listOf("janv", "févr", "mars", "avr", "mai", "juin", "juil", "août", "sept", "oct", "nov", "déc")

    /** Text value, empty for Gameye placeholders. */
    fun text(raw: String?): String = raw?.trim()?.takeUnless { it == MISSING || it == "?" }.orEmpty()

    /**
     * Amount written `26,6`, `" -1.0"` or `?`; null when unknown (`-1`, `?`, empty).
     */
    fun amount(raw: String?): Double? =
        raw
            ?.trim()
            ?.trim('"')
            ?.trim()
            ?.replace(" ", "")
            ?.replace(',', '.')
            ?.toDoubleOrNull()
            ?.takeIf { it >= 0.0 }

    /** Amount with a decimal comma, `-1.0` when unknown (Gameye convention). */
    fun formatAmount(value: Double?): String {
        if (value == null) return "-1.0"
        val cents = (value * CENTS).roundToLong()
        val whole = cents / CENTS.toLong()
        val fraction = cents % CENTS.toLong()
        return if (fraction == 0L) "$whole" else "$whole," + fraction.toString().padStart(2, '0').trimEnd('0')
    }

    /** French short date (`23 déc. 2020`) or ISO date; null otherwise. */
    fun date(raw: String?): LocalDate? {
        val value = raw?.trim().orEmpty()
        runCatching { return LocalDate.parse(value.take(ISO_DATE)) }
        val parts = value.replace(".", "").split(" ").filter { it.isNotBlank() }
        if (parts.size != DATE_PARTS) return null
        val month = months.indexOfFirst { parts[1].lowercase().startsWith(it) }
        val day = parts[0].toIntOrNull()
        val year = parts[2].toIntOrNull()
        if (month < 0 || day == null || year == null) return null
        return runCatching { LocalDate(year, month + 1, day) }.getOrNull()
    }

    /** French short date as Gameye writes it. */
    fun formatDate(date: LocalDate?): String {
        if (date == null) return ""
        val month = months[date.month.ordinal]
        val dotted = if (month in fullMonths) month else "$month."
        return "${date.day} $dotted ${date.year}"
    }

    /** Grade of a 0–1 condition score; null when unknown (`?`). */
    fun grade(raw: String?): Grade? {
        val score = raw?.trim()?.replace(',', '.')?.toDoubleOrNull() ?: return null
        return when {
            score >= MINT -> Grade.MINT
            score >= VERY_GOOD -> Grade.VERY_GOOD
            score >= GOOD -> Grade.GOOD
            score >= FAIR -> Grade.FAIR
            else -> Grade.POOR
        }
    }

    /** 0–1 score of a grade, `?` when unknown. */
    fun formatGrade(grade: Grade?): String =
        when (grade) {
            Grade.MINT -> "1.0"
            Grade.VERY_GOOD -> "0.8"
            Grade.GOOD -> "0.6"
            Grade.FAIR -> "0.5"
            Grade.POOR -> "0.3"
            null -> "?"
        }

    /** Parts of an `Ownership` value. */
    fun components(ownership: String): Components =
        when (ownership.trim().lowercase()) {
            "loose" -> Components.LOOSE
            "loose+" -> Components(extras = true)
            "new", "sealed" -> Components.SEALED
            "boxed" -> Components(box = true)
            "cib+" -> Components(box = true, manual = true, extras = true)
            "dématérialisé", "digital" -> Components.DIGITAL
            "manual", "manual only" -> Components(media = false, manual = true)
            "box", "box only" -> Components(media = false, box = true)
            else -> Components.CIB
        }

    /** `Ownership` value of parts. */
    fun ownership(components: Components): String =
        when {
            components.digital -> "Dématérialisé"
            components.completeness == Completeness.NEW -> "New"
            components.box && components.manual -> if (components.extras) "CIB+" else "CIB"
            components.box -> "Boxed"
            else -> if (components.extras) "Loose+" else "Loose"
        }

    /** Whether a `ReleaseType` is digital. */
    fun isDigital(releaseType: String): Boolean = releaseType.trim().lowercase() in setOf("digital", "download code")

    /** Status of a `UserRecordType`. */
    fun status(raw: String): ItemStatus =
        when (raw.trim().lowercase()) {
            "wishlist" -> ItemStatus.WISHLIST
            "sold" -> ItemStatus.SOLD
            "loaned" -> ItemStatus.LOANED
            else -> ItemStatus.OWNED
        }

    /** `UserRecordType` of a status. */
    fun formatStatus(status: ItemStatus): String =
        when (status) {
            ItemStatus.WISHLIST -> "Wishlist"
            ItemStatus.SOLD -> "Sold"
            ItemStatus.LOANED -> "Loaned"
            ItemStatus.OWNED -> "Owned"
        }

    /** Tags written `a, b; c`. */
    fun tags(raw: String?): List<String> =
        text(raw)
            .split(',', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    private const val CENTS = 100.0
    private const val ISO_DATE = 10
    private const val DATE_PARTS = 3
    private const val MINT = 0.95
    private const val VERY_GOOD = 0.75
    private const val GOOD = 0.55
    private const val FAIR = 0.4
}
