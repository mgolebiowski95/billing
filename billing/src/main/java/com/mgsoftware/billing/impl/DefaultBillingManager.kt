package com.mgsoftware.billing.impl

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.mgsoftware.billing.BillingManager
import com.mgsoftware.billing.ProductDetailsSnippet
import com.mgsoftware.billing.common.BaseObservable

class DefaultBillingManager(
    private val playStoreBillingClient: PlayStoreDataSource
) : BaseObservable<BillingManager.Listener>(), BillingManager {
    private val productIdDetailsMap = mutableMapOf<String, ProductDetails>()
    private val purchasesList = mutableSetOf<String>()

    init {
        playStoreBillingClient.registerListener(this)
    }

    override fun onBillingSetupSuccess() {
        getListeners().forEach { it.onBillingClientReady() }
    }

    override fun onBillingSetupFailed(errorCode: Int, errorMessage: String) {
        Log.e(TAG, "Unable to connect billing service.")
    }

    /**
     * @param purchases contains all acknowledged products
     * @param callFromOnPurchasesUpdated means that call fire in onPurchaseUpdated(), when you only queryPurchases this value is false
     */
    override fun disburseNonConsumableEntitlements(
        purchases: Set<Purchase>,
        callFromOnPurchasesUpdated: Boolean
    ) {
        if (!callFromOnPurchasesUpdated)
            this.purchasesList.clear()
        this.purchasesList.addAll(purchases.map { it.products[0] })
        getListeners().forEach { it.onPurchasesListChanged(this.purchasesList) }
    }

    override fun onPurchaseAcknowledged(productId: String) {
        getListeners().forEach { it.onPurchaseAcknowledged(productId) }
    }

    override fun onPurchaseConsumed(productId: String, quantity: Int) {
        getListeners().forEach { it.onPurchaseConsumed(productId) }
        getListeners().forEach { it.disburseConsumableEntitlements(productId, quantity) }
    }

    override fun openPlayStoreConnection() {
        playStoreBillingClient.openConnection()
        Log.d(TAG, "openPlayStoreConnection")
    }

    override fun closePlayStoreConnection() {
        playStoreBillingClient.closeConnection()
        Log.d(TAG, "closePlayStoreConnection")
    }

    override suspend fun fetchProductDetails(
        productIds: Set<String>,
        @BillingClient.ProductType productType: String
    ) {
        if (playStoreBillingClient.isReady()) {
            val productDetailsResult = playStoreBillingClient.queryProductDetails(productIds, productType)
            val billingResult = productDetailsResult.billingResult
            Log.d(
                TAG,
                "fetchProductDetails: responseCode=${billingResult.responseCode}, debugMessage=${billingResult.debugMessage}, productDetailsList=${productDetailsResult.productDetailsList}"
            )
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsResult.productDetailsList?.forEach {
                    productIdDetailsMap[it.productId] = it
                }
                getListeners().forEach {
                    it.onProductDetailsListChanged(productIdDetailsMap.values.map { it.toProductDetailsSnippet() }
                        .toSet())
                }
            }
        } else {
            Log.w(TAG, "Can't fetchProductDetails because playStoreBillingClient not ready yet.")
        }
    }

    override suspend fun fetchPurchases() {
        if (playStoreBillingClient.isReady()) {
            playStoreBillingClient.queryPurchasesAsync()
        }
    }

    override suspend fun launchBillingFlow(activity: Activity, productId: String) {
        val productDetails = productIdDetailsMap[productId]
        if (playStoreBillingClient.isReady() && productDetails != null) {
            playStoreBillingClient.launchBillingFlow(
                activity,
                productDetails
            )
        } else {
            Log.w(TAG, "Can't launchBillingFlow because playStoreBillingClient not ready yet.")
        }
    }

    private fun ProductDetails.toProductDetailsSnippet(): ProductDetailsSnippet {
        return ProductDetailsSnippet(
            productId,
            productType,
            oneTimePurchaseOfferDetails
                ?.formattedPrice
                ?: subscriptionOfferDetails
                    ?.get(0)
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.get(0)
                    ?.formattedPrice
                ?: "???",
            title,
            description,
        )
    }

    companion object {
        private const val TAG = "BillingManager"
    }
}