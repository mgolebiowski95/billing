package com.example.app.billing

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.mgsoftware.billing.BillingManager
import com.mgsoftware.billing.ProductDetailsSnippet
import kotlinx.coroutines.launch

class BillingViewModel(
    private val billingManager: BillingManager,
    private val billingRepository: BillingRepository
) : ViewModel(), BillingManager.Listener {

    init {
        billingManager.registerListener(this)
        billingManager.openPlayStoreConnection()
    }

    override fun onCleared() {
        super.onCleared()
        billingManager.unregisterListener(this)
        billingManager.closePlayStoreConnection()
    }

    fun launchBillingFlow(activity: Activity, productId: String) {
        viewModelScope.launch {
            billingManager.launchBillingFlow(activity, productId)
        }
    }

    override fun onBillingClientReady() {
        viewModelScope.launch {
            billingManager.fetchProductDetails(
                setOf(
                    AppProductIdProvider.GOLD_MONTHLY
                ),
                BillingClient.ProductType.SUBS
            )
            billingManager.fetchProductDetails(
                setOf(
                    AppProductIdProvider.PREMIUM_CAR,
                    AppProductIdProvider.GAS,
                ),
                BillingClient.ProductType.INAPP
            )
            billingManager.fetchPurchases()
        }
    }

    override fun onProductDetailsListChanged(productDetailsSnippetList: Set<ProductDetailsSnippet>) {
        viewModelScope.launch {
            billingRepository.updateProductDetailsSnippetList(productDetailsSnippetList.toSet())
        }
    }

    override fun onPurchasesListChanged(purchasesList: Set<String>) {
        viewModelScope.launch {
            billingRepository.updatePurchasesList(purchasesList.toSet())
        }
    }

    override fun disburseConsumableEntitlements(productId: String, quantity: Int) {
        Log.d("echo", "disburseConsumableEntitlements: productId=$productId")
    }

    override fun onPurchaseAcknowledged(productId: String) {
    }

    override fun onPurchaseConsumed(productId: String) {
    }
}