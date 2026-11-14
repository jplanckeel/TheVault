package com.thevault.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.thevault.data.remote.ItemSyncRemoteDataSource
import com.thevault.data.remote.PriceRemoteDataSource
import com.thevault.data.remote.dto.QuoteDto
import com.thevault.database.Achievement_unlock
import com.thevault.database.App_setting
import com.thevault.database.Price_quote
import com.thevault.database.Price_snapshot
import com.thevault.database.Sync_state
import com.thevault.database.TheVaultDatabase
import com.thevault.database.Value_snapshot
import com.thevault.domain.model.AchievementUnlock
import com.thevault.domain.model.PriceGrade
import com.thevault.domain.model.PricePoint
import com.thevault.domain.model.PriceQuote
import com.thevault.domain.model.PriceSnapshot
import com.thevault.domain.model.PriceSource
import com.thevault.domain.model.SyncOutcome
import com.thevault.domain.model.SyncPayload
import com.thevault.domain.model.ValueSnapshot
import com.thevault.domain.repository.AchievementRepository
import com.thevault.domain.repository.CatalogSyncRepository
import com.thevault.domain.repository.PriceRepository
import com.thevault.domain.repository.SettingsRepository
import com.thevault.domain.repository.ValueHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * [PriceRepository] caching the backend quotes in SQLite.
 *
 * @param database Local database.
 * @param remote Backend price API.
 */
class PriceRepositoryImpl(
    private val database: TheVaultDatabase,
    private val remote: PriceRemoteDataSource,
    private val clock: Clock = Clock.System,
) : PriceRepository {
    private val queries = database.insightsQueries

    override fun observeQuotes(): Flow<Map<String, List<PriceQuote>>> =
        queries
            .selectAllQuotes()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.mapNotNull { it.toDomain() }.groupBy { it.itemId } }

    override suspend fun refreshQuotes(itemIds: List<String>): Int {
        val quotes = remote.quotes(itemIds).mapNotNull { it.toDomain() }
        val today = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        withContext(Dispatchers.IO) {
            queries.transaction {
                queries.deleteAllQuotes()
                quotes.forEach { queries.insertQuote(it.toEntity()) }
                quotes.groupBy { it.itemId }.forEach { (id, q) -> queries.upsertPriceSnapshot(palSnapshot(id, today, q).toEntity()) }
            }
        }
        return quotes.size
    }

    override fun observeSnapshots(): Flow<Map<String, List<PriceSnapshot>>> =
        queries
            .selectAllSnapshots()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.mapNotNull { it.toDomain() }.groupBy { it.itemId } }

    override suspend fun replaceImportedPrices(snapshots: List<PriceSnapshot>) {
        withContext(Dispatchers.IO) {
            queries.transaction {
                queries.deleteSnapshotsOfSource(PriceSource.GAMEYE_US.key)
                snapshots.forEach { queries.upsertPriceSnapshot(it.toEntity()) }
            }
        }
    }

    override suspend fun history(itemId: String): List<PricePoint> =
        remote
            .history(itemId)
            .mapNotNull { dto ->
                val grade = PriceGrade.fromApi(dto.condition) ?: return@mapNotNull null
                val at = parseInstant(dto.scrapedAt) ?: return@mapNotNull null
                PricePoint(at, grade, dto.priceMedian)
            }.sortedBy { it.at }
}

/** PAL snapshot of the quotes of one item. */
internal fun palSnapshot(
    itemId: String,
    day: LocalDate,
    quotes: List<PriceQuote>,
): PriceSnapshot {
    fun median(grade: PriceGrade) = quotes.find { it.grade == grade }?.median
    return PriceSnapshot(
        itemId,
        day,
        PriceSource.EBAY_PAL,
        median(
            PriceGrade.LOOSE,
        ),
        median(PriceGrade.CIB),
        median(PriceGrade.NEW),
        quotes.sumOf {
            it.samples
        },
    )
}

private fun Price_snapshot.toDomain(): PriceSnapshot? {
    val source = PriceSource.fromKey(source) ?: return null
    val date = runCatching { LocalDate.parse(day) }.getOrNull() ?: return null
    return PriceSnapshot(item_id, date, source, price_loose, price_cib, price_new, sample_size.toInt())
}

private fun PriceSnapshot.toEntity() = Price_snapshot(itemId, day.toString(), source.key, loose, cib, new, "EUR", sampleSize.toLong())

