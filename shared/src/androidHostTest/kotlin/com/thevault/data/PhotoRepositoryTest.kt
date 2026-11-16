package com.thevault.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.thevault.data.repository.PhotoRepositoryImpl
import com.thevault.database.TheVaultDatabase
import com.thevault.domain.usecase.ObserveItemPhotosUseCase
import com.thevault.domain.usecase.RemoveItemPhotoUseCase
import com.thevault.domain.usecase.SetItemPhotoUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PhotoRepositoryTest {
    private val repo =
        PhotoRepositoryImpl(TheVaultDatabase(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { TheVaultDatabase.Schema.create(it) }))
    private val observe = ObserveItemPhotosUseCase(repo)
    private val set = SetItemPhotoUseCase(repo)
    private val remove = RemoveItemPhotoUseCase(repo)

    @Test
    fun anItemKeepsItsLatestPhotoAndReportsTheReplacedFiles() =
        runTest {
            assertEquals(emptyList(), set("zelda", "file:///photos/a.jpg"))
            assertEquals(emptyList(), set("wii", "file:///photos/w.jpg"))
            assertEquals(listOf("file:///photos/a.jpg"), set("zelda", "file:///photos/b.jpg"))
            assertEquals(mapOf("zelda" to "file:///photos/b.jpg", "wii" to "file:///photos/w.jpg"), observe().first())

            assertEquals(listOf("file:///photos/b.jpg"), remove("zelda"))
            assertEquals(mapOf("wii" to "file:///photos/w.jpg"), observe().first())
            assertEquals(emptyList(), remove("zelda"))
        }
}
