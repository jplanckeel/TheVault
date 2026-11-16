package com.thevault.data.repository

import com.thevault.data.remote.CatalogRemoteDataSource
import com.thevault.data.remote.dto.CatalogEntryDto
import com.thevault.domain.model.CatalogSheet
import com.thevault.domain.repository.CatalogRepository

/**
 * [CatalogRepository] reading the backend catalogue.
 *
 * @param remote Catalogue API.
 * @param webOrigin Origin serving the backend files (covers uploaded in the back-office).
 */
class CatalogRepositoryImpl(
    private val remote: CatalogRemoteDataSource,
    private val webOrigin: String,
) : CatalogRepository {
    override suspend fun lookup(externalIds: List<String>): Map<String, CatalogSheet> =
        if (externalIds.isEmpty()) emptyMap() else remote.lookup(externalIds).associate { it.externalId to it.toSheet(webOrigin) }
}

/** Maps a catalogue entry; blank fields become null so they never erase local data. */
internal fun CatalogEntryDto.toSheet(webOrigin: String): CatalogSheet =
    CatalogSheet(
        externalId = externalId,
        coverUrl = resolveCover(coverUrl, webOrigin),
        genres = genres.split(',').map { it.trim() }.filter { it.isNotEmpty() },
        developer = developer.trim().ifBlank { null },
        publisher = publisher.trim().ifBlank { null },
        releaseYear = releaseYear.takeIf { it > 0 },
    )

/** Absolute cover URL: IGDB ones are protocol-relative, uploaded ones are backend paths. */
internal fun resolveCover(
    raw: String,
    webOrigin: String,
): String? {
    val url = raw.trim()
    return when {
        url.isEmpty() -> null
        url.startsWith("https://") || url.startsWith("http://") -> url
        url.startsWith("//") -> "https:$url"
        url.startsWith("/") && webOrigin.isNotBlank() -> webOrigin.trimEnd('/') + url
        else -> null
    }
}
