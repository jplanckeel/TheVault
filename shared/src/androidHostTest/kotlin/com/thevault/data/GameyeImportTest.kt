package com.thevault.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.thevault.data.repository.AccessoryRepositoryImpl
import com.thevault.data.repository.ConsoleRepositoryImpl
import com.thevault.data.repository.GameRepositoryImpl
import com.thevault.database.TheVaultDatabase
import com.thevault.domain.model.Region
import com.thevault.domain.usecase.CsvService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Imports the reference Gameye export (467 items) into a real SQLite database. */
class GameyeImportTest {
    private val csv = File("../2026_03_19_ge_collection.csv").readText()

    private class Vault {
        val db = TheVaultDatabase(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { TheVaultDatabase.Schema.create(it) })
        val games = GameRepositoryImpl(db)
        val consoles = ConsoleRepositoryImpl(db)
        val accessories = AccessoryRepositoryImpl(db)
        val csv = CsvService(games, consoles, accessories)

        suspend fun snapshot() = Triple(games.observeAll().first(), consoles.observeAll().first(), accessories.observeAll().first())
    }

    @Test
    fun importsEveryItemAsPalWithGradesAndDates() =
        runTest {
            val vault = Vault()
            val report = vault.csv.importCsv(csv).getOrThrow()
            assertEquals(467, report.rows)
            assertEquals(467, report.created)
            val (games, consoles, accessories) = vault.snapshot()
            assertEquals(Triple(414, 23, 30), Triple(games.size, consoles.size, accessories.size))
            val all = games.map { it.region } + consoles.map { it.region }
            assertTrue(all.all { it == Region.PAL })
            assertTrue(games.all { it.acquiredAt != null })
            assertTrue(games.none { (it.purchasePrice ?: 0.0) < 0.0 })
            assertTrue(games.count { it.details.components.mediaGrade != null } > 300)
            assertTrue(report.usPrices.size > 400)
        }

    @Test
    fun reimportAndExportAreLossless() =
        runTest {
            val vault = Vault()
            val first = vault.csv.importCsv(csv).getOrThrow()
            val before = vault.snapshot()
            val again = vault.csv.importCsv(csv).getOrThrow()
            assertEquals(467, again.updated)
            assertEquals(before, vault.snapshot())
            val exported = vault.csv.exportCsv(first.usPrices).getOrThrow()
            val copy = Vault()
            val reimported = copy.csv.importCsv(exported).getOrThrow()
            assertEquals(before, copy.snapshot())
            assertEquals(first.usPrices, reimported.usPrices)
        }
}
