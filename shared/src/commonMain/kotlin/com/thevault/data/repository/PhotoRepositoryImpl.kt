package com.thevault.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.thevault.database.Item_photo
import com.thevault.database.TheVaultDatabase
import com.thevault.domain.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * [PhotoRepository] backed by the `item_photo` table. An item keeps a single photo: the
 * latest one replaces the previous ones.
 *
 * @param database Local database.
 * @param clock Time source.
 */
class PhotoRepositoryImpl(
    private val database: TheVaultDatabase,
    private val clock: Clock = Clock.System,
) : PhotoRepository {
    private val queries = database.theVaultQueries

    override fun observe(): Flow<Map<String, String>> =
        queries
            .selectAllPhotos()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.associate { it.item_id to it.uri } }

    override suspend fun set(
        itemId: String,
        uri: String,
    ): List<String> =
        withContext(Dispatchers.IO) {
            database.transactionWithResult {
                val previous = queries.selectPhotosOf(itemId).executeAsList().map { it.uri }
                queries.deletePhotosOf(itemId)
                val now = clock.now()
                queries.insertPhoto(Item_photo("$itemId-${now.toEpochMilliseconds()}", itemId, uri, now.toString()))
                previous.filter { it != uri }
            }
        }

    override suspend fun remove(itemId: String): List<String> =
        withContext(Dispatchers.IO) {
            database.transactionWithResult {
                val previous = queries.selectPhotosOf(itemId).executeAsList().map { it.uri }
                queries.deletePhotosOf(itemId)
                previous
            }
        }
}
