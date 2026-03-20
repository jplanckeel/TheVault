package com.thevault.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RawgSearchResponse(
    val results: List<RawgGameDto> = emptyList()
)

@Serializable
data class RawgGameDto(
    val id: Int,
    val name: String,
    val released: String? = null,
    val background_image: String? = null,
    val platforms: List<RawgPlatformWrapperDto>? = null,
    val genres: List<RawgGenreDto>? = null
)

@Serializable
data class RawgPlatformWrapperDto(
    val platform: RawgPlatformDto? = null
)

@Serializable
data class RawgPlatformDto(
    val name: String
)

@Serializable
data class RawgGenreDto(
    val name: String
)
