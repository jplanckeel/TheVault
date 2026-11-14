package com.thevault.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.thevault.data.mapper.nowTimestamp
import com.thevault.data.mapper.toDomain
import com.thevault.data.mapper.toEntity
import com.thevault.database.TheVaultDatabase
import com.thevault.domain.model.Console
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.repository.ConsoleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * [ConsoleRepository] stored in SQLite, tags included.
 *
 * @param database Local database.
 */
class ConsoleRepositoryImpl(
    database: TheVaultDatabase,
) : ConsoleRepository {
    private val queries = database.theVaultQueries

    override fun observeAll(): Flow<List<Console>> =
        queries
            .selectAllConsoles()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .withTags(queries.observeTags(), { it.id }) { row, tags -> row.toDomain(tags) }

    override suspend fun getById(id: String): Console? =
        withContext(Dispatchers.IO) { queries.selectConsoleById(id).executeAsOneOrNull()?.toDomain(queries.tagsOf(id)) }

    override fun observeByStatus(status: ItemStatus): Flow<List<Console>> =
        queries
            .selectConsoleByStatus(status.name)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .withTags(queries.observeTags(), { it.id }) { row, tags -> row.toDomain(tags) }

    override suspend fun save(console: Console) {
        withContext(Dispatchers.IO) {
            queries.transaction {
                val now = nowTimestamp()
                val createdAt = queries.selectConsoleById(console.id).executeAsOneOrNull()?.created_at ?: now
                queries.upsertConsole(console.toEntity(createdAt, now))
                queries.replaceTags(console.id, console.details.tags)
            }
        }
    }

    override suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            queries.transaction {
                queries.deleteConsoleById(id)
                queries.forgetItem(id)
            }
        }
    }
}
