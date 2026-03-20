package com.thevault.data.mapper

import com.thevault.database.Game as GameEntity
import com.thevault.database.Console as ConsoleEntity
import com.thevault.database.Accessory as AccessoryEntity
import com.thevault.domain.model.Game
import com.thevault.domain.model.Console
import com.thevault.domain.model.Accessory
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.ItemCondition
import com.thevault.domain.model.Region
import kotlinx.datetime.LocalDate

fun GameEntity.toDomain(): Game = Game(
    id = id,
    title = title,
    platform = platform,
    genre = genre,
    releaseYear = release_year.toInt(),
    region = Region.valueOf(region),
    condition = ItemCondition.valueOf(condition_),
    coverUrl = cover_url,
    status = ItemStatus.valueOf(status),
    purchasePrice = purchase_price,
    currentValue = current_value,
    notes = notes,
    acquiredAt = acquired_at?.let { LocalDate.parse(it) },
    rating = rating?.toInt(),
    developer = developer,
    publisher = publisher,
    gameModes = game_modes,
    themes = themes,
    isCollectorEdition = is_collector_edition != 0L,
    isLimitedEdition = is_limited_edition != 0L,
)

fun ConsoleEntity.toDomain(): Console = Console(
    id = id,
    name = name,
    manufacturer = manufacturer,
    region = Region.valueOf(region),
    color = color,
    condition = ItemCondition.valueOf(condition_),
    productionYear = production_year.toInt(),
    status = ItemStatus.valueOf(status),
    purchasePrice = purchase_price,
    currentValue = current_value,
    imageUrl = image_url,
    notes = notes,
    acquiredAt = acquired_at?.let { LocalDate.parse(it) },
    isCollectorEdition = is_collector_edition != 0L,
    isLimitedEdition = is_limited_edition != 0L,
)

fun AccessoryEntity.toDomain(): Accessory = Accessory(
    id = id,
    name = name,
    type = type,
    compatibleConsole = compatible_console,
    condition = ItemCondition.valueOf(condition_),
    status = ItemStatus.valueOf(status),
    purchasePrice = purchase_price,
    currentValue = current_value,
    imageUrl = image_url,
    notes = notes,
    acquiredAt = acquired_at?.let { LocalDate.parse(it) },
    isCollectorEdition = is_collector_edition != 0L,
    isLimitedEdition = is_limited_edition != 0L,
)

fun Game.toEntity(createdAt: String, updatedAt: String): GameEntity = GameEntity(
    id = id,
    title = title,
    platform = platform,
    genre = genre,
    release_year = releaseYear.toLong(),
    region = region.name,
    condition_ = condition.name,
    cover_url = coverUrl,
    status = status.name,
    purchase_price = purchasePrice,
    current_value = currentValue,
    notes = notes,
    acquired_at = acquiredAt?.toString(),
    created_at = createdAt,
    updated_at = updatedAt,
    rating = rating?.toLong(),
    developer = developer,
    publisher = publisher,
    game_modes = gameModes,
    themes = themes,
    is_collector_edition = if (isCollectorEdition) 1L else 0L,
    is_limited_edition = if (isLimitedEdition) 1L else 0L,
)
