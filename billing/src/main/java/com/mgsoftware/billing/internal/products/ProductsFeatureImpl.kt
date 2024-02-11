package com.mgsoftware.billing.internal.products

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.mgsoftware.billing.api.ProductIdProvider
import com.mgsoftware.billing.api.PurchaseValidator
import com.mgsoftware.billing.api.model.BillingException
import com.mgsoftware.billing.api.model.Constants
import com.mgsoftware.billing.api.model.FeatureType
import com.mgsoftware.billing.internal.connection.BillingConnection
import com.mgsoftware.billing.internal.productdetails.ProductDetailsFeature
import com.mgsoftware.billing.utils.getPurchases
import com.mgsoftware.billing.utils.isFeatureSupported
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * query purchases
 * launch billing flow
 */
internal class ProductsFeatureImpl(
    private val connection: BillingConnection,
    private val getProductDetailsFeature: () -> ProductDetailsFeature,
    private val productIdProvider: ProductIdProvider,
    private val purchaseValidator: PurchaseValidator,
) : ProductsFeature, PurchasesUpdatedListener {
    private val purchaseChannel = Channel<PurchaseResult>()
    override val billingFlowInProgress = MutableStateFlow(false)

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        purchaseChannel.trySend(
            PurchaseResult(
                billingResult,
                purchases.orEmpty()
            )
        )
    }

    private val productStateMap = mutableMapOf<String, MutableStateFlow<ProductState>>()

    private val purchaseConsumptionInProcess = mutableSetOf<Purchase>()
    private val purchaseAcknowledgeInProcess = mutableSetOf<Purchase>()

    private val _onNewProductPurchased = MutableSharedFlow<String>()
    override val onNewProductPurchased: SharedFlow<String> get() = _onNewProductPurchased

    override suspend fun fetchProducts() = connection.useBillingClient {
        coroutineScope {
            launch {
                internalFetchPurchases(ProductType.INAPP)
            }
            if (isFeatureSupported(FeatureType.SUBSCRIPTIONS)) {
                launch {
                    internalFetchPurchases(ProductType.SUBS)
                }
            }
        }
    }

    private suspend fun BillingClient.internalFetchPurchases(@ProductType type: String) {
        Timber.tag(Constants.LIBRARY_TAG).d("Fetching '$type' purchases...")
        val params = prepareQueryPurchaseParams(type)
        val purchases = getPurchases(params)
        Timber.tag(Constants.LIBRARY_TAG).d(
            "'$type' purchases fetched: ${
                purchases.joinToString(
                    separator = ", ",
                    transform = { it.products.joinToString(", ") }
                )
            }"
        )
        handlePurchases(purchases)
    }

    private fun prepareQueryPurchaseParams(@ProductType productType: String) =
        QueryPurchasesParams
            .newBuilder()
            .setProductType(productType)
            .build()

    override suspend fun launchBillingFlow(
        activity: Activity,
        productId: String
    ) = connection.useBillingClient {
        if (billingFlowInProgress.value) {
            return@useBillingClient
        }
        val params = getProductDetailsFeature().prepareBillingFlowParams(productId)
        Timber.tag(Constants.LIBRARY_TAG).d("Launch billing flow UI for '$productId'...")
        val billingResult = launchBillingFlow(activity, params)
        if (billingResult.responseCode == BillingResponseCode.OK) {
            Timber.tag(Constants.LIBRARY_TAG).d("Launching billing flow UI successful.")
            billingFlowInProgress.emit(true)
        } else {
            Timber.tag(Constants.LIBRARY_TAG).d("Launch billing flow UI failed.")
            throw BillingException(billingResult)
        }
        awaitForBillingFlowResult()
    }

    private suspend fun ProductsFeatureImpl.awaitForBillingFlowResult() {
        val (billingResult, purchases) = purchaseChannel.receive()
        billingFlowInProgress.emit(false)
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                Timber.tag(Constants.LIBRARY_TAG).d("Billing flow UI is completed successfully.")
                handlePurchases(purchases)
            }

            BillingResponseCode.USER_CANCELED -> {
                Timber.tag(Constants.LIBRARY_TAG).d("Billing flow UI canceled by user.")
            }

            else -> throw BillingException(billingResult)
        }
    }

    private suspend fun handlePurchases(purchases: List<Purchase>) {
        if (purchases.isEmpty()) return

        purchases
            .filter { purchase ->
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchaseValidator.verifyPurchase(purchase)
            }
            .forEach { purchase ->
                // purchased purchase
                // purchase to acknowledge
                // purchase to consume
                updateProductsState(purchase)
                purchase.products.forEach { product ->
                    val isConsumable =
                        productIdProvider.getAutoConsumeProductIds().contains(product)
                    when {
                        isConsumable -> {
                            consumePurchase(purchase)
                        }

                        !purchase.isAcknowledged -> {
                            acknowledgePurchase(purchase)
                        }
                    }
                }
            }
    }

    private suspend fun updateProductsState(purchase: Purchase) =
        purchase.products.forEach { product ->
            productStateMap
                .getOrPut(product) {
                    MutableStateFlow(ProductState.UNPURCHASED)
                }
                .emit(mapProductState(purchase))
        }

    private fun mapProductState(purchase: Purchase) = when (purchase.purchaseState) {
        Purchase.PurchaseState.UNSPECIFIED_STATE -> ProductState.UNPURCHASED
        Purchase.PurchaseState.PENDING -> ProductState.PENDING
        Purchase.PurchaseState.PURCHASED -> if (purchase.isAcknowledged) {
            ProductState.PURCHASED_AND_ACKNOWLEDGED
        } else {
            ProductState.PURCHASED
        }

        else -> ProductState.UNPURCHASED
    }

    private suspend fun consumePurchase(purchase: Purchase) = connection.useBillingClient {
        if (purchaseConsumptionInProcess.contains(purchase)) return@useBillingClient

        purchaseConsumptionInProcess.add(purchase)
        val params = prepareConsumeParams(purchase.purchaseToken)
        Timber.tag(Constants.LIBRARY_TAG)
            .d("Consuming purchase(${purchase.orderId}): ${purchase.products}...")
        val (billingResult, _) = consumePurchase(params)

        purchaseConsumptionInProcess.remove(purchase)
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                Timber.tag(Constants.LIBRARY_TAG).d("Purchase(${purchase.orderId}) consumed.")
                updateProductsState(purchase.products, ProductState.UNPURCHASED)
                purchase.products.forEach { product ->
                    _onNewProductPurchased.emit(product)
                }
            }

            else -> throw BillingException(billingResult)
        }
    }

    private fun prepareConsumeParams(purchaseToken: String) = ConsumeParams.newBuilder()
        .setPurchaseToken(purchaseToken)
        .build()

    private suspend fun acknowledgePurchase(purchase: Purchase) = connection.useBillingClient {
        if (purchaseAcknowledgeInProcess.contains(purchase)) return@useBillingClient

        purchaseAcknowledgeInProcess.add(purchase)
        val params = prepareAcknowledgePurchaseParams(purchase.purchaseToken)
        Timber.tag(Constants.LIBRARY_TAG)
            .d("Acknowledging purchase(${purchase.orderId}): ${purchase.products}...")
        val billingResult = acknowledgePurchase(params)

        purchaseAcknowledgeInProcess.remove(purchase)
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                Timber.tag(Constants.LIBRARY_TAG).d("Purchase(${purchase.orderId}) acknowledged.")
                updateProductsState(purchase.products, ProductState.PURCHASED_AND_ACKNOWLEDGED)
                purchase.products.forEach { product ->
                    _onNewProductPurchased.emit(product)
                }
            }

            else -> throw BillingException(billingResult)
        }
    }

    private fun prepareAcknowledgePurchaseParams(purchaseToken: String) =
        AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

    private suspend fun updateProductsState(products: List<String>, value: ProductState) =
        products.forEach { product ->
            productStateMap
                .getOrPut(product) {
                    MutableStateFlow(value)
                }
                .emit(value)
        }

    override fun isPurchased(productId: String) =
        productStateMap
            .getOrPut(productId) {
                MutableStateFlow(ProductState.UNPURCHASED)
            }
            .map { state ->
                state == ProductState.PURCHASED_AND_ACKNOWLEDGED
            }

    override fun canPurchase(productId: String): Flow<Boolean> =
        productStateMap
            .getOrPut(productId) {
                MutableStateFlow(ProductState.UNPURCHASED)
            }
            .map { state ->
                state == ProductState.UNPURCHASED
            }

    private enum class ProductState { UNPURCHASED, PENDING, PURCHASED, PURCHASED_AND_ACKNOWLEDGED }

    private data class PurchaseResult(
        val billingResult: BillingResult,
        val purchases: List<Purchase>
    )
}