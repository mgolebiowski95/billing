package com.mgsoftware.billing.impl

import android.app.Activity
import com.android.billingclient.api.Purchase
import com.mgsoftware.billing.BillingManager
import com.mgsoftware.billing.ProductDetailsSnippet
import com.mgsoftware.billing.ProductIdProvider
import com.mgsoftware.billing.common.BaseObservable
import com.mgsoftware.billing.utility.intersect

class FakeBillingManager(
    private val productIdProvider: ProductIdProvider,

    ) : BaseObservable<BillingManager.Listener>(),
    BillingManager {

    // remote
    private val _productDetailsSnippetMap = mutableMapOf<String, ProductDetailsSnippet>()

    // local
    private val productDetailsSnippetMap = mutableMapOf<String, ProductDetailsSnippet>()

    // remote
    private val _purchasesList = mutableSetOf<String>()

    // local
    private val purchasesList = mutableSetOf<String>()

    fun addProductDetails(productDetailsSnippet: ProductDetailsSnippet) {
        _productDetailsSnippetMap[productDetailsSnippet.id] = productDetailsSnippet
    }

    fun addPurchase(productId: String) {
        _purchasesList.add(productId)
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

    override fun onPurchaseAcknowledged(productId: String) {
    }

    override fun onPurchaseConsumed(productId: String, quantity: Int) {
    }

    override fun openPlayStoreConnection() {
        onBillingSetupSuccess()
    }

    override fun closePlayStoreConnection() {
    }

    override suspend fun fetchProductDetails(productIds: Set<String>, productType: String) {
        getListeners().forEach {
            _productDetailsSnippetMap.values.intersect { productIds.contains(it.id) }.forEach {
                productDetailsSnippetMap[it.id] = it
            }
            it.onProductDetailsListChanged(productDetailsSnippetMap.values.toSet())
        }
    }

    override suspend fun fetchPurchases() {
        purchasesList.addAll(_purchasesList)
    }

    override suspend fun launchBillingFlow(activity: Activity, productId: String) {

        // fake acknowledge acknowledged product
        if (productIdProvider.getNonConsumableProductIds().contains(productId)) {
            getListeners().forEach { it.onPurchaseAcknowledged(productId) }
            purchasesList.add(productId)
            getListeners().forEach { it.onPurchasesListChanged(purchasesList) }
        }

        // fake consuming consumable product
        if (productIdProvider.getConsumableProductIds().contains(productId)) {
            getListeners().forEach {
                it.onPurchaseConsumed(productId)
            }
            getListeners().forEach {
                it.disburseConsumableEntitlements(productId, 1)
            }
        }
    }
}