/** Maps a backend quote; null when its condition or date is unknown. */
internal fun QuoteDto.toDomain(): PriceQuote? {
    val grade = PriceGrade.fromApi(condition) ?: return null
    val at = parseInstant(scrapedAt) ?: return null
    return PriceQuote(externalId, grade, priceMedian, priceMin, priceMax, sampleCount, at, pricePast?.takeIf { it > 0.0 })
}

private fun parseInstant(value: String): Instant? = runCatching { Instant.parse(value) }.getOrNull()

private fun Price_quote.toDomain(): PriceQuote? {
    val g = PriceGrade.fromApi(grade) ?: return null
    val at = parseInstant(scraped_at) ?: return null
    return PriceQuote(item_id, g, median, price_min, price_max, samples.toInt(), at, median_past)
}

private fun PriceQuote.toEntity() =
    Price_quote(itemId, grade.apiValue, median, min, max, samples.toLong(), scrapedAt.toString(), medianPast)

/**
 * [ValueHistoryRepository] stored in SQLite.
 *
 * @param database Local database.
 */
class ValueHistoryRepositoryImpl(
    private val database: TheVaultDatabase,
) : ValueHistoryRepository {
    private val queries = database.insightsQueries

    override fun observe(): Flow<List<ValueSnapshot>> =
        queries
            .selectSnapshots()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.mapNotNull { row -> parseDay(row.day)?.let { ValueSnapshot(it, row.total_value, row.invested) } } }

    override suspend fun record(snapshot: ValueSnapshot) {
        withContext(Dispatchers.IO) {
            queries.upsertSnapshot(Value_snapshot(snapshot.day.toString(), snapshot.value, snapshot.invested))
        }
    }

    private fun parseDay(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()
}

/**
 * [AchievementRepository] stored in SQLite.
 *
 * @param database Local database.
 */
class AchievementRepositoryImpl(
    private val database: TheVaultDatabase,
) : AchievementRepository {
    private val queries = database.insightsQueries

    override fun observe(): Flow<List<AchievementUnlock>> =
        queries
            .selectUnlocks()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                rows.mapNotNull { row -> parseInstant(row.unlocked_at)?.let { AchievementUnlock(row.id, it, row.seen != 0L) } }
            }

    override suspend fun record(unlocks: List<AchievementUnlock>) {
        withContext(Dispatchers.IO) {
            queries.transaction {
                unlocks.forEach { queries.insertUnlock(Achievement_unlock(it.id, it.unlockedAt.toString(), if (it.seen) 1L else 0L)) }
            }
        }
    }

    override suspend fun markSeen(id: String) {
        withContext(Dispatchers.IO) { queries.markUnlockSeen(id) }
    }
}

/**
 * [SettingsRepository] stored in SQLite.
 *
 * @param database Local database.
 */
class SettingsRepositoryImpl(
    private val database: TheVaultDatabase,
) : SettingsRepository {
    private val queries = database.insightsQueries

    override fun observe(key: String): Flow<String?> = queries.selectSetting(key).asFlow().mapToOneOrNull(Dispatchers.IO)

    override suspend fun get(key: String): String? = observe(key).first()

    override suspend fun set(
        key: String,
        value: String,
    ) {
        withContext(Dispatchers.IO) { queries.upsertSetting(App_setting(key, value)) }
    }
}

/**
 * [CatalogSyncRepository] keeping the pushed identifiers in SQLite.
 *
 * @param database Local database.
 * @param remote Backend catalogue API.
 * @param clock Time source.
 */
class CatalogSyncRepositoryImpl(
    private val database: TheVaultDatabase,
    private val remote: ItemSyncRemoteDataSource,
    private val clock: Clock = Clock.System,
) : CatalogSyncRepository {
    private val queries = database.insightsQueries

    override fun observeSyncedIds(): Flow<Set<String>> =
        queries
            .selectSyncedIds()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.toSet() }

    override suspend fun markDirty(itemId: String) {
        withContext(Dispatchers.IO) { queries.markDirty(itemId) }
    }

    override suspend fun push(payload: SyncPayload): SyncOutcome {
        val outcome = remote.push(payload)
        if (outcome != SyncOutcome.Failed) {
            withContext(Dispatchers.IO) { queries.markSynced(Sync_state(payload.externalId, clock.now().toString())) }
        }
        return outcome
    }
}
