package com.mgsoftware.billing.impl

import android.app.Activity
import android.app.Application
import android.util.Log
import com.android.billingclient.api.*
import com.mgsoftware.billing.BuildConfig
import com.mgsoftware.billing.PurchaseValidator
import com.mgsoftware.billing.ProductIdProvider
import com.mgsoftware.billing.common.BaseObservable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

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
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onBillingServiceDisconnected")
        }

        openConnection()
    }

    fun openConnection() {
        if (!billingClient.isReady)
            billingClient.startConnection(this)

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "openConnection")
        }
    }

    fun closeConnection() {
        billingClient.endConnection()

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "closeConnection")
        }
    }

    fun isReady() = billingClient.isReady

    suspend fun queryProductDetails(
        productId: String,
        @BillingClient.ProductType productType: String
    ): ProductDetailsResult {
        val productIds = setOf(productId)
        return queryProductDetails(productIds, productType)
    }

    suspend fun queryProductDetails(
        productIds: Set<String>,
        @BillingClient.ProductType productType: String
    ): ProductDetailsResult {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "queryProductDetails: productIds=$productIds")
        }

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
        @BillingClient.ProductType productType: String
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
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "queryPurchasesAsync")
        }

        val purchasesList = mutableSetOf<Purchase>()
        val params = QueryPurchasesParams.newBuilder()
        params.setProductType(BillingClient.ProductType.INAPP)
        val inappResult = billingClient.queryPurchasesAsync(params.build())

        purchasesList.addAll(inappResult.purchasesList)
        if (isSubscriptionSupported()) {
            val subsResult = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            purchasesList.addAll(subsResult.purchasesList)
        }
        if (purchasesList.isNotEmpty()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Purchases to be processed: $purchasesList")
            }

            processPurchases(purchasesList, false)
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "There are no purchases.")
            }

            getListeners().forEach { it.disburseNonConsumableEntitlements(emptySet(), false) }
        }
    }

    suspend fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "launchBillingFlow: ${productDetails.productId}")
        }

        val params = BillingFlowParams.newBuilder()
        val productDetailsParamsList =
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            )
        params.setProductDetailsParamsList(productDetailsParamsList)
        billingClient.launchBillingFlow(activity, params.build())
        val result = purchaseChannel.receive()

        val responseCode = result.billingResult.responseCode
        if (BuildConfig.DEBUG) {
            val message =
                billingResponseCodeMessage(responseCode) ?: result.billingResult.debugMessage
            Log.d(TAG, "onPurchasesUpdated: $message")
        }

        when (responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (result.purchases.isEmpty()) {
                    Log.d(TAG, "Empty Purchase List Returned from OK response!")
                } else {
                    processPurchases(result.purchases.toSet(), true)
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

    /**
     * https://developer.android.com/reference/com/android/billingclient/api/BillingClient.BillingResponseCode
     */
    private fun billingResponseCodeMessage(@BillingClient.BillingResponseCode responseCode: Int): String? {
        return when (responseCode) {
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                """
                    A user billing error occurred during processing.

                    Examples where this error may occur:

                    The Play Store app on the user's device is out of date.
                    The user is in an unsupported country.
                    The user is an enterprise user and their enterprise admin has disabled users from making purchases.
                    Google Play is unable to charge the user’s payment method.
                    Letting the user retry may succeed if the condition causing the error has changed (e.g. An enterprise user's admin has allowed purchases for the organization).
                """.trimIndent()
            }

            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                """
                    Error resulting from incorrect usage of the API.

                    Examples where this error may occur:

                    Invalid arguments such as providing an empty product list where required.
                    Misconfiguration of the app such as not signing the app or not having the necessary permissions in the manifest.
                """.trimIndent()
            }

            BillingClient.BillingResponseCode.ERROR -> {
                """
                    Fatal error during the API action.

                    This is an internal Google Play error that may be transient or due to an unexpected condition during processing. You can automatically retry (e.g. with exponential back off) for this case and contact Google Play if issues persist. Be mindful of how long you retry if the retry is happening during a user interaction.
                """.trimIndent()
            }

            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                """
                    The requested feature is not supported by the Play Store on the current device.

                    If your app would like to check if a feature is supported before trying to use the feature your app can call BillingClient.isFeatureSupported(String) to check if a feature is supported. For a list of feature types that can be supported, see BillingClient.FeatureType.

                    For example: Before calling BillingClient.showInAppMessages(Activity, InAppMessageParams, InAppMessageResponseListener) API, you can call BillingClient.isFeatureSupported(String) with the BillingClient.FeatureType.IN_APP_MESSAGING featureType to check if it is supported.
                """.trimIndent()
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                """
                    The purchase failed because the item is already owned.

                    Make sure your app is up-to-date with recent purchases using guidance in the Fetching purchases section in the integration guide. If this error occurs despite making the check for recent purchases, then it may be due to stale purchase information that was cached on the device by Play. When you receive this error, the cache should get updated. After this, your purchases should be reconciled, and you can process them as outlined in the processing purchases section in the integration guide.
                """.trimIndent()
            }

            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                """
                    Requested action on the item failed since it is not owned by the user.

                    Make sure your app is up-to-date with recent purchases using guidance in the Fetching purchases section in the integration guide. If this error occurs despite making the check for recent purchases, then it may be due to stale purchase information cached on the device by Play. When you receive this error, the cache should get updated. After this, your purchases should be reconciled, and you can process the purchases accordingly. For example, if you are trying to consume an item and if the updated purchase information says it is already consumed, you can ignore the error now.
                """.trimIndent()
            }

            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> {
                """
                    The requested product is not available for purchase.

                    Please ensure the product is available in the user’s country. If you recently changed the country availability and are still receiving this error then it may be because of a propagation delay.
                """.trimIndent()
            }

            BillingClient.BillingResponseCode.NETWORK_ERROR -> {
                """
                    A network error occurred during the operation.

                    This error indicates that there was a problem with the network connection between the device and Play systems. This could potentially also be due to the user not having an active network connection.
                """.trimIndent()
            }

            BillingClient.BillingResponseCode.OK -> {
                """
                    Success.
                """.trimIndent()
            }

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                """
                    The app is not connected to the Play Store service via the Google Play Billing Library.

                    Examples where this error may occur:

                    The Play Store could have been updated in the background while your app was still running and the library lost connection.
                    BillingClient.startConnection(BillingClientStateListener) was never called or has not completed yet.
                    Since this state is transient, your app should automatically retry (e.g. with exponential back off) to recover from this error. Be mindful of how long you retry if the retry is happening during a user interaction. The retry should lead to a call to BillingClient.startConnection(BillingClientStateListener) right after or in some time after you received this code.
                """.trimIndent()
            }

            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                """
                    The service is currently unavailable.

                    Since this state is transient, your app should automatically retry (e.g. with exponential back off) to recover from this error. Be mindful of how long you retry if the retry is happening during a user interaction.
                """.trimIndent()
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                """
                    Transaction was canceled by the user.
                """.trimIndent()
            }

            else -> null
        }
    }

    private suspend fun processPurchases(
        purchases: Set<Purchase>,
        callFromOnPurchasesUpdated: Boolean
    ) {
        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "processPurchases: ${
                    purchases.joinToString(
                        separator = ", ",
                        transform = { it.products[0] })
                }"
            )
        }

        val validPurchases = purchases.filter { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED)
                isSignatureValid(purchase)
            else
                false
        }

        val (consumables, nonConsumables) = validPurchases.partition {
            val consumableProductIds = productIdProvider.getConsumableProductIds()
            consumableProductIds.contains(it.products[0])
        }
        if (consumables.isNotEmpty()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Number of purchases to be consumed: ${consumables.size}")
            }

            processConsumablePurchases(consumables)
        }
        if (nonConsumables.isNotEmpty()) {
            val purchasesToBeAcknowledge = nonConsumables.filter { !it.isAcknowledged }
            Log.d(TAG, "Number of purchases to be acknowledge: ${purchasesToBeAcknowledge.size}")
            processNonConsumablePurchases(nonConsumables, callFromOnPurchasesUpdated)
        }
    }

    private suspend fun processConsumablePurchases(purchases: List<Purchase>) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "processConsumablePurchases")
        }

        purchases.forEach { purchase ->
            consumePurchase(purchase)
        }
    }

    private suspend fun consumePurchase(purchase: Purchase) {
        if (purchaseConsumptionInProcess.contains(purchase))
            return

        purchaseConsumptionInProcess.add(purchase)
        val params = ConsumeParams.newBuilder()
        params.setPurchaseToken(purchase.purchaseToken)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "consumePurchase: ${purchase.products[0]}")
        }

        val (billingResult, purchaseToken) = withContext(Dispatchers.IO) {
            billingClient.consumePurchase(params.build())
        }
        purchaseConsumptionInProcess.remove(purchase)

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "consumeResult: success")
                }

                getListeners().forEach {
                    it.onPurchaseConsumed(
                        purchase.products[0],
                        purchase.quantity
                    )
                }
            }

            else -> {
                if (BuildConfig.DEBUG)
                    Log.d(
                        TAG,
                        "consumeResult: error: responseCode=${billingResult.responseCode} => debugMessage=${billingResult.debugMessage}"
                    )

                Log.w(TAG, billingResult.debugMessage)
            }
        }
    }

    private suspend fun processNonConsumablePurchases(
        purchases: List<Purchase>,
        callFromOnPurchasesUpdated: Boolean
    ) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "processNonConsumablePurchases")
        }

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
        val params = AcknowledgePurchaseParams.newBuilder()
        params.setPurchaseToken(purchase.purchaseToken)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "acknowledgePurchase: ${purchase.products[0]}")
        }

        val billingResult = billingClient.acknowledgePurchase(params.build())
        return when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.d(TAG, "acknowledgeResult success")
                getListeners().forEach { it.onPurchaseAcknowledged(purchase.products[0]) }
                true
            }

            else -> {
                Log.w(
                    TAG,
                    "acknowledgeResult error: responseCode=${billingResult.responseCode} => debugMessage=${billingResult.debugMessage}"
                )
                false
            }
        }
    }

    private fun isSignatureValid(purchase: Purchase): Boolean {
        return purchaseValidator.verifyPurchase(purchase)
    }

    private fun isSubscriptionSupported(): Boolean {
        val billingResult =
            billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        return when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                true
            }

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                openConnection()
                false
            }

            else -> {
                Log.w(TAG, "isSubscriptionSupported() error: ${billingResult.debugMessage}")
                false
            }
        }
    }

    data class PurchaseResult(val billingResult: BillingResult, val purchases: List<Purchase>)

    companion object {
        private val TAG = PlayStoreDataSource::class.java.simpleName
    }
}