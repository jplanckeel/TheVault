package com.thevault.data.mapper

import com.thevault.data.remote.dto.BackupDetailsDto
import com.thevault.data.remote.dto.BackupItemDto
import com.thevault.data.remote.dto.BackupPlayDto
import com.thevault.data.remote.dto.BackupSetDto
import com.thevault.data.remote.dto.VaultBackupDto
import com.thevault.domain.model.Accessory
import com.thevault.domain.model.Completeness
import com.thevault.domain.model.Components
import com.thevault.domain.model.Console
import com.thevault.domain.model.CopyDetails
import com.thevault.domain.model.Edition
import com.thevault.domain.model.Game
import com.thevault.domain.model.Grade
import com.thevault.domain.model.ItemSet
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.PlayLog
import com.thevault.domain.model.PlayStatus
import com.thevault.domain.model.Region
import com.thevault.domain.model.SetEntry
import com.thevault.domain.model.VaultContent
import com.thevault.domain.model.WishPrefs
import kotlinx.datetime.LocalDate

/** Backup document of [content], dated [exportedAt]. */
fun VaultContent.toDto(exportedAt: String): VaultBackupDto =
    VaultBackupDto(
        exportedAt = exportedAt,
        items =
            games.map { it.toDto(values[it.id]) } +
                consoles.map { it.toDto(values[it.id]) } +
                accessories.map { it.toDto(values[it.id]) },
        sets = sets.map { s -> BackupSetDto(s.id, s.name, s.entries.map { listOf(it.title, it.platform) }) },
    )

/** Content of a backup document. */
fun VaultBackupDto.toDomain(): VaultContent =
    VaultContent(
        games = items.filter { it.kind == "game" }.map { it.toGame() },
        consoles = items.filter { it.kind == "console" }.map { it.toConsole() },
        accessories = items.filter { it.kind == "accessory" }.map { it.toAccessory() },
        sets = sets.map { s -> ItemSet(s.id, s.name, s.entries.mapNotNull { e -> if (e.size == 2) SetEntry(e[0], e[1]) else null }) },
    )

private fun CopyDetails.toDto() =
    BackupDetailsDto(
        components.media,
        components.box,
        components.manual,
        components.extras,
        components.mediaGrade?.name,
        components.boxGrade?.name,
        components.manualGrade?.name,
        components.sealed,
        components.digital,
        regionVariant,
        barcode,
        productCode,
        shippingCost,
        acquiredFrom,
        location,
        tags,
        loanedTo,
        loanDueAt?.toString(),
        soldPrice,
        soldAt?.toString(),
        wish.priority,
        wish.maxPrice,
        wish.target?.name,
    )

private fun BackupDetailsDto.toDomain() =
    CopyDetails(
        components = Components(media, box, manual, extras, grade(mediaGrade), grade(boxGrade), grade(manualGrade), sealed, digital),
        regionVariant = regionVariant,
        barcode = barcode,
        productCode = productCode,
        shippingCost = shippingCost,
        acquiredFrom = acquiredFrom,
        location = location,
        tags = tags,
        loanedTo = loanedTo,
        loanDueAt = day(loanDueAt),
        soldPrice = soldPrice,
        soldAt = day(soldAt),
        wish = WishPrefs(wishPriority, wishMaxPrice, wishTarget?.let { t -> Completeness.entries.find { it.name == t } }),
    )

/** Edition is stored apart from the details so that share pages can read it. */
private fun BackupItemDto.details() = details.toDomain().copy(edition = Edition.entries.find { it.name == edition } ?: Edition.STANDARD)

private fun base(
    id: String,
    kind: String,
    title: String,
    platform: String,
    status: ItemStatus,
    details: CopyDetails,
    value: Double?,
) = BackupItemDto(
    id = id,
    kind = kind,
    title = title,
    platform = platform,
    status = status.name,
    completeness = details.components.completeness.name,
    edition = details.edition.name,
    value = value,
    region = Region.PAL.name,
    details = details.toDto(),
)

private fun Game.toDto(value: Double?) =
    base(id, "game", title, platform, status, details, value).copy(
        coverUrl = coverUrl,
        region = region.name,
        genre = genre,
        releaseYear = releaseYear,
        purchasePrice = purchasePrice,
        currentValue = currentValue,
        notes = notes,
        acquiredAt = acquiredAt?.toString(),
        rating = rating,
        developer = developer,
        publisher = publisher,
        gameModes = gameModes,
        themes = themes,
        play = BackupPlayDto(play.status?.name, play.minutes),
    )

private fun Console.toDto(value: Double?) =
    base(id, "console", name, name, status, details, value).copy(
        coverUrl = imageUrl,
        region = region.name,
        releaseYear = productionYear,
        manufacturer = manufacturer,
        color = color,
        purchasePrice = purchasePrice,
        currentValue = currentValue,
        notes = notes,
        acquiredAt = acquiredAt?.toString(),
    )

private fun Accessory.toDto(value: Double?) =
    base(id, "accessory", name, compatibleConsole, status, details, value).copy(
        coverUrl = imageUrl,
        type = type,
        purchasePrice = purchasePrice,
        currentValue = currentValue,
        notes = notes,
        acquiredAt = acquiredAt?.toString(),
    )

private fun BackupItemDto.toGame() =
    Game(
        id,
        title,
        platform,
        genre,
        releaseYear,
        regionOf(region),
        details(),
        coverUrl,
        statusOf(status),
        purchasePrice,
        currentValue,
        notes,
        day(acquiredAt),
        rating,
        developer,
        publisher,
        gameModes,
        themes,
        PlayLog(play.status?.let { s -> PlayStatus.entries.find { it.name == s } }, play.minutes),
    )

private fun BackupItemDto.toConsole() =
    Console(
        id,
        title,
        manufacturer,
        regionOf(region),
        color,
        details(),
        releaseYear,
        statusOf(status),
        purchasePrice,
        currentValue,
        coverUrl,
        notes,
        day(acquiredAt),
    )

private fun BackupItemDto.toAccessory() =
    Accessory(id, title, type, platform, details(), statusOf(status), purchasePrice, currentValue, coverUrl, notes, day(acquiredAt))

private fun grade(value: String?): Grade? = value?.let { v -> Grade.entries.find { it.name == v } }

private fun day(value: String?): LocalDate? = value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

private fun regionOf(value: String): Region = Region.entries.find { it.name == value } ?: Region.PAL

private fun statusOf(value: String): ItemStatus = ItemStatus.entries.find { it.name == value } ?: ItemStatus.OWNED
