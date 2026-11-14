package com.thevault.ui.screen.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.ItemStatus
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
}

/**
 * UI state of the wishlist.
 *
 * @property dropping Items whose eBay quote fell over 30 days.
 * @property watched Every wishlist item, in the chosen order.
 * @property total Sum of the known quotes.
 * @property sort Order.
 * @property isLoading Whether data is loading.
 */
data class WishlistUiState(
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
        }
    return WishlistUiState(
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
