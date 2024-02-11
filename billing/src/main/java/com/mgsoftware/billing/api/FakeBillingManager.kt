package com.mgsoftware.billing.api

import android.app.Activity
import com.mgsoftware.billing.api.model.BillingException
import com.mgsoftware.billing.api.model.ConnectionState
import com.mgsoftware.billing.api.model.ProductDetails
import com.mgsoftware.billing.api.model.ProductInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlin.random.Random

class FakeBillingManager(
    private val productIdProvider: ProductIdProvider
) : BillingManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val _onNewProductPurchased = MutableSharedFlow<String>()
    override val onNewProductPurchased: SharedFlow<String>
        get() = _onNewProductPurchased

    val _onBillingError = MutableSharedFlow<BillingException>()
    override val onBillingError: SharedFlow<BillingException>
        get() = _onBillingError

    private val connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)

    override fun connectionState(): StateFlow<ConnectionState> = connectionState

    private val productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    private val isPurchasedMap = mutableMapOf<String, MutableStateFlow<Boolean>>()

    override fun openConnection() {
        scope.launch {
            connectionState.emit(ConnectionState.CONNECTING)
            randomDelay()
            connectionState.emit(ConnectionState.CONNECTED)
        }
    }

    override fun closeConnection() {
        scope.launch {
            connectionState.emit(ConnectionState.CLOSED)
        }
    }

    override fun fetchProductDetails() {
        scope.launch {
            randomDelay()
            productDetails.emit(
                listOf(
                    productIdProvider.getKnownInappProductIds().map {
                        ProductDetails(
                            it,
                            it,
                            it,
                            "price"
                        )
                    },
                    productIdProvider.getKnownSubscriptionProductIds().map {
                        ProductDetails(
                            it,
                            it,
                            it,
                            "price"
                        )
                    }
                ).flatten()
            )
        }
    }

    override fun isPurchased(productId: String): StateFlow<Boolean> {
        return isPurchasedMap.getOrPut(productId) { MutableStateFlow(false) }
    }

    override fun fetchProducts() = Unit

    override fun launchBillingFlow(activity: Activity, productId: String) {
        scope.launch {
            if (!productIdProvider.getAutoConsumeProductIds().contains(productId)) {
                isPurchasedMap.getOrPut(productId) { MutableStateFlow(false) }.emit(true)
            }
            _onNewProductPurchased.emit(productId)
        }
    }

    override fun getProductInfo(productId: String): Flow<ProductInfo> {
        return combine(
            productDetails.mapNotNull { it.find { it.productId == productId } },
            isPurchasedMap.getOrPut(productId) { MutableStateFlow(false) }
        ) { productDetails, isPurchased ->
            ProductInfo(productDetails, !isPurchased)
        }
    }

    override fun dispose() {
        scope.cancel()
    }

    private suspend fun randomDelay() = delay(Random.nextLong(500L, 2000L))
}