package com.thevault.ui.component

import kotlin.math.abs
import kotlin.math.roundToLong

private const val NARROW_NBSP = ' '

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
