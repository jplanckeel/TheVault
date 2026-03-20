package com.thevault.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TwitchTokenResponse(
    val access_token: String,
    val expires_in: Int,
    val token_type: String
)

@Serializable
data class IgdbGameDto(
    val id: Long,
    val name: String,
    val first_release_date: Long? = null,
    val genres: List<IgdbGenreDto>? = null,
    val platforms: List<IgdbPlatformDto>? = null,
    val cover: IgdbCoverDto? = null,
    val summary: String? = null,
    val involved_companies: List<IgdbInvolvedCompanyDto>? = null,
    val game_modes: List<IgdbGameModeDto>? = null,
    val themes: List<IgdbThemeDto>? = null
)

@Serializable
data class IgdbGenreDto(val id: Long, val name: String)

@Serializable
data class IgdbPlatformDto(val id: Long, val name: String)

@Serializable
data class IgdbCoverDto(val id: Long, val url: String)

@Serializable
data class IgdbInvolvedCompanyDto(
    val id: Long,
    val company: IgdbCompanyDto? = null,
    val developer: Boolean = false,
    val publisher: Boolean = false
)

@Serializable
data class IgdbCompanyDto(val id: Long, val name: String)

@Serializable
data class IgdbGameModeDto(val id: Long, val name: String)

@Serializable
data class IgdbThemeDto(val id: Long, val name: String)

