package com.thevault.ui.screen.report

import com.thevault.domain.model.PriceSnapshot
import com.thevault.domain.model.PriceSource
import com.thevault.domain.model.priceGrade
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.owned
import kotlinx.datetime.LocalDate

/**
 * Change of one item's PAL quote between two refreshes.
 *
 * @property entry Item.
 * @property old Previous quote for its completeness.
 * @property new New quote.
 */
data class QuoteChange(
    val entry: CollectionEntry,
    val old: Double,
    val new: Double,
) {
    /** Difference in euros. */
    val delta: Double get() = new - old
}

/**
 * What the last quote refresh changed.
 *
 * @property day Day of the refresh.
 * @property ups Rises, biggest first.
 * @property downs Drops, biggest first.
 */
data class QuoteReport(
    val day: LocalDate,
    val ups: List<QuoteChange>,
    val downs: List<QuoteChange>,
) {
    /** Net change of the collection value. */
    val total: Double get() = ups.sumOf { it.delta } + downs.sumOf { it.delta }

    /** Whether anything moved. */
    val isEmpty: Boolean get() = ups.isEmpty() && downs.isEmpty()
}

/**
 * Compares, for every owned item, its PAL quote of the latest refresh with the previous
 * one. Null before the second refresh. Pure function, unit-tested.
 */
fun buildQuoteReport(
    entries: List<CollectionEntry>,
    snapshots: Map<String, List<PriceSnapshot>>,
): QuoteReport? {
    val pal = snapshots.mapValues { (_, list) -> list.filter { it.source == PriceSource.EBAY_PAL }.sortedBy { it.day } }
    val day = pal.values.flatten().maxOfOrNull { it.day } ?: return null
    val changes =
        entries.owned().mapNotNull { e ->
            val grade = e.condition.priceGrade ?: return@mapNotNull null
            val list = pal[e.id].orEmpty()
            val new = list.lastOrNull { it.day == day }?.price(grade) ?: return@mapNotNull null
            val old = list.lastOrNull { it.day < day }?.price(grade) ?: return@mapNotNull null
            QuoteChange(e, old, new).takeIf { it.delta != 0.0 }
        }
    if (pal.values.none { list -> list.any { it.day < day } }) return null
    return QuoteReport(
        day,
        changes.filter { it.delta > 0 }.sortedByDescending { it.delta },
        changes.filter { it.delta < 0 }.sortedBy { it.delta },
    )
}
