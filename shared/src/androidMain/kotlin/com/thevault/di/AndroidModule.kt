package com.thevault.di

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.thevault.database.TheVaultDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android-specific Koin module providing the SQLite database driver.
 * Must be loaded alongside [sharedModule] in the Android [Application] class.
 */
val androidModule =
    module {

        single {
            AndroidSqliteDriver(
                schema = TheVaultDatabase.Schema,
                context = androidContext(),
                name = "thevault.db",
            )
        }

        single {
            TheVaultDatabase(driver = get<AndroidSqliteDriver>())
        }
    }
