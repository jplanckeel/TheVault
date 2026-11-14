package com.thevault.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.thevault.data.mapper.nowTimestamp
import com.thevault.database.Item_set
import com.thevault.database.Set_entry
import com.thevault.database.TheVaultDatabase
import com.thevault.domain.model.ItemSet
import com.thevault.domain.model.SetEntry
import com.thevault.domain.repository.SetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/**
 * [SetRepository] stored in SQLite.
 *
 * @param database Local database.
 */
class SetRepositoryImpl(
    database: TheVaultDatabase,
) : SetRepository {
    private val queries = database.theVaultQueries

    override fun observe(): Flow<List<ItemSet>> =
        combine(
            queries.selectSets().asFlow().mapToList(Dispatchers.IO),
            queries.selectSetEntries().asFlow().mapToList(Dispatchers.IO),
        ) { sets, entries ->
            val bySet = entries.groupBy({ it.set_id }, { SetEntry(it.title, it.platform) })
            sets.map { ItemSet(it.id, it.name, bySet[it.id].orEmpty().sortedBy { e -> e.title.lowercase() }) }
        }

    override suspend fun save(set: ItemSet) {
        withContext(Dispatchers.IO) {
            queries.transaction {
                queries.upsertSet(Item_set(set.id, set.name.trim(), nowTimestamp()))
                queries.deleteEntriesOfSet(set.id)
                set.entries.forEach { queries.insertSetEntry(Set_entry(set.id, it.title.trim(), it.platform.trim())) }
            }
        }
    }

    override suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            queries.transaction {
                queries.deleteEntriesOfSet(id)
                queries.deleteSet(id)
            }
        }
    }

    override suspend fun addEntry(
        setId: String,
        entry: SetEntry,
    ) {
        withContext(Dispatchers.IO) { queries.insertSetEntry(Set_entry(setId, entry.title.trim(), entry.platform.trim())) }
    }

    override suspend fun removeEntry(
        setId: String,
        entry: SetEntry,
    ) {
        withContext(Dispatchers.IO) { queries.deleteSetEntry(setId, entry.title, entry.platform) }
    }
}
