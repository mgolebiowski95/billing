package com.mgsoftware.billing.api

import android.app.Activity
import com.mgsoftware.billing.api.model.BillingException
import com.mgsoftware.billing.api.model.ConnectionState
import com.mgsoftware.billing.api.model.ProductInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BillingManager {
    val onNewProductPurchased: SharedFlow<String>
    val onBillingError: SharedFlow<BillingException>

    fun connectionState(): StateFlow<ConnectionState>
    fun openConnection()
    fun closeConnection()

    fun fetchProductDetails()

    fun isPurchased(productId: String): StateFlow<Boolean>
    fun fetchProducts()
    fun launchBillingFlow(
        activity: Activity,
        productId: String
    )

    fun getProductInfo(productId: String): Flow<ProductInfo>
    fun dispose()
}