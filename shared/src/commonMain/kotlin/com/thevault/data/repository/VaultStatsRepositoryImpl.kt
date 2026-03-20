package com.thevault.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.thevault.database.TheVaultDatabase
import com.thevault.domain.model.VaultStats
import com.thevault.domain.repository.VaultStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * Implementation of [VaultStatsRepository] using SQLDelight for local persistence.
 *
 * @property database The [TheVaultDatabase] to perform queries on.
 */
class VaultStatsRepositoryImpl(
    private val database: TheVaultDatabase,
) : VaultStatsRepository {

    private val queries = database.theVaultQueries

    override fun observeStats(): Flow<VaultStats> {
        val gamesFlow: Flow<Long> = queries.countOwnedGames().asFlow().mapToOne(Dispatchers.IO)
        val consolesFlow: Flow<Long> = queries.countOwnedConsoles().asFlow().mapToOne(Dispatchers.IO)
        val accessoriesFlow: Flow<Long> = queries.countOwnedAccessories().asFlow().mapToOne(Dispatchers.IO)
        val wishlistFlow: Flow<Long> = queries.countWishlistItems().asFlow().mapToOne(Dispatchers.IO)
        val valueFlow: Flow<Double> = queries.sumEstimatedValue().asFlow().mapToOne(Dispatchers.IO)
        val investedFlow: Flow<Double> = queries.sumTotalInvested().asFlow().mapToOne(Dispatchers.IO)

        return combine(
            gamesFlow, 
            consolesFlow, 
            accessoriesFlow, 
            wishlistFlow, 
            valueFlow
        ) { g, c, a, w, v ->
            // Temporarily store the first 5 in a tuple-like structure to combine with the 6th
            Quintet(g, c, a, w, v)
        }.combine(investedFlow) { q, i ->
            VaultStats(
                totalGames = q.g.toInt(),
                totalConsoles = q.c.toInt(),
                totalAccessories = q.a.toInt(),
                totalWishlist = q.w.toInt(),
                estimatedValue = q.v,
                totalInvested = i
            )
        }
    }

    private data class Quintet(val g: Long, val c: Long, val a: Long, val w: Long, val v: Double)
}
