package com.thevault.domain.usecase

import com.thevault.domain.model.ItemSet
import com.thevault.domain.model.SetEntry
import com.thevault.domain.repository.SetRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes the completion lists.
 *
 * @param repository Set repository.
 */
class ObserveSetsUseCase(
    private val repository: SetRepository,
) {
    /** Lists with their entries. */
    operator fun invoke(): Flow<List<ItemSet>> = repository.observe()
}

/**
 * Creates, renames or fills a completion list.
 *
 * @param repository Set repository.
 */
class SaveSetUseCase(
    private val repository: SetRepository,
) {
    /** Saves [set] with its entries. */
    suspend operator fun invoke(set: ItemSet) = repository.save(set)
}

/**
 * Deletes a completion list.
 *
 * @param repository Set repository.
 */
class DeleteSetUseCase(
    private val repository: SetRepository,
) {
    /** Deletes the list [id]. */
    suspend operator fun invoke(id: String) = repository.delete(id)
}

/**
 * Adds or removes one title of a completion list.
 *
 * @param repository Set repository.
 */
class EditSetEntryUseCase(
    private val repository: SetRepository,
) {
    /** Adds [entry] to [setId] when [add], removes it otherwise. */
    suspend operator fun invoke(
        setId: String,
        entry: SetEntry,
        add: Boolean,
    ) = if (add) repository.addEntry(setId, entry) else repository.removeEntry(setId, entry)
}
