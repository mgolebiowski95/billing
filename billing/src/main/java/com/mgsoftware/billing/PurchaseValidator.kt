package com.mgsoftware.billing

import com.android.billingclient.api.Purchase

interface PurchaseValidator {

    fun verifyPurchase(purchase: Purchase): Boolean
}