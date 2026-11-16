package com.thevault.domain.repository

import kotlinx.coroutines.flow.Flow

/** Photos the user took of their own copies (one per item, stored on the device). */
interface PhotoRepository {
    /** Photo URI of each item that has one, keyed by item identifier. */
    fun observe(): Flow<Map<String, String>>

    /**
     * Makes [uri] the photo of [itemId].
     *
     * @return URIs of the photos it replaces, whose files can be deleted.
     */
    suspend fun set(
        itemId: String,
        uri: String,
    ): List<String>

    /**
     * Removes the photo of [itemId].
     *
     * @return URIs of the removed photos, whose files can be deleted.
     */
    suspend fun remove(itemId: String): List<String>
}
