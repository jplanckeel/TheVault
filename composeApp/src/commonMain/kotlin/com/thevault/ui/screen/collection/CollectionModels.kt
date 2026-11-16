package com.thevault.ui.screen.collection

import com.thevault.domain.model.Accessory
import com.thevault.domain.model.Components
import com.thevault.domain.model.Console
import com.thevault.domain.model.CopyDetails
import com.thevault.domain.model.Game
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.PriceQuote
import com.thevault.domain.model.Region
import com.thevault.ui.component.CardData
import com.thevault.ui.component.Manufacturer
import com.thevault.ui.component.manufacturerOf
import com.thevault.ui.component.shortPlatformName
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
 * @property details Everything else about the copy (purchase, storage, loan, sale, tags).
 * @property quote eBay quote used for [value], null when the value was typed by hand.
 * @property quotes Every eBay quote of the item (all grades).
 * @property usPrice US price of the Gameye import for the item's completeness (indicative).
 * @property valueSource Where [value] comes from.
 * @property hasPhoto Whether [coverUrl] is the user's photo of the copy.
 */
data class CollectionEntry(
    val id: String,
    val kind: ItemKind,
    val title: String,
    val platform: String,
    val manufacturer: Manufacturer,
    val coverUrl: String?,
    val condition: Components,
    val region: Region,
    val status: ItemStatus,
    val value: Double?,
    val purchasePrice: Double?,
    val acquiredAt: LocalDate?,
    val subtype: String = "",
    val details: CopyDetails = CopyDetails(),
    val quote: PriceQuote? = null,
    val quotes: List<PriceQuote> = emptyList(),
    val usPrice: Double? = null,
    val valueSource: ValueSource = if (value != null) ValueSource.Manual else ValueSource.None,
    val hasPhoto: Boolean = false,
) {
    /** Change of the quote over 30 days, in percent; null when unknown. */
    val changePercent: Double?
        get() {
            val past = quote?.medianPast ?: return null
            return if (past > 0.0) (quote.median - past) / past * PERCENT else null
        }

    /** Change of the quote over 30 days, in euros; null when unknown. */
    val changeAmount: Double? get() = quote?.medianPast?.let { quote.median - it }

    /** Data of a collection card. */
    val card: CardData
        get() {
            val caption = if (kind == ItemKind.Console) manufacturer.label else shortPlatformName(platform)
            return CardData(title, platform, coverUrl, condition, value, changePercent, caption, valueSource == ValueSource.UsIndicative)
        }
}

private const val PERCENT = 100.0

/** Maps a game. */
fun Game.toEntry() =
    CollectionEntry(
        id,
        ItemKind.Game,
        title,
        platform,
        manufacturerOf(platform),
        coverUrl,
        details.components,
        region,
        status,
        currentValue,
        purchasePrice,
        acquiredAt,
        details = details,
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
        details.components,
        region,
        status,
        currentValue,
        purchasePrice,
        acquiredAt,
        details = details,
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
        details.components,
        Region.OTHER,
        status,
        currentValue,
        purchasePrice,
        acquiredAt,
        subtype = type,
        details = details,
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
 * @property changePercent 30-day change of the quoted games, in percent; null when unknown.
 */
data class PlatformSummary(
    val platform: String,
    val manufacturer: Manufacturer,
    val count: Int,
    val value: Double,
    val changePercent: Double? = null,
)

/** 30-day change, in percent, of the quoted entries; null when none has a past quote. */
fun List<CollectionEntry>.changePercent(): Double? {
    val quoted = filter { it.quote?.medianPast != null }
    val past = quoted.sumOf { it.quote?.medianPast ?: 0.0 }
    if (past <= 0.0) return null
    return (quoted.sumOf { it.quote?.median ?: 0.0 } - past) / past * PERCENT
}

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
        .map { (platform, list) ->
            PlatformSummary(platform, list.first().manufacturer, list.size, list.totalValue(), list.changePercent())
        }.sortedWith(compareByDescending<PlatformSummary> { it.count }.thenByDescending { it.value })

/** Platforms grouped by manufacturer (Nintendo, Microsoft, Sony, Sega, others). */
fun manufacturerGroups(games: List<CollectionEntry>): List<ManufacturerGroup> =
    platformSummaries(games)
        .groupBy { it.manufacturer }
        .map { (manufacturer, platforms) -> ManufacturerGroup(manufacturer, platforms) }
        .sortedBy { it.manufacturer.order }

private val controllerPattern = Regex("""manette|controller|pad|joy-?con""", RegexOption.IGNORE_CASE)

/** Whether an accessory is a controller (« Manettes » filter). */
fun CollectionEntry.isController(): Boolean = controllerPattern.containsMatchIn("$subtype $title")
