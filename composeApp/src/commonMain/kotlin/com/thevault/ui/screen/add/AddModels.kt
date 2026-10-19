package com.thevault.ui.screen.add

import com.thevault.domain.model.ItemCondition
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import com.thevault.domain.model.RemoteGame
import com.thevault.ui.component.FormatGroup
import com.thevault.ui.component.formatGroup
import com.thevault.ui.screen.collection.CollectionEntry
import com.thevault.ui.screen.collection.ItemKind

/** Formats offered by the quick add sheet. */
val QuickFormats = listOf(ItemCondition.LOOSE, ItemCondition.CIB, ItemCondition.NEW_SEALED)

/** Regions offered by the quick add sheet. */
val QuickRegions = listOf(Region.PAL, Region.NTSC_US, Region.NTSC_JP)

/**
 * Something the user can add: an IGDB game, or a console / accessory typed by hand.
 *
 * @property kind Item category.
 * @property title Title (or typed name).
 * @property platforms Candidate platforms; the user picks one when there are several.
 * @property remote IGDB game, null for consoles and accessories.
 */
data class AddCandidate(
    val kind: ItemKind,
    val title: String,
    val platforms: List<String>,
    val remote: RemoteGame? = null,
) {
    /** Year · developer line of the sheet. */
    val subtitle: String
        get() = listOfNotNull(remote?.releaseYear?.toString(), remote?.developer).joinToString(" · ")
}

/**
 * Choices of the quick add sheet.
 *
 * @property platform Chosen platform.
 * @property status Collection (owned) or wishlist.
 * @property condition Chosen format.
 * @property region Chosen region.
 * @property price Purchase price as typed.
 */
data class QuickAddForm(
    val platform: String = "",
    val status: ItemStatus = ItemStatus.OWNED,
    val condition: ItemCondition = ItemCondition.CIB,
    val region: Region = Region.PAL,
    val price: String = "",
)

/** Normalised « title|platform » key used to flag owned results. */
fun ownedKey(
    title: String,
    platform: String,
): String = normalize(title) + "|" + normalize(platform)

private fun normalize(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

/** Keys of the games already in the collection (any status). */
fun ownedKeys(entries: List<CollectionEntry>): Set<String> =
    entries.filter { it.kind == ItemKind.Game }.mapTo(mutableSetOf()) { ownedKey(it.title, it.platform) }

/** Whether a search result is already owned on one of its platforms. */
fun RemoteGame.isOwned(keys: Set<String>): Boolean = platformNames.any { ownedKey(title, it) in keys }

/** Most frequent region of the collection, PAL when empty. */
fun preferredRegion(entries: List<CollectionEntry>): Region {
    val counts =
        entries
            .filter { it.region in QuickRegions }
            .groupingBy { it.region }
            .eachCount()
    return counts.maxByOrNull { it.value }?.key ?: Region.PAL
}

/** Share of the collection in CIB, in percent (null when the collection is empty). */
fun cibShare(entries: List<CollectionEntry>): Int? {
    if (entries.isEmpty()) return null
    return entries.count { it.condition.formatGroup == FormatGroup.Cib } * PERCENT / entries.size
}

/** Purchase price typed with a comma or a dot; null when empty or invalid. */
fun String.toAmount(): Double? = trim().replace(',', '.').replace(" ", "").toDoubleOrNull()

private const val PERCENT = 100
