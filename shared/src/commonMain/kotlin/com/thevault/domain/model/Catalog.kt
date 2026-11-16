package com.thevault.domain.model

/**
 * Shared catalogue sheet of an item, kept up to date by the backend (IGDB enrichment,
 * covers uploaded and corrections made in the back-office).
 *
 * @property externalId Local identifier of the item.
 * @property coverUrl Absolute cover URL, null when the catalogue has none.
 * @property genres Genres, most relevant first.
 * @property developer Developer, null when unknown.
 * @property publisher Publisher, null when unknown.
 * @property releaseYear Release year, null when unknown.
 */
data class CatalogSheet(
    val externalId: String,
    val coverUrl: String? = null,
    val genres: List<String> = emptyList(),
    val developer: String? = null,
    val publisher: String? = null,
    val releaseYear: Int? = null,
)

/**
 * Game with the catalogue facts applied. Only filled catalogue fields replace local ones;
 * the copy (condition, prices, notes…) is never touched.
 */
fun Game.withCatalog(sheet: CatalogSheet): Game =
    copy(
        coverUrl = sheet.coverUrl ?: coverUrl,
        genre = sheet.genres.firstOrNull() ?: genre,
        releaseYear = sheet.releaseYear ?: releaseYear,
        developer = sheet.developer ?: developer,
        publisher = sheet.publisher ?: publisher,
    )

/** Console with the catalogue picture and year applied. */
fun Console.withCatalog(sheet: CatalogSheet): Console =
    copy(
        imageUrl = sheet.coverUrl ?: imageUrl,
        productionYear = sheet.releaseYear ?: productionYear,
    )

/** Accessory with the catalogue picture applied. */
fun Accessory.withCatalog(sheet: CatalogSheet): Accessory = copy(imageUrl = sheet.coverUrl ?: imageUrl)
