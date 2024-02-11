package com.mgsoftware.billing.internal.products

import android.app.Activity
import com.android.billingclient.api.PurchasesUpdatedListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

internal interface ProductsFeature: PurchasesUpdatedListener {
    val onNewProductPurchased: SharedFlow<String>
    val billingFlowInProgress: StateFlow<Boolean>

    suspend fun fetchProducts()
    fun isPurchased(productId: String): Flow<Boolean>
    fun canPurchase(productId: String): Flow<Boolean>
    suspend fun launchBillingFlow(activity: Activity, productId: String)
}