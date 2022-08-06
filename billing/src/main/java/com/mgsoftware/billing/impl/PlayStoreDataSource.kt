package com.mgsoftware.billing.impl

import android.app.Activity
import android.app.Application
import android.util.Log
import com.android.billingclient.api.*
import com.mgsoftware.billing.BuildConfig
import com.mgsoftware.billing.PurchaseValidator
import com.mgsoftware.billing.SkuProvider
import com.mgsoftware.billing.common.BaseObservable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

class PlayStoreDataSource(
    application: Application,
    private val skuProvider: SkuProvider,
    private val purchaseValidator: PurchaseValidator
) : BaseObservable<PlayStoreDataSource.Listener>(), BillingClientStateListener {

    interface Listener {

        fun onBillingSetupSuccess()

        fun onBillingSetupFailed(errorCode: Int, errorMessage: String)

        fun disburseNonConsumableEntitlements(
            purchases: Set<Purchase>,
            callFromOnPurchasesUpdated: Boolean
        )

        fun onPurchaseAcknowledged(sku: String)

        fun onPurchaseConsumed(sku: String, quantity: Int)
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
        if (BuildConfig.DEBUG)
            Log.d(TAG, "onBillingServiceDisconnected")
        openConnection()
    }

    fun openConnection() {
        if (!billingClient.isReady)
            billingClient.startConnection(this)
        if (BuildConfig.DEBUG)
            Log.d(TAG, "openConnection")
    }

    fun closeConnection() {
        billingClient.endConnection()
        if (BuildConfig.DEBUG)
            Log.d(TAG, "closeConnection")
    }

    fun isReady() = billingClient.isReady

    suspend fun querySkuDetails(
        sku: String,
        @BillingClient.SkuType skuType: String
    ): SkuDetailsResult {
        val skuList = setOf(sku)
        if (BuildConfig.DEBUG)
            Log.d(TAG, "querySkuDetails: skuList=$skuList")
        return querySkuDetails(skuList, skuType)
    }

    suspend fun querySkuDetails(
        skuList: Set<String>,
        @BillingClient.SkuType skuType: String
    ): SkuDetailsResult {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "querySkuDetails: skuList=$skuList")
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList.toList())
        params.setType(skuType)
        return withContext(Dispatchers.IO) {
            billingClient.querySkuDetails(params.build())
        }
    }

    /**
     * callbacks:
     * disburseConsumableEntitlements
     * disburseNonConsumableEntitlements
     */
    suspend fun queryPurchasesAsync() = withContext(Dispatchers.IO) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "queryPurchasesAsync")
        val purchasesList = mutableSetOf<Purchase>()
        val inappResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
        purchasesList.addAll(inappResult.purchasesList.orEmpty())
        if (isSubscriptionSupported()) {
            val subsResult = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
            purchasesList.addAll(subsResult.purchasesList.orEmpty())
        }
        if (purchasesList.isNotEmpty()) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Purchases to be processed: $purchasesList")
            processPurchases(purchasesList, false)
        } else {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "There are no purchases.")
            getListeners().forEach { it.disburseNonConsumableEntitlements(emptySet(), false) }
        }
    }

    suspend fun launchBillingFlow(activity: Activity, skuDetails: SkuDetails) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "launchBillingFlow: ${skuDetails.sku}")
        val params = BillingFlowParams.newBuilder()
        params.setSkuDetails(skuDetails)
        billingClient.launchBillingFlow(activity, params.build())
        val result = purchaseChannel.receive()

        if (BuildConfig.DEBUG)
            Log.d(TAG, "onPurchasesUpdated")
        when (result.billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (result.purchases.isEmpty())
                    Log.d(TAG, "Empty Purchase List Returned from OK response!")
                else
                    processPurchases(result.purchases.toSet(), true)
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                Log.i(TAG, "onPurchasesUpdated: User canceled the purchase")
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.w(
                    TAG,
                    "item already owned? call queryPurchases to verify and process all such items"
                )
                queryPurchasesAsync()
            }
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> Log.e(
                TAG,
                "onPurchasesUpdated: Developer error means that Google Play " +
                        "does not recognize the configuration. If you are just getting started, " +
                        "make sure you have configured the application correctly in the " +
                        "Google Play Console. The SKU product ID must match and the APK you " +
                        "are using must be signed with release keys."
            )
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                openConnection()
            }
            else -> {
                Log.w(TAG, result.billingResult.debugMessage)
            }
        }
    }

    private suspend fun processPurchases(
        purchases: Set<Purchase>,
        callFromOnPurchasesUpdated: Boolean
    ) {
        if (BuildConfig.DEBUG)
            Log.d(
                TAG,
                "processPurchases: ${
                    purchases.joinToString(
                        separator = ", ",
                        transform = { it.skus[0] })
                }"
            )
        val validPurchases = purchases.filter { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED)
                isSignatureValid(purchase)
            else
                false
        }

        val (consumables, nonConsumables) = validPurchases.partition {
            val consumableSkus = skuProvider.getConsumableSkus()
            consumableSkus.contains(it.skus[0])
        }
        if (consumables.isNotEmpty()) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Number of purchases to be consumed: ${consumables.size}")
            processConsumablePurchases(consumables)
        }
        if (nonConsumables.isNotEmpty()) {
            val purchasesToBeAcknowledge = nonConsumables.filter { !it.isAcknowledged }
            Log.d(TAG, "Number of purchases to be acknowledge: ${purchasesToBeAcknowledge.size}")
            processNonConsumablePurchases(nonConsumables, callFromOnPurchasesUpdated)
        }
    }

    private suspend fun processConsumablePurchases(purchases: List<Purchase>) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "processConsumablePurchases")
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
        if (BuildConfig.DEBUG)
            Log.d(TAG, "consumePurchase: ${purchase.skus[0]}")
        val (billingResult, purchaseToken) = billingClient.consumePurchase(params.build())
        purchaseConsumptionInProcess.remove(purchase)

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "consumeResult: success")
                getListeners().forEach {
                    it.onPurchaseConsumed(
                        purchase.skus[0],
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
        if (BuildConfig.DEBUG)
            Log.d(TAG, "processNonConsumablePurchases")
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
        if (BuildConfig.DEBUG)
            Log.d(TAG, "acknowledgePurchase: ${purchase.skus[0]}")
        val billingResult = billingClient.acknowledgePurchase(params.build())
        return when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.d(TAG, "acknowledgeResult success")
                getListeners().forEach { it.onPurchaseAcknowledged(purchase.skus[0]) }
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