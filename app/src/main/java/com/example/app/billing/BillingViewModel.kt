package com.example.app.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.mgsoftware.billing.api.BillingManager
import kotlinx.coroutines.launch

class BillingViewModel(
    private val billingManager: BillingManager
) : ViewModel() {

    init {
        billingManager.openConnection()

        viewModelScope.launch {
            billingManager.connectionState().collect {
                Firebase.crashlytics.log("BillingConnectionState=$it")
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

    fun launchBillingFlow(
        activity: Activity,
        productId: String,
    ) = viewModelScope.launch {
        billingManager.launchBillingFlow(activity, productId)
    }
}