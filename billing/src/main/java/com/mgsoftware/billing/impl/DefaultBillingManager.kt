package com.mgsoftware.billing.impl

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.mgsoftware.billing.BillingManager
import com.mgsoftware.billing.SkuDetailsSnippet
import com.mgsoftware.billing.common.BaseObservable

class DefaultBillingManager(
    private val playStoreBillingClient: PlayStoreDataSource
) : BaseObservable<BillingManager.Listener>(), BillingManager {
    private val skuDetailsMap = mutableMapOf<String, SkuDetails>()
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
        this.purchasesList.addAll(purchases.map { it.skus[0] })
        getListeners().forEach { it.onPurchasesListChanged(this.purchasesList) }
    }

    override fun onPurchaseAcknowledged(sku: String) {
        getListeners().forEach { it.onPurchaseAcknowledged(sku) }
    }

    override fun onPurchaseConsumed(sku: String, quantity: Int) {
        getListeners().forEach { it.onPurchaseConsumed(sku) }
        getListeners().forEach { it.disburseConsumableEntitlements(sku, quantity) }
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
        skuList: Set<String>,
        @BillingClient.SkuType skuType: String
    ) {
        if (playStoreBillingClient.isReady()) {
            val skuDetailsResult = playStoreBillingClient.querySkuDetails(skuList, skuType)
            val billingResult = skuDetailsResult.billingResult
            Log.d(
                TAG,
                "fetchProductDetails: responseCode=${billingResult.responseCode}, debugMessage=${billingResult.debugMessage}, skuDetailsList=${skuDetailsResult.skuDetailsList}"
            )
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                skuDetailsResult.skuDetailsList?.forEach {
                    skuDetailsMap[it.sku] = it
                }
                getListeners().forEach {
                    it.onProductDetailsListChanged(skuDetailsMap.values.map { it.toSkuDetailsSnippet() }
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

    override suspend fun launchBillingFlow(activity: Activity, sku: String) {
        val skuDetails = skuDetailsMap[sku]
        if (playStoreBillingClient.isReady() && skuDetails != null) {
            playStoreBillingClient.launchBillingFlow(
                activity,
                skuDetails
            )
        } else {
            Log.w(TAG, "Can't launchBillingFlow because playStoreBillingClient not ready yet.")
        }
    }

    private fun SkuDetails.toSkuDetailsSnippet(): SkuDetailsSnippet {
        return SkuDetailsSnippet(
            sku,
            type,
            price,
            title,
            description,
        )
    }

    companion object {
        private const val TAG = "BillingManager"
    }
}