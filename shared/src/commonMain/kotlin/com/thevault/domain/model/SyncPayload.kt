package com.thevault.domain.model

/**
 * Item as pushed to the shared catalogue of the backend (POST /items), which then
 * collects its eBay quotes.
 *
 * @property externalId Local identifier, used by the backend to find the item again.
 * @property type `game`, `console` or `accessory`.
 * @property title Title or name.
 * @property platform Platform (games, accessories) or console name.
 * @property region Region name.
 * @property condition Condition name.
 * @property status Status name.
 * @property coverUrl Cover or photo URL, empty when unknown.
 * @property genres Genre, empty when unknown.
 * @property developer Developer, empty when unknown.
 * @property publisher Publisher, empty when unknown.
 * @property releaseYear Release year, 0 when unknown.
 */
data class SyncPayload(
    val externalId: String,
    val type: String,
    val title: String,
    val platform: String,
    val region: String,
    val condition: String,
    val status: String,
    val coverUrl: String = "",
    val genres: String = "",
    val developer: String = "",
    val publisher: String = "",
    val releaseYear: Int = 0,
)

/** Payload of a game. */
fun Game.toSyncPayload() =
    SyncPayload(
        externalId = id,
        type = "game",
        title = title,
        platform = platform,
        region = region.name,
        condition = condition.name,
        status = status.name,
        coverUrl = coverUrl.orEmpty(),
        genres = genre.takeIf { it != "N/A" }.orEmpty(),
        developer = developer.orEmpty(),
        publisher = publisher.orEmpty(),
        releaseYear = releaseYear,
    )

/** Payload of a console; its name doubles as the platform eBay is searched with. */
fun Console.toSyncPayload() =
    SyncPayload(
        externalId = id,
        type = "console",
        title = name,
        platform = name,
        region = region.name,
        condition = condition.name,
        status = status.name,
        coverUrl = imageUrl.orEmpty(),
        releaseYear = productionYear,
    )

/** Payload of an accessory. */
fun Accessory.toSyncPayload() =
    SyncPayload(
        externalId = id,
        type = "accessory",
        title = name,
        platform = compatibleConsole,
        region = Region.OTHER.name,
        condition = condition.name,
        status = status.name,
        coverUrl = imageUrl.orEmpty(),
    )

/**
 * Outcome of pushing one item.
 */
enum class SyncOutcome {
    /** Stored by the backend. */
    Synced,

    /** Refused for good (deleted by an admin, invalid): never retried. */
    Rejected,

    /** Network or server error: retried later. */
    Failed,
}

/**
 * Result of a synchronisation run.
 *
 * @property synced Items stored by the backend.
 * @property failed Items left pending after an error.
 */
data class SyncReport(
    val synced: Int,
    val failed: Int,
)
