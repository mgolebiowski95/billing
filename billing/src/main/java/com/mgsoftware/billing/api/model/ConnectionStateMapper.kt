package com.mgsoftware.billing.api.model

import com.android.billingclient.api.BillingClient

internal class ConnectionStateMapper {
    companion object {
        fun map(@BillingClient.ConnectionState connectionState: Int): ConnectionState {
            return when (connectionState) {
                BillingClient.ConnectionState.DISCONNECTED -> ConnectionState.DISCONNECTED
                BillingClient.ConnectionState.CONNECTING -> ConnectionState.CONNECTING
                BillingClient.ConnectionState.CONNECTED -> ConnectionState.CONNECTED
                BillingClient.ConnectionState.CLOSED -> ConnectionState.CLOSED
                else -> throw IllegalArgumentException("Unknown connectionState: $connectionState")
            }
        }
    }
}