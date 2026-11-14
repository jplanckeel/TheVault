package com.thevault.ui.screen.collection

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * Purchases of one month.
 *
 * @property year Year.
 * @property month Month (1–12).
 * @property count Items acquired.
 * @property spent Price + shipping of those whose price is known.
 */
data class MonthSpend(
    val year: Int,
    val month: Int,
    val count: Int,
    val spent: Double,
)

/** Purchases of the 12 months ending with [today]'s month, oldest first (empty months included). */
fun purchasesByMonth(
    owned: List<CollectionEntry>,
    today: LocalDate,
): List<MonthSpend> {
    val months =
        (MONTHS - 1 downTo 0).map { back ->
            val d = LocalDate(today.year, today.month, 1).minus(DatePeriod(months = back))
            d.year to d.month.ordinal + 1
        }
    val byMonth =
        owned
            .mapNotNull { e ->
                e.acquiredAt?.let { (it.year to it.month.ordinal + 1) to e }
            }.groupBy({ it.first }, { it.second })
    return months.map { (y, m) ->
        val list = byMonth[y to m].orEmpty()
        MonthSpend(y, m, list.size, list.sumOf { it.cost ?: 0.0 })
    }
}

/** Owned items per region and variant (« PAL · FAH », « PAL »…), most first. */
fun regionSplit(owned: List<CollectionEntry>): List<Pair<String, Int>> =
    owned
        .filter { it.kind != ItemKind.Accessory }
        .groupingBy { listOfNotNull(it.region.name.replace("_", "-"), it.details.regionVariant).joinToString(" · ") }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }

private const val MONTHS = 12
