package com.example.app

import android.app.Application
import com.example.app.di.appModule
import com.mgsoftware.billing.BillingManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MyApplication)
            modules(appModule)
        }

        Timber.plant(Timber.DebugTree())
    }
}