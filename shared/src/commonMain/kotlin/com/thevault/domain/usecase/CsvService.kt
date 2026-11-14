package com.thevault.domain.usecase

import com.thevault.domain.model.Completeness
import com.thevault.domain.model.Components
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import com.thevault.domain.repository.AccessoryRepository
import com.thevault.domain.repository.ConsoleRepository
import com.thevault.domain.repository.GameRepository
import kotlinx.coroutines.flow.first

/**
 * Import / export of the collection in the GameEye CSV format.
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
     * Imports a GameEye CSV export. Importing the same file again updates the items instead
     * of duplicating them, and keeps what the app added since (covers, years…).
     *
     * @return Number of imported rows.
     */
    suspend fun importCsv(csvContent: String): Result<Int> =
        try {
            val rows = CsvUtils.parseCsv(csvContent).drop(1).filter { it.size >= MIN_COLUMNS }
            if (rows.isEmpty()) {
                Result.failure(IllegalArgumentException("Fichier CSV vide"))
            } else {
                val ids =
                    ExistingIds(
                        gameRepository.observeAll().first(),
                        consoleRepository.observeAll().first(),
                        accessoryRepository.observeAll().first(),
                    )
                rows.forEach { importRow(it, ids) }
                Result.success(rows.size)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private suspend fun importRow(
        row: List<String>,
        ids: ExistingIds,
    ) {
        val r =
            CsvRow.of(
                row,
                parseStatus(row[2]),
                parseCondition(row[10]),
                parseRegion(row[4]),
                parseDouble(row.getOrNull(14)),
                parseDouble(row.getOrNull(15)),
            )
        when (r.category.lowercase()) {
            "games" -> {
                val existing = ids.take("game", r)?.let { gameRepository.getById(it) }
                gameRepository.save(r.toGame(existing ?: r.newGame(ids.newId("game", r))))
            }
            "systems" -> {
                val existing = ids.take("console", r)?.let { consoleRepository.getById(it) }
                consoleRepository.save(r.toConsole(existing ?: r.newConsole(ids.newId("console", r))))
            }
            else -> {
                val existing = ids.take("accessory", r)?.let { accessoryRepository.getById(it) }
                accessoryRepository.save(r.toAccessory(existing ?: r.newAccessory(ids.newId("accessory", r))))
            }
        }
    }

    /** Exports the whole collection in the GameEye CSV format. */
    suspend fun exportCsv(): Result<String> =
        try {
            val games = gameRepository.observeAll().first()
            val consoles = consoleRepository.observeAll().first()
            val accessories = accessoryRepository.observeAll().first()

            val headers =
                listOf(
                    "Platform",
                    "Category",
                    "UserRecordType",
                    "Title",
                    "Country",
                    "ReleaseType",
                    "Publisher",
                    "Developer",
                    "Genre",
                    "CreatedAt",
                    "Ownership",
                    "PriceLoose",
                    "PriceCIB",
                    "PriceNew",
                    "YourPrice",
                    "PricePaid",
                    "ItemCondition",
                    "BoxCondition",
                    "ManualCondition",
                    "Notes",
                    "Tags",
                    "metacritic",
                )

            val rows = mutableListOf<List<String>>()

            games.forEach { game ->
                val row = createEmptyRow(22)
                row[0] = game.platform
                row[1] = "Games"
                row[2] = exportStatus(game.status)
                row[3] = game.title
                row[4] = exportRegion(game.region)
                row[6] = game.publisher ?: ""
                row[7] = game.developer ?: ""
                row[8] = game.genre
                row[9] = game.acquiredAt?.toString() ?: ""
                row[10] = exportCondition(game.details.components)
                row[14] = game.currentValue?.let { formatDouble(it) } ?: ""
                row[15] = game.purchasePrice?.let { formatDouble(it) } ?: ""
                row[19] = game.notes ?: ""
                row[20] = game.themes ?: ""
                row[21] = game.rating?.toString() ?: ""
                rows.add(row)
            }

            consoles.forEach { console ->
                val row = createEmptyRow(22)
                row[0] = "Systems"
                row[1] = "Systems"
                row[2] = exportStatus(console.status)
                row[3] = console.name
                row[4] = exportRegion(console.region)
                row[6] = console.manufacturer
                row[9] = console.acquiredAt?.toString() ?: ""
                row[10] = exportCondition(console.details.components)
                row[14] = console.currentValue?.let { formatDouble(it) } ?: ""
                row[15] = console.purchasePrice?.let { formatDouble(it) } ?: ""
                row[19] = console.notes ?: ""
                rows.add(row)
            }

            accessories.forEach { acc ->
                val row = createEmptyRow(22)
                row[0] = acc.compatibleConsole
                row[1] = acc.type
                row[2] = exportStatus(acc.status)
                row[3] = acc.name
                row[9] = acc.acquiredAt?.toString() ?: ""
                row[10] = exportCondition(acc.details.components)
                row[14] = acc.currentValue?.let { formatDouble(it) } ?: ""
                row[15] = acc.purchasePrice?.let { formatDouble(it) } ?: ""
                row[19] = acc.notes ?: ""
                rows.add(row)
            }

            Result.success(CsvUtils.generateCsv(headers, rows))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }

    private fun createEmptyRow(size: Int): MutableList<String> {
        val list = mutableListOf<String>()
        for (i in 0 until size) list.add("")
        return list
    }

    private fun parseStatus(status: String): ItemStatus =
        when (status.lowercase()) {
            "wishlist" -> ItemStatus.WISHLIST
            "sold" -> ItemStatus.SOLD
            "loaned" -> ItemStatus.LOANED
            else -> ItemStatus.OWNED
        }

    private fun exportStatus(status: ItemStatus): String =
        when (status) {
            ItemStatus.WISHLIST -> "Wishlist"
            ItemStatus.SOLD -> "Sold"
            ItemStatus.LOANED -> "Loaned"
            ItemStatus.OWNED -> "Owned"
        }

    private fun parseCondition(condition: String): Components =
        when (condition.trim().lowercase()) {
            "loose" -> Components.LOOSE
            "loose+" -> Components(extras = true)
            "new", "sealed" -> Components.SEALED
            "boxed" -> Components(box = true)
            "cib+" -> Components(box = true, manual = true, extras = true)
            "dématérialisé", "digital" -> Components.DIGITAL
            else -> Components.CIB
        }

    private fun exportCondition(components: Components): String =
        when {
            components.digital -> "Dématérialisé"
            components.completeness == Completeness.NEW -> "New"
            components.box && components.manual -> if (components.extras) "CIB+" else "CIB"
            components.box -> "Boxed"
            else -> if (components.extras) "Loose+" else "Loose"
        }

    private fun parseRegion(region: String): Region =
        when (region.lowercase()) {
            "united kingdom", "europe", "france" -> Region.PAL
            "united states of america", "usa", "us" -> Region.NTSC_US
            "japan", "jp" -> Region.NTSC_JP
            else -> Region.OTHER
        }

    private fun exportRegion(region: Region): String =
        when (region) {
            Region.PAL -> "United Kingdom"
            Region.NTSC_US -> "United States of America"
            Region.NTSC_JP -> "Japan"
            Region.OTHER -> "World"
        }

    private fun parseDouble(value: String?): Double? {
        if (value.isNullOrBlank() || value == "?" || value == "-1.0" || value == "-1,0" || value == "Missing Field") return null
        return value
            .replace(" ", "")
            .replace(" ", "")
            .replace(",", ".")
            .toDoubleOrNull()
    }

    private fun formatDouble(value: Double): String {
        return value.toString().replace(".", ",") // format for french Excel
    }

    private companion object {
        const val MIN_COLUMNS = 21
    }
}
