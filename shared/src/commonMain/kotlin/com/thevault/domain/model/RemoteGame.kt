package com.thevault.domain.model

data class RemoteGame(
    val id: String,
    val title: String,
    val releaseYear: Int? = null,
    val platformNames: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val coverUrl: String? = null,
    val summary: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val gameModes: List<String> = emptyList(),
    val themes: List<String> = emptyList(),
)
