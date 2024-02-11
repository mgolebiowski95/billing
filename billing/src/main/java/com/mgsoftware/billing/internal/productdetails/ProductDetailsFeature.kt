package com.mgsoftware.billing.internal.productdetails

import com.android.billingclient.api.BillingFlowParams
import com.mgsoftware.billing.api.model.ProductDetails
import kotlinx.coroutines.flow.StateFlow

internal interface ProductDetailsFeature {
    suspend fun fetchProductDetails()
    fun productDetails(productId: String): StateFlow<ProductDetails?>
    fun prepareBillingFlowParams(key: String): BillingFlowParams

}