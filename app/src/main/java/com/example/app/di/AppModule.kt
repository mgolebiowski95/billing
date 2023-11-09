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
    single<ProductIdProvider> { AppProductIdProvider() }
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

private fun provideFakeBillingManager(productIdProvider: ProductIdProvider): BillingManager {
    val impl = FakeBillingManager(productIdProvider)
    impl.addProductDetails(
        createFakeProductDetails(
            AppProductIdProvider.GOLD_MONTHLY,
            BillingClient.ProductType.SUBS
        )
    )
    impl.addProductDetails(
        createFakeProductDetails(
            AppProductIdProvider.PREMIUM_CAR,
            BillingClient.ProductType.INAPP
        )
    )
    impl.addProductDetails(
        createFakeProductDetails(
            AppProductIdProvider.GAS,
            BillingClient.ProductType.INAPP
        )
    )
    return impl
}

private fun createFakeProductDetails(
    productId: String,
    @BillingClient.ProductType productType: String
): ProductDetailsSnippet {
    return ProductDetailsSnippet(productId, productType, "test", "test", "test")
}