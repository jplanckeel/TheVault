@file:Suppress("MagicNumber") // Design tokens and geometry of the console mockups.

package com.thevault.ui.component

import kotlin.math.abs
import kotlin.math.roundToLong

private const val NARROW_NBSP = ' '

/**
 * Privacy switch « Masquer les prix »: when on, [formatEuro] prints « ••• € ». Set by the
 * app root from the user setting (the UI is recomposed when it changes); exports use their
 * own formatting and are never masked.
 */
object PriceMask {
    /** Whether amounts are hidden. */
    var hidden: Boolean = false
}

/**
 * Formats an amount in euros using the fr-FR convention (`1 234 €`, `12,50 €`).
 * Implemented by hand because `String.format` is not available in common code.
 *
 * @param amount Amount to format.
 * @param decimals Number of decimals (0 in lists, 2 on detail screens).
 */
fun formatEuro(
    amount: Double,
    decimals: Int = 0,
): String {
    if (PriceMask.hidden) return "•••${NARROW_NBSP}€"
    val factor = if (decimals == 2) 100L else 1L
    val scaled = (abs(amount) * factor).roundToLong()
    val integerPart = scaled / factor
    val grouped =
        integerPart
            .toString()
            .reversed()
            .chunked(3)
            .joinToString(NARROW_NBSP.toString())
            .reversed()
    val sign = if (amount < 0 && scaled != 0L) "-" else ""
    val fraction = if (decimals == 2) "," + (scaled % factor).toString().padStart(2, '0') else ""
    return "$sign$grouped$fraction${NARROW_NBSP}€"
}

/** Game count with French agreement: "1 jeu", "3 jeux". */
fun gameCount(count: Int): String = "$count ${gameNoun(count)}"

/** "jeu" or "jeux" depending on [count]. */
fun gameNoun(count: Int): String = if (count > 1) "jeux" else "jeu"

/** Formats an optional amount, returning an em dash when unknown. */
fun formatEuroOrDash(
    amount: Double?,
    decimals: Int = 0,
): String = amount?.let { formatEuro(it, decimals) } ?: "—"

/** Formats a signed amount with an explicit `+` for positive values. */
fun formatSignedEuro(amount: Double): String = if (amount > 0) "+${formatEuro(amount)}" else formatEuro(amount)

private val frenchMonths =
    listOf("janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc.")

/** French date, e.g. "24 mars 2021". */
fun formatDate(date: kotlinx.datetime.LocalDate): String = "${date.day} ${frenchMonths[date.month.ordinal]} ${date.year}"

/** French long date, e.g. "12 mars 2024" (used in uppercase on engraved lines). */
fun formatLongDate(date: kotlinx.datetime.LocalDate): String {
    val months =
        listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre")
    return "${date.day} ${months[date.month.ordinal]} ${date.year}"
}

/** Percentage with one decimal and a French comma: "6,8 %". */
fun formatPercent(value: Double): String {
    val tenths = (abs(value) * 10).roundToLong()
    val text = if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10},${tenths % 10}"
    return "$text${NARROW_NBSP}%"
}

/** Rounded percentage: "18 %". */
fun formatWholePercent(value: Double): String = "${abs(value).roundToLong()}${NARROW_NBSP}%"

/** Relative age in French: "à l'instant", "il y a 2 h", "il y a 3 j". */
fun formatAge(elapsed: kotlin.time.Duration): String =
    when {
        elapsed.inWholeMinutes < 1 -> "à l'instant"
        elapsed.inWholeHours < 1 -> "il y a ${elapsed.inWholeMinutes} min"
        elapsed.inWholeDays < 1 -> "il y a ${elapsed.inWholeHours} h"
        else -> "il y a ${elapsed.inWholeDays} j"
    }

/** Number with thin-space thousands: "1 240". */
fun formatCount(value: Long): String =
    value
        .toString()
        .reversed()
        .chunked(3)
        .joinToString(NARROW_NBSP.toString())
        .reversed()
