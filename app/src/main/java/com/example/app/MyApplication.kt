package com.example.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.app.di.appModule
import com.example.app.ui.common.appstate.AppStateManager
import com.mgsoftware.billing.di.billingModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val koinApplication = startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MyApplication)
            modules(appModule, billingModule)
        }
        val appStateManager: AppStateManager = koinApplication.koin.get()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appStateManager)

        Timber.plant(Timber.DebugTree())
    }
}