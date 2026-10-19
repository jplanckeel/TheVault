package com.thevault.ui.screen.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevault.domain.model.ItemStatus
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemActions
import com.thevault.ui.screen.collection.VaultEntriesSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel of the wishlist screen. */
class WishlistViewModel(
    source: VaultEntriesSource,
    private val actions: ItemActions,
) : ViewModel() {
    /** Wishlist entries, most valuable first; null while loading. */
    val entries: StateFlow<List<CollectionEntry>?> =
        source
            .entries()
            .map { all -> all.filter { it.status == ItemStatus.WISHLIST }.sortedByDescending { it.value ?: -1.0 } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** « Je l'ai acheté » — moves the item to the collection. */
    fun markBought(entry: CollectionEntry) {
        viewModelScope.launch { actions.setStatus(entry.kind, entry.id, ItemStatus.OWNED) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
