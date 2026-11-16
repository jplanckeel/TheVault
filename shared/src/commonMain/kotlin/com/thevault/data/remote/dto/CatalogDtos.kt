package com.thevault.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Body of `POST /items/lookup`.
 *
 * @property externalIds Local identifiers of the items to look up.
 */
@Serializable
data class CatalogLookupRequestDto(
    @SerialName("external_ids") val externalIds: List<String>,
)

/**
 * Shared catalogue sheet of one item, as returned by `POST /items/lookup`.
 *
 * @property externalId Identifier the app asked for.
 * @property title Catalogue title.
 * @property platform Catalogue platform.
 * @property coverUrl Cover URL (absolute, protocol-relative or a backend path).
 * @property genres Comma-separated genres.
 * @property developer Developer.
 * @property publisher Publisher.
 * @property releaseYear Release year, 0 when unknown.
 */
@Serializable
data class CatalogEntryDto(
    @SerialName("external_id") val externalId: String,
    val title: String = "",
    val platform: String = "",
    @SerialName("cover_url") val coverUrl: String = "",
    val genres: String = "",
    val developer: String = "",
    val publisher: String = "",
    @SerialName("release_year") val releaseYear: Int = 0,
)
