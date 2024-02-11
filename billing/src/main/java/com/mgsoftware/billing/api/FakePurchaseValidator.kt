package com.mgsoftware.billing.api

import com.android.billingclient.api.Purchase

class FakePurchaseValidator : PurchaseValidator {
    var verifyResult = true

    override suspend fun verifyPurchase(purchase: Purchase): Boolean = verifyResult
}