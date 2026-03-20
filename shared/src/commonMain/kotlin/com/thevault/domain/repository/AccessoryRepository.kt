package com.thevault.domain.repository

import com.thevault.domain.model.Accessory
import com.thevault.domain.model.ItemStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing [Accessory] entities.
 * All implementations must reside in the data layer.
 */
interface AccessoryRepository {
    /**
     * Observes all accessories in the collection as a live stream.
     *
     * @return A [Flow] emitting the current list of accessories on every change.
     */
    fun observeAll(): Flow<List<Accessory>>

    /**
     * Retrieves a single accessory by its identifier.
     *
     * @param id The unique identifier of the accessory.
     * @return The [Accessory] if found, or null otherwise.
     */
    suspend fun getById(id: String): Accessory?

    /**
     * Observes accessories filtered by their status.
     *
     * @param status The [ItemStatus] to filter by.
     * @return A [Flow] emitting the filtered list.
     */
    fun observeByStatus(status: ItemStatus): Flow<List<Accessory>>

    /**
     * Inserts or replaces an accessory in the collection.
     *
     * @param accessory The [Accessory] to save.
     */
    suspend fun save(accessory: Accessory)

    /**
     * Permanently removes an accessory from the collection.
     *
     * @param id The unique identifier of the accessory to delete.
     */
    suspend fun delete(id: String)
}
