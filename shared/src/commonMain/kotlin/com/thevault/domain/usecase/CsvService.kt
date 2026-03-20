package com.thevault.domain.usecase

import com.thevault.domain.model.Accessory
import com.thevault.domain.model.Console
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemCondition
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import com.thevault.domain.repository.AccessoryRepository
import com.thevault.domain.repository.ConsoleRepository
import com.thevault.domain.repository.GameRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class CsvService(
    private val gameRepository: GameRepository,
    private val consoleRepository: ConsoleRepository,
    private val accessoryRepository: AccessoryRepository,
) {
    suspend fun importCsv(csvContent: String): Result<Unit> {
        return try {
            val rows = CsvUtils.parseCsv(csvContent)
            if (rows.isEmpty()) return Result.failure(Exception("Fichier CSV vide"))
            
            // Skip the header (first row)
            for (row in rows.drop(1)) {
                if (row.size < 21) continue // Skip invalid rows
                
                val platform = row.getOrNull(0) ?: ""
                val category = row.getOrNull(1) ?: ""
                val statusString = row.getOrNull(2) ?: ""
                val title = row.getOrNull(3) ?: "Unknown"
                val country = row.getOrNull(4) ?: ""
                val publisher = row.getOrNull(6) ?: ""
                val developer = row.getOrNull(7) ?: ""
                val genre = row.getOrNull(8) ?: ""
                // Row 9 is CreatedAt
                val ownershipCondition = row.getOrNull(10) ?: ""
                val currentValueString = row.getOrNull(14)
                val purchasePriceString = row.getOrNull(15)
                val notes = row.getOrNull(19) ?: ""
                val tags = row.getOrNull(20) ?: ""
                val rating = row.getOrNull(21)?.toIntOrNull()

                val status = parseStatus(statusString)
                val condition = parseCondition(ownershipCondition)
                val region = parseRegion(country)
                
                val currentValue = parseDouble(currentValueString)
                val purchasePrice = parseDouble(purchasePriceString)

                // Generate a random ID (or unique based on title + platform)
                val id = (title + platform + category).hashCode().toString() + Clock.System.now().toEpochMilliseconds()

                when (category.lowercase()) {
                    "games" -> {
                        val game = Game(
                            id = id,
                            title = title,
                            platform = platform,
                            genre = genre,
                            releaseYear = 0,
                            region = region,
                            condition = condition,
                            status = status,
                            purchasePrice = purchasePrice,
                            currentValue = currentValue,
                            notes = notes,
                            rating = rating,
                            developer = developer,
                            publisher = publisher,
                            themes = tags,
                            isCollectorEdition = notes.contains("collector", ignoreCase = true),
                            isLimitedEdition = notes.contains("limited", ignoreCase = true) || notes.contains("limitée", ignoreCase = true)
                        )
                        gameRepository.save(game)
                    }
                    "systems" -> {
                        val console = Console(
                            id = id,
                            name = title,
                            manufacturer = publisher.ifEmpty { "Unknown" },
                            region = region,
                            color = "Unknown",
                            condition = condition,
                            productionYear = 0,
                            status = status,
                            purchasePrice = purchasePrice,
                            currentValue = currentValue,
                            notes = notes,
                            isCollectorEdition = notes.contains("collector", ignoreCase = true),
                            isLimitedEdition = notes.contains("limited", ignoreCase = true) || notes.contains("limitée", ignoreCase = true)
                        )
                        consoleRepository.save(console)
                    }
                    else -> { // Accessories, Controllers, Strategy Guide...
                        val accessory = Accessory(
                            id = id,
                            name = title,
                            type = category,
                            compatibleConsole = platform,
                            condition = condition,
                            status = status,
                            purchasePrice = purchasePrice,
                            currentValue = currentValue,
                            notes = notes,
                            isCollectorEdition = notes.contains("collector", ignoreCase = true),
                            isLimitedEdition = notes.contains("limited", ignoreCase = true) || notes.contains("limitée", ignoreCase = true)
                        )
                        accessoryRepository.save(accessory)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun exportCsv(): Result<String> {
        return try {
            val games = gameRepository.observeAll().first()
            val consoles = consoleRepository.observeAll().first()
            val accessories = accessoryRepository.observeAll().first()

            val headers = listOf(
                "Platform", "Category", "UserRecordType", "Title", "Country",
                "ReleaseType", "Publisher", "Developer", "Genre", "CreatedAt",
                "Ownership", "PriceLoose", "PriceCIB", "PriceNew", "YourPrice",
                "PricePaid", "ItemCondition", "BoxCondition", "ManualCondition", "Notes",
                "Tags", "metacritic"
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
                row[10] = exportCondition(game.condition)
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
                row[10] = exportCondition(console.condition)
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
                row[10] = exportCondition(acc.condition)
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
    }

    private fun createEmptyRow(size: Int): MutableList<String> {
        val list = mutableListOf<String>()
        for (i in 0 until size) list.add("")
        return list
    }

    private fun parseStatus(status: String): ItemStatus {
        return when (status.lowercase()) {
            "wishlist" -> ItemStatus.WISHLIST
            "sold" -> ItemStatus.SOLD
            "loaned" -> ItemStatus.LOANED
            else -> ItemStatus.OWNED
        }
    }

    private fun exportStatus(status: ItemStatus): String {
        return when (status) {
            ItemStatus.WISHLIST -> "Wishlist"
            ItemStatus.SOLD -> "Sold"
            ItemStatus.LOANED -> "Loaned"
            ItemStatus.OWNED -> "Owned"
        }
    }

    private fun parseCondition(condition: String): ItemCondition {
        return when (condition.lowercase()) {
            "loose" -> ItemCondition.LOOSE
            "new", "sealed" -> ItemCondition.NEW_SEALED
            "boxed", "o", "cib+" -> ItemCondition.CIB_NO_MANUAL
            "dématérialisé", "digital" -> ItemCondition.GOOD // Fallback
            else -> ItemCondition.CIB
        }
    }

    private fun exportCondition(condition: ItemCondition): String {
        return when (condition) {
            ItemCondition.LOOSE -> "Loose"
            ItemCondition.NEW_SEALED -> "New"
            ItemCondition.CIB_NO_MANUAL -> "Boxed"
            ItemCondition.CIB -> "CIB"
            else -> "CIB"
        }
    }

    private fun parseRegion(region: String): Region {
        return when (region.lowercase()) {
            "united kingdom", "europe", "france" -> Region.PAL
            "united states of america", "usa", "us" -> Region.NTSC_US
            "japan", "jp" -> Region.NTSC_JP
            else -> Region.OTHER
        }
    }

    private fun exportRegion(region: Region): String {
        return when (region) {
            Region.PAL -> "United Kingdom"
            Region.NTSC_US -> "United States of America"
            Region.NTSC_JP -> "Japan"
            Region.OTHER -> "World"
        }
    }

    private fun parseDouble(value: String?): Double? {
        if (value.isNullOrBlank() || value == "?" || value == "-1.0" || value == "-1,0" || value == "Missing Field") return null
        return value.replace(" ", "").replace(" ", "").replace(",", ".").toDoubleOrNull()
    }

    private fun formatDouble(value: Double): String {
        return value.toString().replace(".", ",") // format for french Excel
    }
}
