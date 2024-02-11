package com.mgsoftware.billing.api.model

import com.android.billingclient.api.BillingClient

enum class BillingResponseCode(
    val value: Int
) {
    SERVICE_TIMEOUT(BillingClient.BillingResponseCode.SERVICE_TIMEOUT),
    FEATURE_NOT_SUPPORTED(BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED),
    SERVICE_DISCONNECTED(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED),
    OK(BillingClient.BillingResponseCode.OK),
    USER_CANCELED(BillingClient.BillingResponseCode.USER_CANCELED),
    SERVICE_UNAVAILABLE(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE),
    BILLING_UNAVAILABLE(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE),
    ITEM_UNAVAILABLE(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE),
    DEVELOPER_ERROR(BillingClient.BillingResponseCode.DEVELOPER_ERROR),
    ERROR(BillingClient.BillingResponseCode.ERROR),
    ITEM_ALREADY_OWNED(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED),
    ITEM_NOT_OWNED(BillingClient.BillingResponseCode.ITEM_NOT_OWNED),
    NETWORK_ERROR(BillingClient.BillingResponseCode.NETWORK_ERROR);

    override fun toString(): String {
        return "$name($value)"
    }
}