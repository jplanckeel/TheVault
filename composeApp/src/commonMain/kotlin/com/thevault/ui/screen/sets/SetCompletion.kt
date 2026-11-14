package com.thevault.ui.screen.sets

import com.thevault.domain.model.ItemSet
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.SetEntry
import com.thevault.ui.screen.add.ownedKey
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind
import com.thevault.ui.screen.collection.ValueSource

/**
 * One title of a list, matched against the collection.
 *
 * @property entry Title and platform.
 * @property owned Owned copy, null when missing.
 * @property wished Wishlist item for the title, when missing.
 */
data class SetLine(
    val entry: SetEntry,
    val owned: CollectionEntry?,
    val wished: CollectionEntry?,
) {
    /** Whether the title is in the collection. */
    val isOwned: Boolean get() = owned != null

    /** Quote of the missing title (wishlist item with a PAL quote), null when unknown. */
    val missingCost: Double? get() = wished?.takeIf { it.valueSource == ValueSource.Ebay }?.value
}

/**
 * Completion of a list.
 *
 * @property set List.
 * @property lines Titles matched against the collection.
 */
data class SetCompletion(
    val set: ItemSet,
    val lines: List<SetLine>,
) {
    /** Owned titles. */
    val ownedCount: Int get() = lines.count { it.isOwned }

    /** Completion between 0 and 1. */
    val ratio: Float get() = if (lines.isEmpty()) 0f else ownedCount.toFloat() / lines.size

    /** Missing titles. */
    val missing: List<SetLine> get() = lines.filter { !it.isOwned }

    /** Estimated cost of the missing titles that have a PAL quote. */
    val estimatedCost: Double get() = missing.sumOf { it.missingCost ?: 0.0 }

    /** Missing titles without a quote (not counted in [estimatedCost]). */
    val unquoted: Int get() = missing.count { it.missingCost == null }
}

/** Matches every list against the games of the collection. Pure function, unit-tested. */
fun completions(
    sets: List<ItemSet>,
    entries: List<CollectionEntry>,
): List<SetCompletion> {
    val games = entries.filter { it.kind == ItemKind.Game }
    val owned = games.filter { it.status == ItemStatus.OWNED }.associateBy { ownedKey(it.title, it.platform) }
    val wished = games.filter { it.status == ItemStatus.WISHLIST }.associateBy { ownedKey(it.title, it.platform) }
    return sets.map { set ->
        SetCompletion(
            set,
            set.entries.map { e ->
                val key = ownedKey(e.title, e.platform)
                SetLine(e, owned[key], wished[key])
            },
        )
    }
}
