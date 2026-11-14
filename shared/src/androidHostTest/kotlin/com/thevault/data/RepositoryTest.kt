package com.thevault.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.thevault.data.repository.AccessoryRepositoryImpl
import com.thevault.data.repository.ConsoleRepositoryImpl
import com.thevault.data.repository.GameRepositoryImpl
import com.thevault.database.TheVaultDatabase
import com.thevault.domain.model.Accessory
import com.thevault.domain.model.Components
import com.thevault.domain.model.Console
import com.thevault.domain.model.CopyDetails
import com.thevault.domain.model.Edition
import com.thevault.domain.model.Game
import com.thevault.domain.model.Grade
import com.thevault.domain.model.ItemStatus
import com.thevault.domain.model.Region
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Round trips through SQLite: every copy detail and the tags survive a save. */
class RepositoryTest {
    private fun database() = TheVaultDatabase(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { TheVaultDatabase.Schema.create(it) })

    private val details =
        CopyDetails(
            components = Components(box = true, manual = false, extras = true, mediaGrade = Grade.VERY_GOOD, boxGrade = Grade.FAIR),
            regionVariant = "FAH",
            edition = Edition.BUDGET,
            barcode = "045496390031",
            productCode = "DOL-GZLP-FAH",
            shippingCost = 4.5,
            acquiredFrom = "Leboncoin",
            location = "Étagère 2",
            tags = listOf("zelda", "à revendre"),
            loanedTo = "Paul",
            loanDueAt = LocalDate(2026, 12, 1),
            soldPrice = null,
            soldAt = null,
        )

    @Test
    fun gameRoundTripKeepsDetailsAndTags() =
        runTest {
            val repo = GameRepositoryImpl(database())
            val game = Game("g", "Zelda", "GameCube", "Action", 2003, Region.PAL, details, status = ItemStatus.LOANED, purchasePrice = 30.0)
            repo.save(game)
            assertEquals(game, repo.getById("g"))
            assertEquals(listOf(game), repo.observeAll().first())
            repo.save(game.copy(details = details.copy(tags = listOf("zelda"))))
            assertEquals(listOf("zelda"), repo.getById("g")?.details?.tags)
            repo.delete("g")
            assertNull(repo.getById("g"))
        }

    @Test
    fun consolesAndAccessoriesArePersisted() =
        runTest {
            val db = database()
            val console = Console("c", "GameCube", "Nintendo", Region.PAL, "Indigo", details, 2001, ItemStatus.OWNED)
            val accessory = Accessory("a", "Manette", "Controller", "GameCube", details, ItemStatus.SOLD)
            ConsoleRepositoryImpl(db).save(console)
            AccessoryRepositoryImpl(db).save(accessory)
            assertEquals(console, ConsoleRepositoryImpl(db).getById("c"))
            assertEquals(accessory, AccessoryRepositoryImpl(db).observeAll().first().single())
        }
}
