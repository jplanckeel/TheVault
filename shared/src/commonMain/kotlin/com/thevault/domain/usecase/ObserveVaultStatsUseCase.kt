package com.thevault.domain.usecase

import com.thevault.domain.model.VaultStats
import com.thevault.domain.repository.VaultStatsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to observe the aggregated statistics of the entire vault collection.
 *
 * @property repository The [VaultStatsRepository] to observe data from.
 */
class ObserveVaultStatsUseCase(
    private val repository: VaultStatsRepository,
) {
    /**
     * Executes the use case.
     *
     * @return A [Flow] emitting updated [VaultStats] on every collection change.
     */
    operator fun invoke(): Flow<VaultStats> = repository.observeStats()
}
