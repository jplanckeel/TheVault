package com.thevault.domain.repository

import com.thevault.domain.model.ItemSet
import com.thevault.domain.model.SetEntry
import kotlinx.coroutines.flow.Flow

/** Completion lists (platform, series, personal lists). */
interface SetRepository {
    /** Every list with its entries, by name. */
    fun observe(): Flow<List<ItemSet>>

    /** Creates or renames a list and replaces its entries. */
    suspend fun save(set: ItemSet)

    /** Deletes a list. */
    suspend fun delete(id: String)

    /** Adds [entry] to the list [setId]. */
    suspend fun addEntry(
        setId: String,
        entry: SetEntry,
    )

    /** Removes [entry] from the list [setId]. */
    suspend fun removeEntry(
        setId: String,
        entry: SetEntry,
    )
}
