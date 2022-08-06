package com.example.app.billing

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.mgsoftware.billing.BillingManager
import com.mgsoftware.billing.SkuDetailsSnippet
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

    fun launchBillingFlow(activity: Activity, sku: String) {
        viewModelScope.launch {
            billingManager.launchBillingFlow(activity, sku)
        }
    }

    override fun onBillingClientReady() {
        viewModelScope.launch {
            billingManager.fetchProductDetails(
                setOf(
                    AppSkuProvider.GOLD_MONTHLY
                ),
                BillingClient.SkuType.SUBS
            )
            billingManager.fetchProductDetails(
                setOf(
                    AppSkuProvider.PREMIUM_CAR,
                    AppSkuProvider.GAS,
                ),
                BillingClient.SkuType.INAPP
            )
            billingManager.fetchPurchases()
        }
    }

    override fun onProductDetailsListChanged(skuDetailsSnippetList: Set<SkuDetailsSnippet>) {
        viewModelScope.launch {
            billingRepository.updateSkuDetailsSnippetList(skuDetailsSnippetList.toSet())
        }
    }

    override fun onPurchasesListChanged(purchasesList: Set<String>) {
        viewModelScope.launch {
            billingRepository.updatePurchasesList(purchasesList.toSet())
        }
    }

    override fun disburseConsumableEntitlements(sku: String, quantity: Int) {
        Log.d("echo", "disburseConsumableEntitlements: sku=$sku")
    }

    override fun onPurchaseAcknowledged(sku: String) {
    }

    override fun onPurchaseConsumed(sku: String) {
    }
}