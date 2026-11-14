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
import kotlinx.coroutines.withContext

/**
 * [GameRepository] stored in SQLite, tags included.
 *
 * @param database Local database.
 */
class GameRepositoryImpl(
    database: TheVaultDatabase,
) : GameRepository {
    private val queries = database.theVaultQueries

    override fun observeAll(): Flow<List<Game>> =
        queries
            .selectAll()
            .asFlow()
            .mapToList(
                Dispatchers.IO,
            ).withTags(queries.observeTags(), { it.id }) { row, tags -> row.toDomain(tags) }

    override suspend fun getById(id: String): Game? =
        withContext(Dispatchers.IO) { queries.selectById(id).executeAsOneOrNull()?.toDomain(queries.tagsOf(id)) }

    override fun observeByStatus(status: ItemStatus): Flow<List<Game>> =
        queries
            .selectByStatus(status.name)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .withTags(queries.observeTags(), { it.id }) { row, tags -> row.toDomain(tags) }

    override suspend fun save(game: Game) {
        withContext(Dispatchers.IO) {
            queries.transaction {
                val now = nowTimestamp()
                val createdAt = queries.selectById(game.id).executeAsOneOrNull()?.created_at ?: now
                queries.upsert(game.toEntity(createdAt, now))
                queries.replaceTags(game.id, game.details.tags)
            }
        }
    }

    override suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            queries.transaction {
                queries.deleteById(id)
                queries.forgetItem(id)
            }
        }
    }

    override fun search(query: String): Flow<List<Game>> =
        queries
            .search("%$query%")
            .asFlow()
            .mapToList(Dispatchers.IO)
            .withTags(queries.observeTags(), { it.id }) { row, tags -> row.toDomain(tags) }
}
