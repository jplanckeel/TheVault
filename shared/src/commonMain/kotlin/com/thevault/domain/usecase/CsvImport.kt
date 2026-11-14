package com.thevault.domain.usecase

import com.thevault.domain.model.Accessory
import com.thevault.domain.model.Components
import com.thevault.domain.model.Console
import com.thevault.domain.model.CopyDetails
import com.thevault.domain.model.Edition
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import com.thevault.domain.usecase.GameyeFormat.Col
import kotlinx.datetime.LocalDate

/**
 * US prices of an item in the Gameye export (PriceCharting). Never shown as a PAL quote.
 *
 * @property loose Loose price.
 * @property cib Complete price.
 * @property new New price.
 */
data class UsPrices(
    val loose: Double?,
    val cib: Double?,
    val new: Double?,
) {
    /** Whether at least one price is known. */
    val isKnown: Boolean get() = loose != null || cib != null || new != null
}

/**
 * One row of a Gameye CSV export, interpreted. The whole collection is PAL: the unreliable
 * `Country` column is ignored and the region variant is left empty.
 *
 * @property platform Platform.
 * @property category `Games`, `Systems` or an accessory category.
 * @property status Status.
 * @property title Title.
 * @property publisher Publisher (manufacturer for systems).
 * @property developer Developer.
 * @property genre Genre.
 * @property createdAt Date the item was added in Gameye.
 * @property components Parts and grades.
 * @property usPrices US prices of the export.
 * @property yourPrice Gameye « YourPrice » (an estimate, not the user's value).
 * @property price Price paid, null when unknown.
 * @property notes Notes.
 * @property tags Tags.
 */
data class CsvRow(
    val platform: String,
    val category: String,
    val status: ItemStatus,
    val title: String,
    val publisher: String,
    val developer: String,
    val genre: String,
    val createdAt: LocalDate?,
    val components: Components,
    val usPrices: UsPrices,
    val yourPrice: Double?,
    val price: Double?,
    val notes: String,
    val tags: List<String>,
) {
    private val edition: Edition
        get() =
            when {
                notes.contains("collector", ignoreCase = true) -> Edition.COLLECTOR
                notes.contains("limited", ignoreCase = true) || notes.contains("limitée", ignoreCase = true) -> Edition.LIMITED
                else -> Edition.STANDARD
            }

    /** Kind of item (`game`, `console`, `accessory`). */
    val kind: String
        get() =
            when (category.lowercase()) {
                "games" -> "game"
                "systems" -> "console"
                else -> "accessory"
            }

    /** Identity of the row, shared by the copies of the same item. */
    fun key(): String = listOf(kind, title, if (kind == "console") title else platform).joinToString("|") { it.trim().lowercase() }

    private fun details(base: CopyDetails = CopyDetails()) =
        base.copy(
            components = components,
            edition = if (base.edition == Edition.STANDARD) edition else base.edition,
            tags = (base.tags + tags).distinct(),
        )

    /** Value kept on re-import: a previous import stored « YourPrice » as the value; drop it. */
    private fun keptValue(current: Double?): Double? = current?.takeUnless { it == yourPrice }

    /** Blank game with [id]. */
    fun newGame(id: String) = toGame(Game(id, title, platform, genre, 0, Region.PAL, status = status))

    /** Blank console with [id]. */
    fun newConsole(id: String) =
        toConsole(Console(id, title, publisher.ifEmpty { "Unknown" }, Region.PAL, "", productionYear = 0, status = status))

    /** Blank accessory with [id]. */
    fun newAccessory(id: String) = toAccessory(Accessory(id, title, category, platform, status = status))

    /** CSV fields applied to [base]; cover, year and app-only fields are kept. */
    fun toGame(base: Game) =
        base.copy(
            title = title,
            platform = platform,
            genre = genre.ifBlank { base.genre.ifBlank { "N/A" } },
            region = Region.PAL,
            details = details(base.details),
            status = status,
            purchasePrice = price ?: base.purchasePrice,
            currentValue = keptValue(base.currentValue),
            notes = notes.ifBlank { base.notes },
            developer = developer.ifBlank { base.developer },
            publisher = publisher.ifBlank { base.publisher },
            acquiredAt = createdAt ?: base.acquiredAt,
        )

    /** CSV fields applied to [base]. */
    fun toConsole(base: Console) =
        base.copy(
            name = title,
            region = Region.PAL,
            details = details(base.details),
            status = status,
            purchasePrice = price ?: base.purchasePrice,
            currentValue = keptValue(base.currentValue),
            notes = notes.ifBlank { base.notes },
            acquiredAt = createdAt ?: base.acquiredAt,
        )

    /** CSV fields applied to [base]. */
    fun toAccessory(base: Accessory) =
        base.copy(
            name = title,
            type = category,
            compatibleConsole = platform,
            details = details(base.details),
            status = status,
            purchasePrice = price ?: base.purchasePrice,
            currentValue = keptValue(base.currentValue),
            notes = notes.ifBlank { base.notes },
            acquiredAt = createdAt ?: base.acquiredAt,
        )

    /** Companion helpers. */
    companion object {
        /** Minimum number of columns of a valid row. */
        const val MIN_COLUMNS = 21

        /** Interprets a raw Gameye row. */
        fun parse(row: List<String>): CsvRow {
            val f = GameyeFormat
            val ownership = f.components(row[Col.OWNERSHIP])
            val base = if (f.isDigital(row[Col.RELEASE_TYPE])) Components.DIGITAL else ownership
            val parts =
                base.copy(
                    mediaGrade = f.grade(row[Col.ITEM_CONDITION]).takeIf { base.media },
                    boxGrade = f.grade(row[Col.BOX_CONDITION]).takeIf { base.box },
                    manualGrade = f.grade(row[Col.MANUAL_CONDITION]).takeIf { base.manual },
                )
            return CsvRow(
                platform = f.text(row[Col.PLATFORM]),
                category = f.text(row[Col.CATEGORY]),
                status = f.status(row[Col.RECORD_TYPE]),
                title = f.text(row[Col.TITLE]).ifBlank { "Sans titre" },
                publisher = f.text(row[Col.PUBLISHER]),
                developer = f.text(row[Col.DEVELOPER]),
                genre = f.text(row[Col.GENRE]),
                createdAt = f.date(row[Col.CREATED_AT]),
                components = parts,
                usPrices = UsPrices(f.amount(row[Col.PRICE_LOOSE]), f.amount(row[Col.PRICE_CIB]), f.amount(row[Col.PRICE_NEW])),
                yourPrice = f.amount(row[Col.YOUR_PRICE]),
                price = f.amount(row[Col.PRICE_PAID]),
                notes = f.text(row.getOrNull(Col.NOTES)),
                tags = f.tags(row.getOrNull(Col.TAGS)),
            )
        }
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
    private val used: MutableSet<String> = (games.map { it.id } + consoles.map { it.id } + accessories.map { it.id }).toMutableSet()

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
    fun take(row: CsvRow): String? = free[row.key()]?.removeFirstOrNull()

    /** Stable identifier of a new item (the n-th copy of the same row gets suffix n). */
    fun newId(row: CsvRow): String {
        val key = row.key()
        var n = seen.getOrElse(key) { 0 }
        var id = "csv-${key.hashCode().toUInt().toString(HEX)}-$n"
        while (id in used) id = "csv-${key.hashCode().toUInt().toString(HEX)}-${++n}"
        seen[key] = n + 1
        used += id
        return id
    }

    private companion object {
        const val HEX = 16
    }
}
