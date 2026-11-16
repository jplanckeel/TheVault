package com.thevault.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BoSearchResultDto(
    val id: String,
    val title: String,
    @SerialName("release_year") val releaseYear: Int = 0,
    val platforms: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    @SerialName("cover_url") val coverUrl: String? = null,
    val summary: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
)
