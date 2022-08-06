package com.example.app.di

import com.android.billingclient.api.BillingClient
import com.example.app.billing.*
import com.example.app.ui.common.messager.Messenger
import com.example.app.ui.common.messager.ToastMessenger
import com.example.app.ui.main.MainFragmentViewModel
import com.example.app.utility.AndroidValues
import com.example.app.utility.Values
import com.mgsoftware.billing.*
import com.mgsoftware.billing.impl.DefaultBillingManager
import com.mgsoftware.billing.impl.DefaultPurchaseValidator
import com.mgsoftware.billing.impl.FakeBillingManager
import com.mgsoftware.billing.impl.FakeKeyProvider
import com.mgsoftware.billing.impl.PlayStoreDataSource
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<SkuProvider> { AppSkuProvider() }
    single<KeyProvider> { FakeKeyProvider() }
    single<PurchaseValidator> { DefaultPurchaseValidator(get()) }
    single { PlayStoreDataSource(androidApplication(), get(), get()) }
    single<BillingManager> {
        val impl = DefaultBillingManager(get())
        impl
    }

    single<BillingRepository> { BillingRepositoryImpl() }
    viewModel { BillingViewModel(get(), get()) }

    single<Values> { AndroidValues(androidContext()) }
    single<Messenger> { ToastMessenger(androidContext()) }
    viewModel { MainFragmentViewModel(get()) }
}

private fun provideFakeBillingManager(skuProvider: SkuProvider): BillingManager {
    val impl = FakeBillingManager(skuProvider)
    impl.addProductDetails(
        createFakeProductDetails(
            AppSkuProvider.GOLD_MONTHLY,
            BillingClient.SkuType.SUBS
        )
    )
    impl.addProductDetails(
        createFakeProductDetails(
            AppSkuProvider.PREMIUM_CAR,
            BillingClient.SkuType.INAPP
        )
    )
    impl.addProductDetails(
        createFakeProductDetails(
            AppSkuProvider.GAS,
            BillingClient.SkuType.INAPP
        )
    )
    return impl
}

private fun createFakeProductDetails(
    sku: String,
    @BillingClient.SkuType skuType: String
): SkuDetailsSnippet {
    return SkuDetailsSnippet(sku, skuType, "test", "test", "test")
}