package com.mgsoftware.billing.api.model

import com.android.billingclient.api.BillingClient

enum class ConnectionState(
    val value: Int
) {
    DISCONNECTED(BillingClient.ConnectionState.DISCONNECTED),
    CONNECTING(BillingClient.ConnectionState.CONNECTING),
    CONNECTED(BillingClient.ConnectionState.CONNECTED),
    CLOSED(BillingClient.ConnectionState.CLOSED);

    override fun toString(): String {
        return "$name($value)"
    }
}