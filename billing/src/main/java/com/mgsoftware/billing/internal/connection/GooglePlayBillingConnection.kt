package com.mgsoftware.billing.internal.connection

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import com.mgsoftware.billing.api.model.ConnectionState
import com.mgsoftware.billing.api.model.ConnectionStateMapper
import com.mgsoftware.billing.api.model.Constants
import com.mgsoftware.billing.utils.retry
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import timber.log.Timber

class GooglePlayBillingConnection(
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

    private fun prepareBillingClient(): BillingClient {
        val params = preparePendingPurchasesParams()
        return BillingClient.newBuilder(context)
            .enableAutoServiceReconnection()
            .enablePendingPurchases(params)
            .setListener(purchasesUpdatedListener)
            .build()
    }

    private fun preparePendingPurchasesParams() = PendingPurchasesParams
        .newBuilder()
        .enableOneTimeProducts()
        .build()

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val state: StateFlow<ConnectionState>
        get() = _state

    private val billingClientStateListener = object : BillingClientStateListener {
        override fun onBillingServiceDisconnected() {
            Timber.tag(Constants.LIBRARY_TAG).d("Connection lost.")
            updateState()
            onBillingServiceDisconnected?.invoke()
        }

        override fun onBillingSetupFinished(billingResult: BillingResult) {
            billingResultChannel.trySend(billingResult)
        }
    }

    private val billingResultChannel = Channel<BillingResult>()
    private val mutex = Mutex()
    override var onBillingServiceDisconnected: (() -> Unit)? = null

    override suspend fun open() {
        establishConnectionOrThrowException()
    }

    override fun close() {
        if (billingClient.connectionState == BillingClient.ConnectionState.CONNECTED) {
            Timber.tag(Constants.LIBRARY_TAG).d("Closing connection...")
            billingClient.endConnection()
            Timber.tag(Constants.LIBRARY_TAG).d("Connection closed.")
            updateState()
            _billingClient = null
        }
    }

    override suspend fun <T> useBillingClient(block: suspend BillingClient.() -> T): T {
        if (billingClient.connectionState == BillingClient.ConnectionState.DISCONNECTED) {
            establishConnectionOrThrowException()
        }
        return block(billingClient)
    }

    private suspend fun establishConnectionOrThrowException() = mutex.withLock {
        retry(
            initialDelayBeforeNextAttempt = 500,
            retries = 4,
            onFailedAttempt = {
                Timber
                    .tag(Constants.LIBRARY_TAG)
                    .w("A attempt to establish a connection failed caused by $it")
                true
            }
        ) {
            updateState()
            if (state.value == ConnectionState.CONNECTED) {
                return@retry
            }

            Timber.tag(Constants.LIBRARY_TAG).d("Establish connection...")
            billingClient.startConnection(billingClientStateListener)
            updateState()
            withTimeout(8000) {
                billingResultChannel.receive()
            }
            updateState()
            when (billingClient.connectionState) {
                BillingClient.ConnectionState.CONNECTED -> {
                    Timber.tag(Constants.LIBRARY_TAG).d("Connection established.")
                }

                else -> {
                    throw Exception("Not yet connected.")
                }
            }
        }
    }

    private fun updateState() =
        _state.update { ConnectionStateMapper.map(billingClient.connectionState) }
}
