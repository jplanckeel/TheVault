package com.thevault.ui.screen.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.priceGrade
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.VaultEntriesSource
import com.thevault.ui.screen.collection.totalValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Order of the wishlist.
 *
 * @property label Toggle label.
 */
enum class WishSort(
    val label: String,
) {
    /** Most expensive first. */
    Price("Prix"),

    /** Biggest drop first. */
    Drop("Baisse"),

    /** Alphabetical. */
    Title("A → Z"),

    /** Highest priority first. */
    Priority("Priorité"),
}

/**
 * PAL quote of the wanted completeness (or of the item's own) when it is at or under the
 * max price; null otherwise.
 */
val CollectionEntry.underMaxPrice: Double?
    get() {
        val max = details.wish.maxPrice ?: return null
        val grade = details.wish.target?.priceGrade ?: condition.priceGrade
        val price = quotes.find { it.grade == grade && it.isReliable }?.median ?: return null
        return price.takeIf { it <= max }
    }

/**
 * UI state of the wishlist.
 *
 * @property underPrice Items whose PAL quote is at or under their max price.
 * @property dropping Items whose eBay quote fell over 30 days.
 * @property watched Every wishlist item, in the chosen order.
 * @property total Sum of the known quotes.
 * @property sort Order.
 * @property isLoading Whether data is loading.
 */
data class WishlistUiState(
    val underPrice: List<CollectionEntry> = emptyList(),
    val dropping: List<CollectionEntry> = emptyList(),
    val watched: List<CollectionEntry> = emptyList(),
    val total: Double = 0.0,
    val sort: WishSort = WishSort.Price,
    val isLoading: Boolean = true,
)

/** Builds the wishlist state. Pure function. */
fun buildWishlist(
    all: List<CollectionEntry>,
    sort: WishSort,
): WishlistUiState {
    val wished = all.filter { it.status == ItemStatus.WISHLIST }
    val comparator: Comparator<CollectionEntry> =
        when (sort) {
            WishSort.Price -> compareByDescending { it.value ?: -1.0 }
            WishSort.Drop -> compareBy { it.changePercent ?: Double.MAX_VALUE }
            WishSort.Title -> compareBy { it.title.lowercase() }
            WishSort.Priority -> compareBy<CollectionEntry> { it.details.wish.priority ?: Int.MAX_VALUE }.thenBy { it.title.lowercase() }
        }
    return WishlistUiState(
        underPrice = wished.filter { it.underMaxPrice != null }.sortedBy { it.details.wish.priority ?: Int.MAX_VALUE },
        dropping = wished.filter { it.changePercent ?: 0.0 < 0.0 }.sortedBy { it.changePercent },
        watched = wished.sortedWith(comparator),
        total = wished.totalValue(),
        sort = sort,
        isLoading = false,
    )
}

/** ViewModel of the wishlist screen. */
class WishlistViewModel(
    source: VaultEntriesSource,
) : ViewModel() {
    private val sort = MutableStateFlow(WishSort.Price)

    /** Observable UI state. */
    val uiState: StateFlow<WishlistUiState> =
        combine(source.entries(), sort) { all, s -> buildWishlist(all, s) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WishlistUiState())

    /** Cycles the order. */
    fun onSortToggled() {
        sort.value = WishSort.entries[(sort.value.ordinal + 1) % WishSort.entries.size]
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
