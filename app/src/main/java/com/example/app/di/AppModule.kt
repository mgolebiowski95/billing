package com.example.app.di

import android.util.Log
import com.example.app.billing.AppProductIdProvider
import com.example.app.ui.MainActivityViewModel
import com.example.app.billing.BillingViewModel
import com.example.app.ui.common.appstate.AppStateManager
import com.example.app.ui.common.messager.Messenger
import com.example.app.ui.common.messager.ToastMessenger
import com.example.app.utility.AndroidValues
import com.example.app.utility.Values
import com.mgsoftware.billing.api.validation.KeyProvider
import com.mgsoftware.billing.api.ProductIdProvider
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single<Values> { AndroidValues(androidContext()) }
    single<Messenger> { ToastMessenger(androidContext()) }
    single { AppStateManager() }

    single<ProductIdProvider> { AppProductIdProvider() }
    single<KeyProvider> {
        object : KeyProvider {
            override fun getLicenceKey(): String {
                return ""
            }
        }
    }
    viewModelOf(::BillingViewModel)

    viewModel { MainActivityViewModel(get(), get()) }
}