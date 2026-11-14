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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ConsoleRepositoryImpl(
    private val database: TheVaultDatabase,
) : ConsoleRepository {
    private val queries = database.theVaultQueries

    override fun observeAll(): Flow<List<Console>> =
        queries
            .selectAllConsoles()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: String): Console? = queries.selectConsoleById(id).executeAsOneOrNull()?.toDomain()

    override fun observeByStatus(status: ItemStatus): Flow<List<Console>> =
        queries
            .selectConsoleByStatus(status.name)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun save(console: Console) {
        withContext(Dispatchers.IO) {
            val now = nowTimestamp()
            val createdAt = queries.selectConsoleById(console.id).executeAsOneOrNull()?.created_at ?: now
            queries.upsertConsole(console.toEntity(createdAt, now))
        }
    }

    override suspend fun delete(id: String) {
        queries.deleteConsoleById(id)
    }
}
