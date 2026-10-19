package com.thevault.ui.screen.collection

import com.thevault.domain.model.Accessory
import com.thevault.domain.model.Console
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemCondition
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import com.thevault.ui.component.FormatGroup
import com.thevault.ui.component.Manufacturer
import com.thevault.ui.component.TileData
import com.thevault.ui.component.formatGroup
import com.thevault.ui.component.manufacturerOf
import kotlinx.datetime.LocalDate

/**
 * Item category.
 *
 * @property label Plural label (segmented control).
 * @property singular Singular label (add screen).
 */
enum class ItemKind(
    val label: String,
    val singular: String,
) {
    /** Video games. */
    Game("Jeux", "Jeu"),

    /** Consoles. */
    Console("Consoles", "Console"),

    /** Accessories. */
    Accessory("Accessoires", "Accessoire"),
}

/**
 * Flattened game, console or accessory.
 *
 * @property id Domain identifier.
 * @property kind Category.
 * @property title Display title.
 * @property platform Platform (games, accessories) or console platform name.
 * @property manufacturer Manufacturer, used to group and tint.
 * @property coverUrl Cover or photo URL.
 * @property condition Physical condition.
 * @property region Region.
 * @property status Ownership status.
 * @property value Estimated value.
 * @property purchasePrice Purchase price.
 * @property acquiredAt Acquisition date.
 * @property subtype Accessory type ("Manette"…), empty otherwise.
 */
data class CollectionEntry(
    val id: String,
    val kind: ItemKind,
    val title: String,
    val platform: String,
    val manufacturer: Manufacturer,
    val coverUrl: String?,
    val condition: ItemCondition,
    val region: Region,
    val status: ItemStatus,
    val value: Double?,
    val purchasePrice: Double?,
    val acquiredAt: LocalDate?,
    val subtype: String = "",
) {
    /** Data for a grid tile. */
    val tile: TileData get() = TileData(title, platform, coverUrl, condition, value, purchasePrice)
}

/** Maps a game. */
fun Game.toEntry() =
    CollectionEntry(
        id,
        ItemKind.Game,
        title,
        platform,
        manufacturerOf(platform),
        coverUrl,
        condition,
        region,
        status,
        currentValue,
        purchasePrice,
        acquiredAt,
    )

/** Maps a console; the console name gives its platform tint. */
fun Console.toEntry() =
    CollectionEntry(
        id,
        ItemKind.Console,
        name,
        name,
        manufacturerFromLabel(manufacturer, name),
        imageUrl,
        condition,
        region,
        status,
        currentValue,
        purchasePrice,
        acquiredAt,
    )

/** Maps an accessory; its compatible console gives its platform. */
fun Accessory.toEntry() =
    CollectionEntry(
        id,
        ItemKind.Accessory,
        name,
        compatibleConsole,
        manufacturerOf(compatibleConsole),
        imageUrl,
        condition,
        Region.OTHER,
        status,
        currentValue,
        purchasePrice,
        acquiredAt,
        subtype = type,
    )

private fun manufacturerFromLabel(
    label: String,
    fallbackPlatform: String,
): Manufacturer =
    Manufacturer.entries.find { it.label.equals(label.trim(), ignoreCase = true) }
        ?: manufacturerOf("$label $fallbackPlatform")

/** Sum of known values. */
fun List<CollectionEntry>.totalValue(): Double = sumOf { it.value ?: 0.0 }

/**
 * One platform tile of the collection overview.
 *
 * @property platform Platform name.
 * @property manufacturer Manufacturer.
 * @property count Number of games.
 * @property value Estimated value of these games.
 */
data class PlatformSummary(
    val platform: String,
    val manufacturer: Manufacturer,
    val count: Int,
    val value: Double,
)

/**
 * Platforms of one manufacturer.
 *
 * @property manufacturer Manufacturer.
 * @property platforms Platforms, most games first.
 */
data class ManufacturerGroup(
    val manufacturer: Manufacturer,
    val platforms: List<PlatformSummary>,
) {
    /** Number of games across the group. */
    val count: Int get() = platforms.sumOf { it.count }
}

/** Platform summaries of the given games, most games first. */
fun platformSummaries(games: List<CollectionEntry>): List<PlatformSummary> =
    games
        .filter { it.platform.isNotBlank() }
        .groupBy { it.platform }
        .map { (platform, list) -> PlatformSummary(platform, list.first().manufacturer, list.size, list.totalValue()) }
        .sortedWith(compareByDescending<PlatformSummary> { it.count }.thenByDescending { it.value })

/** Platforms grouped by manufacturer (Nintendo, Microsoft, Sony, Sega, others). */
fun manufacturerGroups(games: List<CollectionEntry>): List<ManufacturerGroup> =
    platformSummaries(games)
        .groupBy { it.manufacturer }
        .map { (manufacturer, platforms) -> ManufacturerGroup(manufacturer, platforms) }
        .sortedBy { it.manufacturer.order }

/**
 * Sort order of a game grid.
 *
 * @property label Toggle label.
 */
enum class GameSort(
    val label: String,
) {
    /** Highest value first. */
    Value("Valeur ↓"),

    /** Most recently acquired first. */
    Recent("Récents"),

    /** Alphabetical. */
    Title("A → Z"),
}

/**
 * Filters of a game grid.
 *
 * @property platform Platform, or null for every platform.
 * @property query Free-text search.
 * @property format Format filter, or null for all.
 * @property sort Sort order.
 */
data class GameFilters(
    val platform: String? = null,
    val query: String = "",
    val format: FormatGroup? = null,
    val sort: GameSort = GameSort.Value,
)

/** Applies [filters] to games. Pure function, unit-tested. */
fun filterGames(
    games: List<CollectionEntry>,
    filters: GameFilters,
): List<CollectionEntry> {
    val query = filters.query.trim()
    return games
        .filter { filters.platform == null || it.platform == filters.platform }
        .filter { filters.format == null || it.condition.formatGroup == filters.format }
        .filter { query.isEmpty() || it.title.contains(query, ignoreCase = true) }
        .sortedWith(sortComparator(filters.sort))
}

/** Comparator of a [GameSort]. */
fun sortComparator(sort: GameSort): Comparator<CollectionEntry> =
    when (sort) {
        GameSort.Value -> compareByDescending<CollectionEntry> { it.value ?: -1.0 }.thenBy { it.title.lowercase() }
        GameSort.Recent ->
            compareByDescending<CollectionEntry, LocalDate?>(nullsFirst()) { it.acquiredAt }.thenBy { it.title.lowercase() }
        GameSort.Title -> compareBy { it.title.lowercase() }
    }

private val controllerPattern = Regex("""manette|controller|pad|joy-?con""", RegexOption.IGNORE_CASE)

/** Whether an accessory is a controller (« Manettes » filter). */
fun CollectionEntry.isController(): Boolean = controllerPattern.containsMatchIn("$subtype $title")
