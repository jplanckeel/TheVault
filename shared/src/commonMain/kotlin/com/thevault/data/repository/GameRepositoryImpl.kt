package com.thevault.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.thevault.data.mapper.nowTimestamp
import com.thevault.data.mapper.toDomain
import com.thevault.data.mapper.toEntity
import com.thevault.database.TheVaultDatabase
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.repository.GameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GameRepositoryImpl(
    private val database: TheVaultDatabase,
) : GameRepository {
    private val queries = database.theVaultQueries

    override fun observeAll(): Flow<List<Game>> =
        queries
            .selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: String): Game? = queries.selectById(id).executeAsOneOrNull()?.toDomain()

    override fun observeByStatus(status: ItemStatus): Flow<List<Game>> =
        queries
            .selectByStatus(status.name)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun save(game: Game) {
        withContext(Dispatchers.IO) {
            val now = nowTimestamp()
            val createdAt = queries.selectById(game.id).executeAsOneOrNull()?.created_at ?: now
            queries.upsert(game.toEntity(createdAt, now))
        }
    }

    override suspend fun delete(id: String) {
        queries.deleteById(id)
    }

    override fun search(query: String): Flow<List<Game>> =
        queries
            .search("%$query%")
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }
}
