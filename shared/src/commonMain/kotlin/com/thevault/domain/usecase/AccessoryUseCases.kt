package com.thevault.domain.usecase

import com.thevault.domain.model.Accessory
import com.thevault.domain.repository.AccessoryRepository
import com.thevault.domain.repository.CatalogSyncRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes every accessory.
 *
 * @param repository Accessory repository.
 */
class ObserveAccessoriesUseCase(
    private val repository: AccessoryRepository,
) {
    /** Every accessory, live. */
    operator fun invoke(): Flow<List<Accessory>> = repository.observeAll()
}

/**
 * Loads one accessory.
 *
 * @param repository Accessory repository.
 */
class GetAccessoryByIdUseCase(
    private val repository: AccessoryRepository,
) {
    /** The accessory [id], or null. */
    suspend operator fun invoke(id: String): Accessory? = repository.getById(id)
}

/**
 * Saves (inserts or updates) an accessory, then pushes it to the catalogue in the background.
 *
 * @param repository Accessory repository.
 * @param sync Queue of the items to push.
 * @param scheduler Background synchronisation.
 */
class SaveAccessoryUseCase(
    private val repository: AccessoryRepository,
    private val sync: CatalogSyncRepository,
    private val scheduler: SyncScheduler,
) {
    /** Persists [accessory]. */
    suspend operator fun invoke(accessory: Accessory) {
        repository.save(accessory)
        sync.markDirty(accessory.id)
        scheduler.request()
    }
}

/**
 * Permanently deletes a accessory.
 *
 * @param repository Accessory repository.
 */
class DeleteAccessoryUseCase(
    private val repository: AccessoryRepository,
) {
    /** Deletes the item [id]. */
    suspend operator fun invoke(id: String) = repository.delete(id)
}
