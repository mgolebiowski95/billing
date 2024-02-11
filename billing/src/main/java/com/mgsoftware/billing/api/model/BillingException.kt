package com.mgsoftware.billing.api.model

import com.android.billingclient.api.BillingResult

class BillingException internal constructor(billingResult: BillingResult) : Exception() {
    val responseCode: BillingResponseCode =
        BillingResponseCodeMapper.map(billingResult.responseCode)

    override val message: String = "$responseCode: ${billingResult.debugMessage}"
}

