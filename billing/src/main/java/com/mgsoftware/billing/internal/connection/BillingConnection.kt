package com.mgsoftware.billing.internal.connection

import com.android.billingclient.api.BillingClient
import com.mgsoftware.billing.api.model.ConnectionState
import kotlinx.coroutines.flow.StateFlow

internal interface BillingConnection {
    val state: StateFlow<ConnectionState>

    var onConnectionEstablished: (() -> Unit)?
    var onBillingServiceDisconnected: (() -> Unit)?

    suspend fun open()

    fun close()

    /**
     * safe call action on billing client or throw exception if connection isn't established
     */
    suspend fun <T> useBillingClient(block: suspend BillingClient.() -> T): T

}