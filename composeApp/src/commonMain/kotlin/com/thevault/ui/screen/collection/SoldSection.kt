package com.thevault.ui.screen.collection

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sell
import com.thevault.ui.component.EmptyState
import com.thevault.ui.component.Engrave
import com.thevault.ui.component.ListRow
import com.thevault.ui.component.PriceDelta
import com.thevault.ui.component.formatEuro
import com.thevault.ui.component.formatEuroOrDash
import com.thevault.ui.component.formatLongDate
import kotlin.math.abs

/**
 * Totals of the sales.
 *
 * @property count Sold items.
 * @property revenue Sum of the known sale prices.
 * @property profit Sale price − price paid − shipping, on the items where both are known.
 */
data class SalesSummary(
    val count: Int,
    val revenue: Double,
    val profit: Double,
)

/** Realised profit of a sold item, null when the sale or purchase price is unknown. */
val CollectionEntry.realisedProfit: Double?
    get() {
        val sold = details.soldPrice ?: return null
        val cost = details.totalCost(purchasePrice) ?: return null
        return sold - cost
    }

/** Totals of [sold]. */
fun salesSummary(sold: List<CollectionEntry>) =
    SalesSummary(sold.size, sold.sumOf { it.details.soldPrice ?: 0.0 }, sold.sumOf { it.realisedProfit ?: 0.0 })

/** « Vendus » tab: sale price, date and realised profit. */
internal fun LazyGridScope.soldSection(
    sold: List<CollectionEntry>,
    navigation: CollectionNavigation,
) {
    if (sold.isEmpty()) {
        fullWidth { EmptyState(Icons.Default.Sell, "Aucune vente", message = "Passe un objet en « Vendu » pour suivre son bénéfice.") }
        return
    }
    val s = salesSummary(sold)
    fullWidth {
        Engrave(
            "${s.count} ventes · ${formatEuro(
                s.revenue,
            )} encaissés · bénéfice ${if (s.profit >= 0) "+" else "−"}${formatEuro(abs(s.profit))}",
        )
    }
    items(sold, key = { "sold_${it.kind}_${it.id}" }, span = { GridItemSpan(maxLineSpan) }) { e ->
        val meta =
            listOfNotNull(
                e.details.soldAt?.let { "Vendu le ${formatLongDate(it)}" },
                e.purchasePrice?.let { "payé ${formatEuro(it)}" },
            ).joinToString(" · ").ifBlank { "Prix de vente à renseigner" }
        ListRow(
            e.title,
            meta = meta,
            trailing = formatEuroOrDash(e.details.soldPrice),
            cover = Triple(e.coverUrl, e.platform, e.condition),
            onClick = { navigation.onEntryClick(e) },
            trailingContent = e.realisedProfit?.takeIf { it != 0.0 }?.let { p -> { PriceDelta(formatEuro(abs(p)), p > 0, small = true) } },
        )
    }
}
