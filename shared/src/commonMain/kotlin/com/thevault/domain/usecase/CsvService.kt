package com.thevault.domain.usecase

import com.thevault.domain.model.Accessory
import com.thevault.domain.model.Console
import com.thevault.domain.model.Game
import com.thevault.domain.repository.AccessoryRepository
import com.thevault.domain.repository.ConsoleRepository
import com.thevault.domain.repository.GameRepository
import com.thevault.domain.usecase.GameyeFormat.Col
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Result of a CSV import.
 *
 * @property rows Imported rows.
 * @property created Items created.
 * @property updated Items updated (already in the collection).
 * @property usPrices US prices of the export, by item identifier (stored as `gameye_import_us`).
 */
data class ImportReport(
    val rows: Int,
    val created: Int,
    val updated: Int,
    val usPrices: Map<String, UsPrices>,
)

/**
 * Import / export of the collection in the Gameye CSV format.
 *
 * @param gameRepository Games.
 * @param consoleRepository Consoles.
 * @param accessoryRepository Accessories.
 */
class CsvService(
    private val gameRepository: GameRepository,
    private val consoleRepository: ConsoleRepository,
    private val accessoryRepository: AccessoryRepository,
) {
    /**
     * Imports a Gameye export. Importing the same file again updates the items instead of
     * duplicating them, and keeps what the app added since (covers, years, tags, location…).
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun importCsv(csvContent: String): Result<ImportReport> =
        try {
            val rows =
                CsvUtils
                    .parseCsv(csvContent)
                    .drop(1)
                    .filter { it.size >= CsvRow.MIN_COLUMNS }
                    .map { CsvRow.parse(it) }
            if (rows.isEmpty()) {
                Result.failure(IllegalArgumentException("Fichier CSV vide"))
            } else {
                Result.success(importRows(rows))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private suspend fun importRows(rows: List<CsvRow>): ImportReport {
        val ids =
            ExistingIds(
                gameRepository.observeAll().first(),
                consoleRepository.observeAll().first(),
                accessoryRepository.observeAll().first(),
            )
        var updated = 0
        val prices = mutableMapOf<String, UsPrices>()
        rows.forEach { row ->
            val existing = ids.take(row)
            if (existing != null) updated++
            val id = existing ?: ids.newId(row)
            save(row, id, existing != null)
            if (row.usPrices.isKnown) prices[id] = row.usPrices
        }
        return ImportReport(rows.size, rows.size - updated, updated, prices)
    }

    private suspend fun save(
        row: CsvRow,
        id: String,
        exists: Boolean,
    ) {
        when (row.kind) {
            "game" -> gameRepository.save((if (exists) gameRepository.getById(id) else null)?.let(row::toGame) ?: row.newGame(id))
            "console" ->
                consoleRepository.save(
                    (if (exists) consoleRepository.getById(id) else null)?.let(row::toConsole) ?: row.newConsole(id),
                )
            else ->
                accessoryRepository.save(
                    (if (exists) accessoryRepository.getById(id) else null)?.let(row::toAccessory) ?: row.newAccessory(id),
                )
        }
    }

    /**
     * Exports the whole collection in the Gameye format.
     *
     * @param usPrices US prices to write back in PriceLoose / PriceCIB / PriceNew, by item.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun exportCsv(usPrices: Map<String, UsPrices> = emptyMap()): Result<String> =
        try {
            val rows =
                gameRepository.observeAll().first().map { it.toRow(usPrices[it.id]) } +
                    consoleRepository.observeAll().first().map { it.toRow(usPrices[it.id]) } +
                    accessoryRepository.observeAll().first().map { it.toRow(usPrices[it.id]) }
            Result.success(CsvUtils.generateCsv(GameyeFormat.HEADERS, rows))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}

private fun row(fill: MutableList<String>.() -> Unit): List<String> = MutableList(GameyeFormat.HEADERS.size) { "" }.apply(fill)

private fun MutableList<String>.copyColumns(
    details: com.thevault.domain.model.CopyDetails,
    prices: UsPrices?,
    paid: Double?,
    notes: String?,
) {
    val f = GameyeFormat
    val c = details.components
    this[Col.COUNTRY] = "Europe"
    this[Col.RELEASE_TYPE] = if (c.digital) "Digital" else "Official"
    this[Col.OWNERSHIP] = f.ownership(c)
    this[Col.PRICE_LOOSE] = f.formatAmount(prices?.loose)
    this[Col.PRICE_CIB] = f.formatAmount(prices?.cib)
    this[Col.PRICE_NEW] = f.formatAmount(prices?.new)
    this[Col.YOUR_PRICE] = f.formatAmount(null)
    this[Col.PRICE_PAID] = f.formatAmount(paid)
    this[Col.ITEM_CONDITION] = f.formatGrade(c.mediaGrade)
    this[Col.BOX_CONDITION] = f.formatGrade(c.boxGrade)
    this[Col.MANUAL_CONDITION] = f.formatGrade(c.manualGrade)
    this[Col.NOTES] = notes.orEmpty()
    this[Col.TAGS] = details.tags.joinToString(", ")
}

private fun Game.toRow(prices: UsPrices?) =
    row {
        this[Col.PLATFORM] = platform
        this[Col.CATEGORY] = "Games"
        this[Col.RECORD_TYPE] = GameyeFormat.formatStatus(status)
        this[Col.TITLE] = title
        this[Col.PUBLISHER] = publisher.orEmpty()
        this[Col.DEVELOPER] = developer.orEmpty()
        this[Col.GENRE] = genre.takeIf { it != "N/A" }.orEmpty()
        this[Col.CREATED_AT] = GameyeFormat.formatDate(acquiredAt)
        copyColumns(details, prices, purchasePrice, notes)
    }

private fun Console.toRow(prices: UsPrices?) =
    row {
        this[Col.PLATFORM] = name
        this[Col.CATEGORY] = "Systems"
        this[Col.RECORD_TYPE] = GameyeFormat.formatStatus(status)
        this[Col.TITLE] = name
        this[Col.PUBLISHER] = manufacturer
        this[Col.CREATED_AT] = GameyeFormat.formatDate(acquiredAt)
        copyColumns(details, prices, purchasePrice, notes)
    }

private fun Accessory.toRow(prices: UsPrices?) =
    row {
        this[Col.PLATFORM] = compatibleConsole
        this[Col.CATEGORY] = type
        this[Col.RECORD_TYPE] = GameyeFormat.formatStatus(status)
        this[Col.TITLE] = name
        this[Col.CREATED_AT] = GameyeFormat.formatDate(acquiredAt)
        copyColumns(details, prices, purchasePrice, notes)
    }
