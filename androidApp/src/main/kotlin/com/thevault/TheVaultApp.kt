package com.thevault

import com.thevault.alerts.WishAlertWorker
import android.app.Application
import com.thevault.di.androidModule
import com.thevault.di.appModule
import com.thevault.di.commonModule
import com.thevault.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/** Android Application class — initializes Koin DI. */
class TheVaultApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(level = Level.DEBUG)
            androidContext(this@TheVaultApp)
            modules(
                sharedModule,
                androidModule,
                appModule,
                commonModule,
            )
        }
        WishAlertWorker.schedule(this)
    }
}
