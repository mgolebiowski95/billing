package com.mgsoftware.billing.internal.connection

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener
import com.mgsoftware.billing.api.model.ConnectionState
import com.mgsoftware.billing.api.model.ConnectionStateMapper
import com.mgsoftware.billing.api.model.Constants
import com.mgsoftware.billing.utils.retry
import com.mgsoftware.billing.utils.startConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

internal class OldGooglePlayBillingConnection(
    private val context: Context,
    private val purchasesUpdatedListener: PurchasesUpdatedListener
) : BillingConnection {
    private var _billingClient: BillingClient? = null
    private val billingClient: BillingClient
        get() = _billingClient?.takeUnless { it.connectionState == BillingClient.ConnectionState.CLOSED }
            ?: prepareBillingClient().also {
                _billingClient = it
                updateState()
            }

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val state: StateFlow<ConnectionState>
        get() = _state

    override var onConnectionEstablished: (() -> Unit)? = null
    override var onBillingServiceDisconnected: (() -> Unit)? = null

    private fun prepareBillingClient() =
        BillingClient.newBuilder(context)
            .enablePendingPurchases()
            .setListener(purchasesUpdatedListener)
            .build()

    override suspend fun open() {
        if (billingClient.connectionState == BillingClient.ConnectionState.DISCONNECTED) {
            establishConnection()
            onConnectionEstablished?.invoke()
        }
    }

    private fun onBillingServiceDisconnected() {
        Timber.tag(Constants.LIBRARY_TAG).d("Connection lost.")
        updateState()
        onBillingServiceDisconnected?.invoke()
    }

    override fun close() {
        if (billingClient.connectionState == BillingClient.ConnectionState.CONNECTED) {
            Timber.tag(Constants.LIBRARY_TAG).d("Closing connection...")
            billingClient.endConnection()
            Timber.tag(Constants.LIBRARY_TAG).d("Connection closed.")
            updateState(ConnectionState.CLOSED)
            _billingClient = null
        }
    }

    override suspend fun <T> useBillingClient(block: suspend BillingClient.() -> T): T {
        if (billingClient.connectionState == BillingClient.ConnectionState.DISCONNECTED) {
            establishConnection()
        }
        return block(billingClient)
    }

    private suspend fun establishConnection() = retry(
        onFailedAttempt = {
            Timber.tag(Constants.LIBRARY_TAG).w("A attempt to establish a connection failed caused by $it")
            true
        }
    ) {
        Timber.tag(Constants.LIBRARY_TAG).d("Establish connection...")
        billingClient.startConnection(
            onStartExecution = ::updateState,
            onBillingServiceDisconnected = ::onBillingServiceDisconnected
        )
        Timber.tag(Constants.LIBRARY_TAG).d("Connection established.")
        updateState()
    }

    private fun updateState() =
        _state.update { ConnectionStateMapper.map(billingClient.connectionState) }

    private fun updateState(newState: ConnectionState) =
        _state.update { newState }
}