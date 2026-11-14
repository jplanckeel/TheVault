package com.thevault.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.thevault.data.mapper.nowTimestamp
import com.thevault.data.mapper.toDomain
import com.thevault.data.mapper.toEntity
import com.thevault.database.TheVaultDatabase
import com.thevault.domain.model.Accessory
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.repository.AccessoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AccessoryRepositoryImpl(
    private val database: TheVaultDatabase,
) : AccessoryRepository {
    private val queries = database.theVaultQueries

    override fun observeAll(): Flow<List<Accessory>> =
        queries
            .selectAllAccessories()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: String): Accessory? = queries.selectAccessoryById(id).executeAsOneOrNull()?.toDomain()

    override fun observeByStatus(status: ItemStatus): Flow<List<Accessory>> =
        queries
            .selectAccessoryByStatus(status.name)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun save(accessory: Accessory) {
        withContext(Dispatchers.IO) {
            val now = nowTimestamp()
            val createdAt = queries.selectAccessoryById(accessory.id).executeAsOneOrNull()?.created_at ?: now
            queries.upsertAccessory(accessory.toEntity(createdAt, now))
        }
    }

    override suspend fun delete(id: String) {
        queries.deleteAccessoryById(id)
    }
}
