package com.example.app.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.mgsoftware.billing.api.BillingManager
import com.mgsoftware.billing.api.model.ConnectionState
import kotlinx.coroutines.launch

class BillingViewModel(
    private val billingManager: BillingManager
) : ViewModel() {
    val connectionState = billingManager.connectionState()

    init {
        billingManager.openConnection()

        viewModelScope.launch {
            billingManager.connectionState().collect {
                Firebase.crashlytics.log("BillingConnectionState=$it")

                if(it == ConnectionState.CONNECTED) {
                    billingManager.fetchProductDetails()
                }
            }
        }
    }

    override fun onCleared() {
        billingManager.closeConnection()
        billingManager.dispose()
    }

    fun openConnection() = billingManager.openConnection()

    fun closeConnection() = billingManager.closeConnection()

    fun fetchPurchases() = billingManager.fetchProducts()

    fun fetchProductDetails() = billingManager.fetchProductDetails()

    fun launchBillingFlow(
        activity: Activity,
        productId: String,
    ) = viewModelScope.launch {
        billingManager.launchBillingFlow(activity, productId)
    }
}