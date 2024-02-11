package com.mgsoftware.billing.api

import android.app.Activity
import android.content.Context
import com.mgsoftware.billing.api.model.BillingException
import com.mgsoftware.billing.api.model.BillingResponseCode
import com.mgsoftware.billing.api.model.Constants
import com.mgsoftware.billing.api.model.FeatureType
import com.mgsoftware.billing.api.model.ProductInfo
import com.mgsoftware.billing.internal.connection.BillingConnection
import com.mgsoftware.billing.internal.connection.GooglePlayBillingConnection
import com.mgsoftware.billing.internal.productdetails.ProductDetailsFeature
import com.mgsoftware.billing.internal.productdetails.ProductDetailsFeatureImpl
import com.mgsoftware.billing.internal.productdetails.ProductDetailsStore
import com.mgsoftware.billing.internal.productdetails.SkuDetailsFeatureImpl
import com.mgsoftware.billing.internal.products.ProductsFeature
import com.mgsoftware.billing.internal.products.ProductsFeatureImpl
import com.mgsoftware.billing.utils.isFeatureSupported
import com.mgsoftware.billing.utils.retry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

internal class GooglePlayBillingManager(
    context: Context,
    private val productIdProvider: ProductIdProvider,
    purchaseValidator: PurchaseValidator,
    private val productDetailsStore: ProductDetailsStore,
) : BillingManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val connection: BillingConnection = GooglePlayBillingConnection(
        context = context,
        purchasesUpdatedListener = { billingResult, purchases ->
            productsFeature.onPurchasesUpdated(billingResult, purchases)
        },
    )
    private lateinit var productDetailsFeature: ProductDetailsFeature
    private val productsFeature: ProductsFeature = ProductsFeatureImpl(
        connection = connection,
        getProductDetailsFeature = { productDetailsFeature },
        productIdProvider = productIdProvider,
        purchaseValidator = purchaseValidator
    )

    override val onNewProductPurchased: SharedFlow<String> = productsFeature.onNewProductPurchased
    private val _onBillingError = MutableSharedFlow<BillingException>()
    override val onBillingError: SharedFlow<BillingException>
        get() = _onBillingError

    init {
        connection.onConnectionEstablished = {
            scope.launch {
                prepareProductDetailsFeature()
                fetchProductDetails()
                fetchProducts()
            }
        }
        connection.onBillingServiceDisconnected = {
            scope.launch {
                connection.open()
            }
        }
    }

    private suspend fun prepareProductDetailsFeature(): ProductDetailsFeature =
        if (::productDetailsFeature.isInitialized) productDetailsFeature else connection.useBillingClient {
            if (isFeatureSupported(FeatureType.PRODUCT_DETAILS)) {
                ProductDetailsFeatureImpl(connection, productIdProvider, productDetailsStore)
            } else {
                SkuDetailsFeatureImpl(connection, productIdProvider, productDetailsStore)
            }
        }.also { productDetailsFeature = it }

    override fun connectionState() = connection.state

    override fun openConnection() {
        scope.launch {
            try {
                connection.open()
            } catch (e: BillingException) {
                _onBillingError.emit(e)
            }
        }
    }

    override fun closeConnection() = connection.close()

    override fun fetchProductDetails() {
        scope.launch {
            retry(
                onFailedAttempt = {
                    Timber.tag(Constants.LIBRARY_TAG).w("A attempt to fetch product details failed caused by $it")
                    if (it !is BillingException) return@retry false

                    when (it.responseCode) {
                        BillingResponseCode.SERVICE_DISCONNECTED -> {
                            connection.open()
                        }

                        else -> Unit
                    }
                    true
                },
                onFailure = {
                    if (it is BillingException) {
                        _onBillingError.emit(it)
                    }
                }
            ) {
                Timber.tag(Constants.LIBRARY_TAG).d("A attempt to fetch product details...")
                productDetailsFeature.fetchProductDetails()
            }
        }
    }


    override fun isPurchased(productId: String): StateFlow<Boolean> =
        productsFeature
            .isPurchased(productId)
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    override fun fetchProducts() {
        scope.launch {
            retry(
                onFailedAttempt = {
                    Timber.tag(Constants.LIBRARY_TAG).w("A attempt to fetch purchases failed caused by $it")
                    if (it !is BillingException) return@retry false

                    when (it.responseCode) {
                        BillingResponseCode.SERVICE_DISCONNECTED -> {
                            connection.open()
                        }

                        BillingResponseCode.ITEM_NOT_OWNED -> {
                            productsFeature.fetchProducts()
                        }

                        else -> Unit
                    }
                    true
                },
                onFailure = {
                    if (it is BillingException) {
                        _onBillingError.emit(it)
                    }
                }
            ) {
                Timber.tag(Constants.LIBRARY_TAG).d("A attempt to Fetch purchases...")
                productsFeature.fetchProducts()
            }
        }
    }

    override fun launchBillingFlow(activity: Activity, productId: String) {
        scope.launch {
            retry(
                onFailedAttempt = {
                    Timber.tag(Constants.LIBRARY_TAG).w("A attempt to launch billing flow failed caused by $it")
                    if (it !is BillingException) return@retry false

                    when (it.responseCode) {
                        BillingResponseCode.SERVICE_DISCONNECTED -> {
                            connection.open()
                            true
                        }

                        BillingResponseCode.ITEM_ALREADY_OWNED -> {
                            productsFeature.fetchProducts()
                            false
                        }

                        else -> false
                    }
                },
                onFailure = {
                    if (it is BillingException && it.responseCode != BillingResponseCode.USER_CANCELED) {
                        _onBillingError.emit(it)
                    }
                }
            ) {
                productsFeature.launchBillingFlow(activity, productId)
            }
        }
    }

    override fun getProductInfo(productId: String): Flow<ProductInfo> = combine(
        productDetailsStore.get(productId).filterNotNull(),
        productsFeature.canPurchase(productId),
    ) { productDetails, canPurchase ->
        ProductInfo(productDetails, canPurchase)
    }

    override fun dispose() = scope.cancel()
}