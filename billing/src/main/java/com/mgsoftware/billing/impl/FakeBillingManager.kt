package com.mgsoftware.billing.impl

import android.app.Activity
import com.android.billingclient.api.Purchase
import com.mgsoftware.billing.BillingManager
import com.mgsoftware.billing.SkuDetailsSnippet
import com.mgsoftware.billing.SkuProvider
import com.mgsoftware.billing.common.BaseObservable
import com.mgsoftware.billing.utility.intersect

class FakeBillingManager(
    private val skuProvider: SkuProvider,

    ) : BaseObservable<BillingManager.Listener>(),
    BillingManager {

    // remote
    private val _skuDetailsSnippetMap = mutableMapOf<String, SkuDetailsSnippet>()

    // local
    private val skuDetailsSnippetMap = mutableMapOf<String, SkuDetailsSnippet>()

    // remote
    private val _purchasesList = mutableSetOf<String>()

    // local
    private val purchasesList = mutableSetOf<String>()

    fun addProductDetails(skuDetailsSnippet: SkuDetailsSnippet) {
        _skuDetailsSnippetMap[skuDetailsSnippet.sku] = skuDetailsSnippet
    }

    fun addPurchase(sku: String) {
        _purchasesList.add(sku)
    }

    override fun onBillingSetupSuccess() {
        getListeners().forEach { it.onBillingClientReady() }
    }

    override fun onBillingSetupFailed(errorCode: Int, errorMessage: String) {
    }

    override fun disburseNonConsumableEntitlements(
        purchases: Set<Purchase>,
        callFromOnPurchasesUpdated: Boolean
    ) {
    }

    override fun onPurchaseAcknowledged(sku: String) {
    }

    override fun onPurchaseConsumed(sku: String, quantity: Int) {
    }

    override fun openPlayStoreConnection() {
        onBillingSetupSuccess()
    }

    override fun closePlayStoreConnection() {
    }

    override suspend fun fetchProductDetails(skuList: Set<String>, skuType: String) {
        getListeners().forEach {
            _skuDetailsSnippetMap.values.intersect { skuList.contains(it.sku) }.forEach {
                skuDetailsSnippetMap[it.sku] = it
            }
            it.onProductDetailsListChanged(skuDetailsSnippetMap.values.toSet())
        }
    }

    override suspend fun fetchPurchases() {
        purchasesList.addAll(_purchasesList)
    }

    override suspend fun launchBillingFlow(activity: Activity, sku: String) {

        // fake acknowledge acknowledged product
        if (skuProvider.getNonConsumableSkus().contains(sku)) {
            getListeners().forEach { it.onPurchaseAcknowledged(sku) }
            purchasesList.add(sku)
            getListeners().forEach { it.onPurchasesListChanged(purchasesList) }
        }

        // fake consuming consumable product
        if (skuProvider.getConsumableSkus().contains(sku)) {
            getListeners().forEach {
                it.onPurchaseConsumed(sku)
            }
            getListeners().forEach {
                it.disburseConsumableEntitlements(sku, 1)
            }
        }
    }
}