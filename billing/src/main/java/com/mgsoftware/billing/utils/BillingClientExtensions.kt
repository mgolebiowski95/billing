package com.mgsoftware.billing.utils

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.mgsoftware.billing.api.model.BillingException
import com.mgsoftware.billing.api.model.FeatureType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal fun BillingClient.isFeatureSupported(type: FeatureType): Boolean {
    val billingResult = isFeatureSupported(type.value)
    return when (billingResult.responseCode) {
        BillingResponseCode.OK -> true
        else -> false
    }
}

internal suspend fun BillingClient.startConnection(
    onStartExecution: () -> Unit,
    onBillingServiceDisconnected: () -> Unit
) = suspendCoroutine {
    startConnection(object : BillingClientStateListener {
        override fun onBillingServiceDisconnected() = onBillingServiceDisconnected()

        override fun onBillingSetupFinished(billingResult: BillingResult) {
            if (billingResult.responseCode == BillingResponseCode.OK) {
                it.resume(Unit)
            } else {
                it.resumeWithException(BillingException(billingResult))
            }
        }
    })
    onStartExecution()
}

internal suspend fun BillingClient.getProductDetails(
    params: QueryProductDetailsParams
): List<ProductDetails>? {
    val (billingResult, productDetails) = queryProductDetails(params)
    if (billingResult.responseCode == BillingResponseCode.OK) {
        return productDetails
    } else {
        throw BillingException(billingResult)
    }
}

internal suspend fun BillingClient.getPurchases(
    params: QueryPurchasesParams
): List<Purchase> {
    val (billingResult, skuDetails) = queryPurchasesAsync(params)
    if (billingResult.responseCode == BillingResponseCode.OK) {
        return skuDetails
    } else {
        throw BillingException(billingResult)
    }
}
