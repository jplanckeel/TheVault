package com.thevault.domain.repository

import com.thevault.domain.model.Console
import com.thevault.domain.model.ItemStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing [Console] entities.
 * All implementations must reside in the data layer.
 */
interface ConsoleRepository {
    /**
     * Observes all consoles in the collection as a live stream.
     *
     * @return A [Flow] emitting the current list of consoles on every change.
     */
    fun observeAll(): Flow<List<Console>>

    /**
     * Retrieves a single console by its identifier.
     *
     * @param id The unique identifier of the console.
     * @return The [Console] if found, or null otherwise.
     */
    suspend fun getById(id: String): Console?

    /**
     * Observes consoles filtered by their status.
     *
     * @param status The [ItemStatus] to filter by.
     * @return A [Flow] emitting the filtered list.
     */
    fun observeByStatus(status: ItemStatus): Flow<List<Console>>

    /**
     * Inserts or replaces a console in the collection.
     *
     * @param console The [Console] to save.
     */
    suspend fun save(console: Console)

    /**
     * Permanently removes a console from the collection.
     *
     * @param id The unique identifier of the console to delete.
     */
    suspend fun delete(id: String)
}
