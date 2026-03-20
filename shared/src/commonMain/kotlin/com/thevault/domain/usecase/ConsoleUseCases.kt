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
