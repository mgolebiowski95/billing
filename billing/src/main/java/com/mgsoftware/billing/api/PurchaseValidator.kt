package com.mgsoftware.billing.api

import com.android.billingclient.api.Purchase

interface PurchaseValidator {

    suspend fun verifyPurchase(purchase: Purchase): Boolean
}