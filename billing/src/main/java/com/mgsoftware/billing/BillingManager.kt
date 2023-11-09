package com.mgsoftware.billing

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.mgsoftware.billing.common.Observable
import com.mgsoftware.billing.impl.PlayStoreDataSource

interface BillingManager : Observable<BillingManager.Listener>, PlayStoreDataSource.Listener {

    interface Listener {

        fun onBillingClientReady()

        fun onProductDetailsListChanged(productDetailsSnippetList: Set<ProductDetailsSnippet>)

        fun onPurchasesListChanged(purchasesList: Set<String>)

        fun disburseConsumableEntitlements(productId: String, quantity: Int)

        fun onPurchaseAcknowledged(productId: String)

        fun onPurchaseConsumed(productId: String)
    }

    override fun onBillingSetupSuccess()

    override fun onBillingSetupFailed(errorCode: Int, errorMessage: String)

    /**
     * @param purchases contains all acknowledged products
     * @param callFromOnPurchasesUpdated means that call fire in onPurchaseUpdated(), when you only queryPurchases this value is false
     */
    override fun disburseNonConsumableEntitlements(
        purchases: Set<Purchase>,
        callFromOnPurchasesUpdated: Boolean
    )

    fun openPlayStoreConnection()

    fun closePlayStoreConnection()

    suspend fun fetchProductDetails(productIds: Set<String>, @BillingClient.ProductType productType: String)

    suspend fun fetchPurchases()

    suspend fun launchBillingFlow(activity: Activity, productId: String)
}