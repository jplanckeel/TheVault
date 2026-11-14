package com.thevault.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.thevault.database.Item_tag
import com.thevault.database.TheVaultQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Tags of every item, keyed by item identifier. */
internal fun TheVaultQueries.observeTags(): Flow<Map<String, List<String>>> =
    selectAllTags()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { rows -> rows.groupBy({ it.item_id }, { it.tag }) }

/** Tags of [itemId]. */
internal fun TheVaultQueries.tagsOf(itemId: String): List<String> =
    selectAllTags().executeAsList().filter { it.item_id == itemId }.map { it.tag }

/** Replaces the tags of [itemId] (call inside a transaction). */
internal fun TheVaultQueries.replaceTags(
    itemId: String,
    tags: List<String>,
) {
    deleteTagsOf(itemId)
    tags
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .forEach { insertTag(Item_tag(itemId, it)) }
}

/** Removes the tags and photos of a deleted item (call inside a transaction). */
internal fun TheVaultQueries.forgetItem(itemId: String) {
    deleteTagsOf(itemId)
    selectPhotosOf(itemId).executeAsList().forEach { deletePhoto(it.id) }
}

/** Combines rows with the tag map into domain objects. */
internal fun <R, D> Flow<List<R>>.withTags(
    tags: Flow<Map<String, List<String>>>,
    id: (R) -> String,
    map: (R, List<String>) -> D,
): Flow<List<D>> = combine(this, tags) { rows, byId -> rows.map { map(it, byId[id(it)].orEmpty()) } }
