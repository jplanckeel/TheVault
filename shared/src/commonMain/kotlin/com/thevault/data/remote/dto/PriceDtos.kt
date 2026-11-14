package com.thevault.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Body of POST /prices/quotes.
 *
 * @property externalIds Identifiers of the items to quote.
 * @property days Age, in days, of the past median returned with each quote.
 */
@Serializable
data class QuotesRequestDto(
    @SerialName("external_ids") val externalIds: List<String>,
    val days: Int = 30,
)

/**
 * Latest quote of an item for one condition.
 *
 * @property externalId Item identifier.
 * @property condition `loose`, `cib` or `new`.
 * @property priceMin Lowest price.
 * @property priceMax Highest price.
 * @property priceMedian Median price.
 * @property sampleCount Number of listings.
 * @property scrapedAt ISO-8601 time of the quote.
 * @property pricePast Median quoted `days` ago, or null.
 */
@Serializable
data class QuoteDto(
    @SerialName("external_id") val externalId: String,
    val condition: String,
    @SerialName("price_min") val priceMin: Double = 0.0,
    @SerialName("price_max") val priceMax: Double = 0.0,
    @SerialName("price_median") val priceMedian: Double = 0.0,
    @SerialName("sample_count") val sampleCount: Int = 0,
    @SerialName("scraped_at") val scrapedAt: String,
    @SerialName("price_past") val pricePast: Double? = null,
)

/**
 * One price snapshot (GET /prices/history).
 *
 * @property condition `loose`, `cib` or `new`.
 * @property priceMedian Median price.
 * @property scrapedAt ISO-8601 time of the snapshot.
 */
@Serializable
data class PriceSnapshotDto(
    val condition: String,
    @SerialName("price_median") val priceMedian: Double = 0.0,
    @SerialName("scraped_at") val scrapedAt: String,
)

/**
 * Item pushed to the catalogue (POST /items).
 *
 * @property externalId Local identifier.
 * @property type `game`, `console` or `accessory`.
 * @property title Title.
 * @property platform Platform.
 * @property region Region.
 * @property condition Condition.
 * @property status Status.
 * @property coverUrl Cover URL.
 * @property summary Summary (unused by the app).
 * @property genres Genres.
 * @property developer Developer.
 * @property publisher Publisher.
 * @property releaseYear Release year.
 */
@Serializable
data class SyncItemDto(
    @SerialName("external_id") val externalId: String,
    val type: String,
    val title: String,
    val platform: String,
    val region: String,
    val condition: String,
    val status: String,
    @SerialName("cover_url") val coverUrl: String = "",
    val summary: String = "",
    val genres: String = "",
    val developer: String = "",
    val publisher: String = "",
    @SerialName("release_year") val releaseYear: Int = 0,
)
