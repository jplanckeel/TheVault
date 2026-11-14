package com.thevault.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.thevault.data.mapper.toDomain
import com.thevault.database.TheVaultDatabase
import com.thevault.domain.model.Completeness
import com.thevault.domain.model.Edition
import com.thevault.domain.model.Grade
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Upgrades a database created with the schema of version 4 (before copy components). */
class MigrationTest {
    private fun legacyDriver(): JdbcSqliteDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val schema = requireNotNull(javaClass.getResource("/schema_v4.sql")).readText()
        schema
            .split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { driver.execute(null, it, 0) }
        return driver
    }

    private fun JdbcSqliteDriver.insertGame(
        id: String,
        condition: String,
        collector: Int = 0,
        limited: Int = 0,
    ) {
        execute(
            null,
            "INSERT INTO game VALUES ('$id', 'T$id', 'SNES', 'RPG', 1994, 'PAL', '$condition', NULL, 'OWNED', 20.0, 40.0, NULL, " +
                "'2021-03-24', 'now', 'now', NULL, NULL, NULL, NULL, NULL, $collector, $limited)",
            0,
        )
    }

    private fun migrated(): Map<String, com.thevault.domain.model.Game> {
        val driver = legacyDriver()
        driver.insertGame("loose", "LOOSE")
        driver.insertGame("boxed", "CIB_NO_MANUAL")
        driver.insertGame("cib", "CIB", collector = 1)
        driver.insertGame("sealed", "NEW_SEALED", limited = 1)
        driver.insertGame("worn", "GOOD")
        TheVaultDatabase.Schema.migrate(driver, 4, TheVaultDatabase.Schema.version)
        return TheVaultDatabase(driver)
            .theVaultQueries
            .selectAll()
            .executeAsList()
            .associate { it.id to it.toDomain() }
    }

    @Test
    fun convertsLegacyConditionsIntoComponents() {
        val games = migrated()
        val loose = games.getValue("loose").details.components
        assertTrue(loose.media)
        assertFalse(loose.box)
        assertEquals(Completeness.LOOSE, loose.completeness)
        val boxed = games.getValue("boxed").details.components
        assertTrue(boxed.box)
        assertFalse(boxed.manual)
        assertEquals(Completeness.CIB, boxed.completeness)
        val cib = games.getValue("cib").details.components
        assertTrue(cib.box && cib.manual)
        val sealed = games.getValue("sealed").details.components
        assertTrue(sealed.sealed)
        assertEquals(Completeness.NEW, sealed.completeness)
        assertEquals(
            Grade.GOOD,
            games
                .getValue("worn")
                .details.components.mediaGrade,
        )
    }

    @Test
    fun convertsEditionFlagsAndKeepsOtherColumns() {
        val games = migrated()
        assertEquals(Edition.COLLECTOR, games.getValue("cib").details.edition)
        assertEquals(Edition.LIMITED, games.getValue("sealed").details.edition)
        assertEquals(Edition.STANDARD, games.getValue("loose").details.edition)
        assertEquals(20.0, games.getValue("cib").purchasePrice)
        assertEquals("2021-03-24", games.getValue("cib").acquiredAt.toString())
    }

    @Test
    fun freshSchemaMatchesMigratedTables() {
        val fresh = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { TheVaultDatabase.Schema.create(it) }
        val upgraded = legacyDriver().also { TheVaultDatabase.Schema.migrate(it, 4, TheVaultDatabase.Schema.version) }
        listOf("game", "console", "accessory", "item_tag", "item_photo", "price_snapshot").forEach { table ->
            assertEquals(fresh.columns(table), upgraded.columns(table), "columns of $table")
        }
    }

    private fun JdbcSqliteDriver.columns(table: String): Set<String> =
        executeQuery(
            null,
            "PRAGMA table_info($table)",
            { cursor ->
                val names = mutableSetOf<String>()
                while (cursor.next().value) names += cursor.getString(1).orEmpty()
                app.cash.sqldelight.db.QueryResult
                    .Value(names)
            },
            0,
        ).value
}
