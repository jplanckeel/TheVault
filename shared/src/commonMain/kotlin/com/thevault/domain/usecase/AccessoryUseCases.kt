package com.thevault.domain.usecase

import com.thevault.domain.model.Accessory
import com.thevault.domain.repository.AccessoryRepository
import kotlinx.coroutines.flow.Flow

class ObserveAccessoriesUseCase(
    private val repository: AccessoryRepository,
) {
    operator fun invoke(): Flow<List<Accessory>> = repository.observeAll()
}

class GetAccessoryByIdUseCase(
    private val repository: AccessoryRepository,
) {
    suspend operator fun invoke(id: String): Accessory? = repository.getById(id)
}
