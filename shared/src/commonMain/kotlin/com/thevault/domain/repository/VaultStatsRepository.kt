package com.thevault.domain.repository

import com.thevault.domain.model.VaultStats
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for accessing aggregated vault statistics.
 */
interface VaultStatsRepository {
    /**
     * Observes the aggregated statistics of the entire vault collection.
     *
     * @return A [Flow] emitting updated [VaultStats] on every collection change.
     */
    fun observeStats(): Flow<VaultStats>
}
