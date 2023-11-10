package com.mgsoftware.billing.impl

import android.app.Activity
import android.app.Application
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.ProductType
import com.mgsoftware.billing.PurchaseValidator
import com.mgsoftware.billing.ProductIdProvider
import com.mgsoftware.billing.common.BaseObservable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import timber.log.Timber

class PlayStoreDataSource(
    application: Application,
    private val productIdProvider: ProductIdProvider,
    private val purchaseValidator: PurchaseValidator
) : BaseObservable<PlayStoreDataSource.Listener>(), BillingClientStateListener {

    interface Listener {

        fun onBillingSetupSuccess()

        fun onBillingSetupFailed(errorCode: Int, errorMessage: String)

        fun disburseNonConsumableEntitlements(
            purchases: Set<Purchase>,
            callFromOnPurchasesUpdated: Boolean
        )

        fun onPurchaseAcknowledged(productId: String)

        fun onPurchaseConsumed(productId: String, quantity: Int)
    }

    private var billingClient: BillingClient
    private val purchaseChannel = Channel<PurchaseResult>(Channel.UNLIMITED)

    private val purchaseConsumptionInProcess: MutableSet<Purchase> = HashSet()

    init {
        val bcb = BillingClient.newBuilder(application.applicationContext)
        billingClient = bcb
            .enablePendingPurchases()
            .setListener { billingResult, purchases ->
                purchaseChannel.trySend(PurchaseResult(billingResult, purchases.orEmpty()))
            }
            .build()

    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK)
            getListeners().forEach { it.onBillingSetupSuccess() }
        else
            getListeners().forEach {
                it.onBillingSetupFailed(
                    billingResult.responseCode,
                    billingResult.debugMessage
                )
            }
    }

    override fun onBillingServiceDisconnected() {
        Timber.d("onBillingServiceDisconnected")
        openConnection()
    }

    fun openConnection() {
        if (!billingClient.isReady) {
            Timber.d("openConnection")
            billingClient.startConnection(this)
        }
    }

    fun closeConnection() {
        if (billingClient.isReady) {
            Timber.d("closeConnection")
            billingClient.endConnection()
        }
    }

    fun isReady() = billingClient.isReady

    suspend fun queryProductDetails(
        productId: String,
        @ProductType productType: String
    ): ProductDetailsResult {
        val productIds = setOf(productId)
        return queryProductDetails(productIds, productType)
    }

    suspend fun queryProductDetails(
        productIds: Set<String>,
        @ProductType productType: String
    ): ProductDetailsResult {
        Timber.d("queryProductDetails: productIds=$productIds")
        return withContext(Dispatchers.IO) {
            val params = prepareQueryProductDetailsParams(
                productIds,
                productType
            )
            billingClient.queryProductDetails(params)
        }
    }

    private fun prepareQueryProductDetailsParams(
        productIds: Set<String>,
        @ProductType productType: String
    ) = QueryProductDetailsParams
        .newBuilder()
        .setProductList(
            productIds.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(productType)
                    .build()
            }
        )
        .build()

    /**
     * callbacks:
     * disburseConsumableEntitlements
     * disburseNonConsumableEntitlements
     */
    suspend fun queryPurchasesAsync() = withContext(Dispatchers.IO) {
        Timber.d("queryPurchasesAsync")
        val purchases = mutableSetOf<Purchase>()

        val params = prepareQueryPurchaseParams(ProductType.INAPP)
        val inappResult = billingClient.queryPurchasesAsync(params)
        Timber.d("queryPurchasesAsync (INAPP): ${inappResult.billingResult}")
        purchases.addAll(inappResult.purchasesList)

        if (isSubscriptionSupported()) {
            val params = prepareQueryPurchaseParams(ProductType.SUBS)
            val subsResult = billingClient.queryPurchasesAsync(params)
            Timber.d("queryPurchasesAsync (SUBS): ${subsResult.billingResult}")
            purchases.addAll(subsResult.purchasesList)
        }

        if (purchases.isNotEmpty()) {
            Timber.d("Purchases to be processed: $purchases")
            processPurchases(
                purchases = purchases,
                callFromOnPurchasesUpdated = false
            )
        } else {
            Timber.d("There are no purchases to be process.")
            getListeners().forEach { it.disburseNonConsumableEntitlements(emptySet(), false) }
        }
    }

    private fun prepareQueryPurchaseParams(@ProductType productType: String) = QueryPurchasesParams
        .newBuilder()
        .setProductType(productType)
        .build()

    private fun isSubscriptionSupported(): Boolean {
        val billingResult =
            billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        Timber.d("isSubscriptionSupported: $billingResult")
        return when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                true
            }

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                openConnection()
                false
            }

            else -> {
                false
            }
        }
    }

    suspend fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails
    ) {
        Timber.d("launchBillingFlow: " + productDetails.productId)

        val params = prepareBillingFlowParams(productDetails)
        billingClient.launchBillingFlow(activity, params)
        val result = purchaseChannel.receive()
        Timber.d("onPurchasesUpdated: ${result.billingResult}")

        when (result.billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (result.purchases.isEmpty()) {
                    Timber.d("Empty Purchase List Returned from OK response!")
                } else {
                    processPurchases(
                        purchases = result.purchases.toSet(),
                        callFromOnPurchasesUpdated = true
                    )
                }
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                queryPurchasesAsync()
            }

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                openConnection()
            }
        }
    }

    private fun prepareBillingFlowParams(productDetails: ProductDetails) =
        BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(prepareProductDetailsParams(productDetails)))
            .build()

    private fun prepareProductDetailsParams(productDetails: ProductDetails): BillingFlowParams.ProductDetailsParams {
        val builder = BillingFlowParams.ProductDetailsParams.newBuilder()
        builder.setProductDetails(productDetails)

        val subscriptionOfferDetails = productDetails.subscriptionOfferDetails
        if (subscriptionOfferDetails != null) {
            builder.setOfferToken(subscriptionOfferDetails.first().offerToken)
        }

        return builder.build()
    }

    private suspend fun processPurchases(
        purchases: Set<Purchase>,
        callFromOnPurchasesUpdated: Boolean
    ) {
        Timber.d(
            "processPurchases: ${
                purchases.joinToString(
                    separator = ", ",
                    transform = { it.defaultProduct() }
                )
            }"
        )

        val validPurchases = purchases.filter { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED)
                isSignatureValid(purchase)
            else
                false
        }

        val (consumables, nonConsumables) = validPurchases.partition { purchase ->
            val consumableProductIds = productIdProvider.getConsumableProductIds()
            consumableProductIds.contains(purchase.defaultProduct())
        }
        if (consumables.isNotEmpty()) {
            Timber.d("Number of purchases to be consumed: " + consumables.size)
            processConsumablePurchases(consumables)
        }
        if (nonConsumables.isNotEmpty()) {
            val purchasesToBeAcknowledge = nonConsumables.filter { !it.isAcknowledged }
            Timber.d("Number of purchases to be acknowledge: " + purchasesToBeAcknowledge.size)
            processNonConsumablePurchases(nonConsumables, callFromOnPurchasesUpdated)
        }
    }

    private fun isSignatureValid(purchase: Purchase): Boolean {
        return purchaseValidator.verifyPurchase(purchase)
    }

    private suspend fun processConsumablePurchases(purchases: List<Purchase>) {
        Timber.d("processConsumablePurchases")
        purchases.forEach { purchase ->
            consumePurchase(purchase)
        }
    }

    private suspend fun consumePurchase(purchase: Purchase) {
        if (purchaseConsumptionInProcess.contains(purchase)) {
            return
        }
        purchaseConsumptionInProcess.add(purchase)
        Timber.d("consumePurchase: ${purchase.defaultProduct()}")

        val params = prepareConsumeParams(purchase.purchaseToken)
        val (billingResult, _) = withContext(Dispatchers.IO) {
            billingClient.consumePurchase(params)
        }
        Timber.d("consumePurchase (${purchase.defaultProduct()}): $billingResult")
        purchaseConsumptionInProcess.remove(purchase)

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                getListeners().forEach {
                    it.onPurchaseConsumed(
                        purchase.defaultProduct(),
                        purchase.quantity
                    )
                }
            }
        }
    }

    private fun prepareConsumeParams(purchaseToken: String) = ConsumeParams.newBuilder()
        .setPurchaseToken(purchaseToken)
        .build()

    private suspend fun processNonConsumablePurchases(
        purchases: List<Purchase>,
        callFromOnPurchasesUpdated: Boolean
    ) {
        Timber.d("processNonConsumablePurchases")
        val acknowledgedPurchases = purchases.filterTo(mutableSetOf()) { purchase ->
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            } else {
                true
            }
        }
        getListeners().forEach {
            it.disburseNonConsumableEntitlements(
                acknowledgedPurchases,
                callFromOnPurchasesUpdated
            )
        }
    }

    private suspend fun acknowledgePurchase(purchase: Purchase): Boolean {
        Timber.d("acknowledgePurchase: ${purchase.defaultProduct()}")

        val params = prepareAcknowledgePurchaseParams(purchase.purchaseToken)
        val billingResult = withContext(Dispatchers.IO) {
            billingClient.acknowledgePurchase(params)
        }
        Timber.d("acknowledgePurchase: (${purchase.defaultProduct()}): $billingResult")
        return when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                getListeners().forEach { it.onPurchaseAcknowledged(purchase.defaultProduct()) }
                true
            }

            else -> {
                false
            }
        }
    }

    private fun prepareAcknowledgePurchaseParams(purchaseToken: String) =
        AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()


    /**
     * return first product of purchase. Library does not handle multi product purchases.
     */
    private fun Purchase.defaultProduct() = products.first()

    data class PurchaseResult(val billingResult: BillingResult, val purchases: List<Purchase>)

    companion object {
        private const val DEFAULT_INDEX = 0
    }
}