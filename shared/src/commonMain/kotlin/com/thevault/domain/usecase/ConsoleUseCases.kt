package com.thevault.domain.usecase

import com.thevault.domain.model.Console
import com.thevault.domain.repository.ConsoleRepository
import kotlinx.coroutines.flow.Flow

class ObserveConsolesUseCase(
    private val repository: ConsoleRepository,
) {
    operator fun invoke(): Flow<List<Console>> = repository.observeAll()
}

class GetConsoleByIdUseCase(
    private val repository: ConsoleRepository,
) {
    suspend operator fun invoke(id: String): Console? = repository.getById(id)
}

/**
 * Saves (inserts or updates) a console.
 *
 * @property repository Console repository.
 */
class SaveConsoleUseCase(
    private val repository: ConsoleRepository,
) {
    /** Persists [console]. */
    suspend operator fun invoke(console: Console) = repository.save(console)
}
