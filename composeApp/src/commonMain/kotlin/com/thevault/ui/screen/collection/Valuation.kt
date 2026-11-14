package com.thevault.ui.screen.collection

import com.thevault.domain.model.Components
import com.thevault.domain.model.PriceGrade
import com.thevault.domain.model.PricePoint
import com.thevault.domain.model.PriceQuote
import com.thevault.domain.model.PriceSnapshot
import com.thevault.domain.model.PriceSource
import com.thevault.domain.model.priceGrade
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/** Origin of the value of an item. */
enum class ValueSource {
    /** Reliable eBay PAL quote. */
    Ebay,

    /** Value typed by the user. */
    Manual,

    /** US price of a Gameye import — indicative, never a PAL quote. */
    UsIndicative,

    /** Unknown. */
    None,
}

/**
 * Effective value: the reliable eBay PAL quote of the item's completeness, otherwise the
 * value typed by the user, otherwise the indicative US price of the import.
 */
fun effectiveValue(
    manual: Double?,
    quote: PriceQuote?,
    us: Double? = null,
): Pair<Double?, ValueSource> =
    when {
        quote != null && quote.isReliable -> quote.median to ValueSource.Ebay
        manual != null -> manual to ValueSource.Manual
        us != null -> us to ValueSource.UsIndicative
        else -> null to ValueSource.None
    }

/** Quote matching [condition] among [quotes], when reliable. */
fun quoteFor(
    condition: Components,
    quotes: List<PriceQuote>,
): PriceQuote? {
    val grade = condition.priceGrade ?: return null
    return quotes.find { it.grade == grade && it.isReliable }
}

/** US price of the last Gameye import for [condition]. */
fun usPriceFor(
    condition: Components,
    snapshots: List<PriceSnapshot>,
): Double? {
    val grade = condition.priceGrade ?: return null
    return snapshots.lastOrNull { it.source == PriceSource.GAMEYE_US }?.price(grade)
}

/**
 * Values the entries with the eBay quotes and the imported US prices. The manual value
 * typed by the user stays in [CollectionEntry.value] until a source replaces it.
 */
fun List<CollectionEntry>.withMarket(
    quotes: Map<String, List<PriceQuote>>,
    snapshots: Map<String, List<PriceSnapshot>> = emptyMap(),
): List<CollectionEntry> =
    map { entry ->
        val itemQuotes = quotes[entry.id].orEmpty()
        val q = quoteFor(entry.condition, itemQuotes)
        val us = usPriceFor(entry.condition, snapshots[entry.id].orEmpty())
        val (value, source) = effectiveValue(entry.value, q, us)
        entry.copy(value = value, valueSource = source, quote = q, quotes = itemQuotes, usPrice = us)
    }

/** Applies the eBay quotes ([byId]) to the entries. */
fun List<CollectionEntry>.withQuotes(byId: Map<String, List<PriceQuote>>): List<CollectionEntry> = withMarket(byId)

/** PAL quote history recorded on the device, as price points (one per grade and day). */
fun localHistory(snapshots: List<PriceSnapshot>): List<PricePoint> =
    snapshots
        .filter { it.source == PriceSource.EBAY_PAL }
        .flatMap { s ->
            val at = s.day.atStartOfDayIn(TimeZone.UTC)
            PriceGrade.entries.mapNotNull { g -> s.price(g)?.let { PricePoint(at, g, it) } }
        }

/** Backend history completed with the local points of days the backend does not have. */
fun mergeHistory(
    remote: List<PricePoint>,
    local: List<PricePoint>,
): List<PricePoint> {
    fun key(p: PricePoint) = p.grade to p.at.toLocalDateTime(TimeZone.UTC).date
    val known = remote.map(::key).toSet()
    return (remote + local.filter { key(it) !in known }).sortedBy { it.at }
}
