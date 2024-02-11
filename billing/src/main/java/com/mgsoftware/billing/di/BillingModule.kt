package com.mgsoftware.billing.di

import com.mgsoftware.billing.api.GooglePlayBillingManager
import com.mgsoftware.billing.api.BillingManager
import com.mgsoftware.billing.internal.productdetails.ProductDetailsStore
import com.mgsoftware.billing.api.validation.DefaultPurchaseValidator
import com.mgsoftware.billing.api.PurchaseValidator
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val billingModule = module {
    single { ProductDetailsStore() }
    single<PurchaseValidator> { DefaultPurchaseValidator(get()) }
    single<BillingManager> {
        GooglePlayBillingManager(
            context = androidApplication(),
            productIdProvider = get(),
            purchaseValidator = get(),
            productDetailsStore = get(),
        )
    }
}