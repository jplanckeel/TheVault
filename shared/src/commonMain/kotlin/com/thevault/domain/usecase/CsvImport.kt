package com.thevault.domain.usecase

import com.thevault.domain.model.Accessory
import com.thevault.domain.model.Console
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemCondition
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import kotlinx.datetime.LocalDate

/**
 * One parsed row of a GameEye CSV export.
 *
 * @property platform Platform.
 * @property category `Games`, `Systems` or an accessory category.
 * @property title Title.
 * @property publisher Publisher (manufacturer for systems).
 * @property developer Developer.
 * @property genre Genre.
 * @property createdAt Date the item was added in GameEye.
 * @property notes Notes.
 * @property tags Tags.
 * @property rating Rating.
 * @property status Status.
 * @property condition Condition.
 * @property region Region.
 * @property value Value.
 * @property price Purchase price.
 */
internal data class CsvRow(
    val platform: String,
    val category: String,
    val title: String,
    val publisher: String,
    val developer: String,
    val genre: String,
    val createdAt: LocalDate?,
    val notes: String,
    val tags: String,
    val rating: Int?,
    val status: ItemStatus,
    val condition: ItemCondition,
    val region: Region,
    val value: Double?,
    val price: Double?,
) {
    private val collector get() = notes.contains("collector", ignoreCase = true)
    private val limited get() = notes.contains("limited", ignoreCase = true) || notes.contains("limitée", ignoreCase = true)

    /** Identity of the row, shared by the copies of the same item. */
    fun key(kind: String): String = listOf(kind, title, platform).joinToString("|") { it.trim().lowercase() }

    /** Blank game with [id]. */
    fun newGame(id: String) = Game(id, title, platform, genre, 0, region, condition, status = status, acquiredAt = createdAt)

    /** Blank console with [id]. */
    fun newConsole(id: String) =
        Console(id, title, publisher.ifEmpty { "Unknown" }, region, "", condition, 0, status, acquiredAt = createdAt)

    /** Blank accessory with [id]. */
    fun newAccessory(id: String) = Accessory(id, title, category, platform, condition, status, acquiredAt = createdAt)

    /** CSV fields applied to [base]; cover, year and app-only fields are kept. */
    fun toGame(base: Game) =
        base.copy(
            title = title,
            platform = platform,
            genre = genre.ifBlank { base.genre },
            region = region,
            condition = condition,
            status = status,
            purchasePrice = price ?: base.purchasePrice,
            currentValue = value ?: base.currentValue,
            notes = notes.ifBlank { base.notes },
            rating = rating ?: base.rating,
            developer = developer.ifBlank { base.developer },
            publisher = publisher.ifBlank { base.publisher },
            themes = tags.ifBlank { base.themes },
            acquiredAt = base.acquiredAt ?: createdAt,
            isCollectorEdition = base.isCollectorEdition || collector,
            isLimitedEdition = base.isLimitedEdition || limited,
        )

    /** CSV fields applied to [base]. */
    fun toConsole(base: Console) =
        base.copy(
            name = title,
            region = region,
            condition = condition,
            status = status,
            purchasePrice = price ?: base.purchasePrice,
            currentValue = value ?: base.currentValue,
            notes = notes.ifBlank { base.notes },
            acquiredAt = base.acquiredAt ?: createdAt,
            isCollectorEdition = base.isCollectorEdition || collector,
            isLimitedEdition = base.isLimitedEdition || limited,
        )

    /** CSV fields applied to [base]. */
    fun toAccessory(base: Accessory) =
        base.copy(
            name = title,
            type = category,
            compatibleConsole = platform,
            condition = condition,
            status = status,
            purchasePrice = price ?: base.purchasePrice,
            currentValue = value ?: base.currentValue,
            notes = notes.ifBlank { base.notes },
            acquiredAt = base.acquiredAt ?: createdAt,
        )

    /** Companion helpers. */
    companion object {
        /** Builds a row from raw columns and the already parsed enums / amounts. */
        @Suppress("LongParameterList")
        fun of(
            row: List<String>,
            status: ItemStatus,
            condition: ItemCondition,
            region: Region,
            value: Double?,
            price: Double?,
        ) = CsvRow(
            platform = row[0],
            category = row[1],
            title = row[3].ifBlank { "Unknown" },
            publisher = row[6],
            developer = row[7],
            genre = row[8],
            createdAt = parseDate(row[9]),
            notes = row.getOrNull(19).orEmpty(),
            tags = row.getOrNull(20).orEmpty(),
            rating = row.getOrNull(21)?.toIntOrNull(),
            status = status,
            condition = condition,
            region = region,
            value = value,
            price = price,
        )

        /** First 10 characters as an ISO date ("2021-03-24 10:12:00" → 2021-03-24). */
        fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value.trim().take(ISO_DATE)) }.getOrNull()

        private const val ISO_DATE = 10
    }
}

/**
 * Matches CSV rows to the items already in the collection (same kind, title and platform),
 * so that importing twice updates instead of duplicating.
 */
internal class ExistingIds(
    games: List<Game>,
    consoles: List<Console>,
    accessories: List<Accessory>,
) {
    private val free: MutableMap<String, ArrayDeque<String>> = mutableMapOf()
    private val seen: MutableMap<String, Int> = mutableMapOf()

    init {
        games.forEach { add("game", it.title, it.platform, it.id) }
        consoles.forEach { add("console", it.name, it.name, it.id) }
        accessories.forEach { add("accessory", it.name, it.compatibleConsole, it.id) }
    }

    private fun add(
        kind: String,
        title: String,
        platform: String,
        id: String,
    ) {
        val key = listOf(kind, title, platform).joinToString("|") { it.trim().lowercase() }
        free.getOrPut(key) { ArrayDeque() }.addLast(id)
    }

    /** Identifier of an existing item matching [row], consumed so each copy is matched once. */
    fun take(
        kind: String,
        row: CsvRow,
    ): String? {
        val key = if (kind == "console") listOf(kind, row.title, row.title).joinToString("|") { it.trim().lowercase() } else row.key(kind)
        return free[key]?.removeFirstOrNull()
    }

    /** Stable identifier of a new item (the n-th copy of the same row gets suffix n). */
    fun newId(
        kind: String,
        row: CsvRow,
    ): String {
        val key = row.key(kind)
        val n = seen.getOrElse(key) { 0 }
        seen[key] = n + 1
        return "csv-${key.hashCode().toUInt().toString(HEX)}-$n"
    }

    private companion object {
        const val HEX = 16
    }
}
