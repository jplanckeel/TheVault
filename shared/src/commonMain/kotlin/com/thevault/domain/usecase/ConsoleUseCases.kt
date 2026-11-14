package com.thevault.domain.usecase

import com.thevault.domain.model.Console
import com.thevault.domain.repository.CatalogSyncRepository
import com.thevault.domain.repository.ConsoleRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes every console.
 *
 * @param repository Console repository.
 */
class ObserveConsolesUseCase(
    private val repository: ConsoleRepository,
) {
    /** Every console, live. */
    operator fun invoke(): Flow<List<Console>> = repository.observeAll()
}

/**
 * Loads one console.
 *
 * @param repository Console repository.
 */
class GetConsoleByIdUseCase(
    private val repository: ConsoleRepository,
) {
    /** The console [id], or null. */
    suspend operator fun invoke(id: String): Console? = repository.getById(id)
}

/**
 * Saves (inserts or updates) a console, then pushes it to the catalogue in the background.
 *
 * @param repository Console repository.
 * @param sync Queue of the items to push.
 * @param scheduler Background synchronisation.
 */
class SaveConsoleUseCase(
    private val repository: ConsoleRepository,
    private val sync: CatalogSyncRepository,
    private val scheduler: SyncScheduler,
) {
    /** Persists [console]. */
    suspend operator fun invoke(console: Console) {
        repository.save(console)
        sync.markDirty(console.id)
        scheduler.request()
    }
}

/**
 * Permanently deletes a console.
 *
 * @param repository Console repository.
 */
class DeleteConsoleUseCase(
    private val repository: ConsoleRepository,
) {
    /** Deletes the item [id]. */
    suspend operator fun invoke(id: String) = repository.delete(id)
}
