package com.thevault.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.thevault.data.mapper.toDomain
import com.thevault.database.TheVaultDatabase
import com.thevault.domain.model.Accessory
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.repository.AccessoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccessoryRepositoryImpl(
    private val database: TheVaultDatabase,
) : AccessoryRepository {

    private val queries = database.theVaultQueries

    override fun observeAll(): Flow<List<Accessory>> {
        return queries.selectAllAccessories()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getById(id: String): Accessory? {
        return queries.selectAccessoryById(id).executeAsOneOrNull()?.toDomain()
    }

    override fun observeByStatus(status: ItemStatus): Flow<List<Accessory>> {
        return queries.selectAccessoryByStatus(status.name)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun save(accessory: Accessory) {
        // Implementation for saving (upsertAccessory)
    }

    override suspend fun delete(id: String) {
        queries.deleteAccessoryById(id)
    }
}
