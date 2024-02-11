package com.mgsoftware.billing.api.model

import com.android.billingclient.api.BillingClient

internal enum class FeatureType(@BillingClient.FeatureType val value: String) {
    SUBSCRIPTIONS(BillingClient.FeatureType.SUBSCRIPTIONS),
    PRODUCT_DETAILS(BillingClient.FeatureType.PRODUCT_DETAILS)
}