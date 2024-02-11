package com.mgsoftware.billing.api.model

import com.android.billingclient.api.BillingClient

internal class BillingResponseCodeMapper {
    companion object {
        fun map(@BillingClient.BillingResponseCode responseCode: Int): BillingResponseCode {
            return when (responseCode) {
                BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> BillingResponseCode.SERVICE_TIMEOUT
                BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> BillingResponseCode.FEATURE_NOT_SUPPORTED
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> BillingResponseCode.SERVICE_DISCONNECTED
                BillingClient.BillingResponseCode.OK -> BillingResponseCode.OK
                BillingClient.BillingResponseCode.USER_CANCELED -> BillingResponseCode.USER_CANCELED
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> BillingResponseCode.SERVICE_UNAVAILABLE
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> BillingResponseCode.BILLING_UNAVAILABLE
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> BillingResponseCode.ITEM_UNAVAILABLE
                BillingClient.BillingResponseCode.DEVELOPER_ERROR -> BillingResponseCode.DEVELOPER_ERROR
                BillingClient.BillingResponseCode.ERROR -> BillingResponseCode.ERROR
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> BillingResponseCode.ITEM_ALREADY_OWNED
                BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> BillingResponseCode.ITEM_NOT_OWNED
                BillingClient.BillingResponseCode.NETWORK_ERROR -> BillingResponseCode.NETWORK_ERROR
                else -> throw IllegalArgumentException("Unknown response code: $responseCode")
            }
        }
    }
}