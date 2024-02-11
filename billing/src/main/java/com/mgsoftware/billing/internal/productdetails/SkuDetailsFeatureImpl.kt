package com.mgsoftware.billing.internal.productdetails

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.mgsoftware.billing.api.ProductIdProvider
import com.mgsoftware.billing.api.model.Constants
import com.mgsoftware.billing.api.model.FeatureType
import com.mgsoftware.billing.api.model.ProductDetails
import com.mgsoftware.billing.internal.connection.BillingConnection
import com.mgsoftware.billing.utils.getSkuDetails
import com.mgsoftware.billing.utils.isFeatureSupported
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

internal class SkuDetailsFeatureImpl(
    private val connection: BillingConnection,
    private val productIdProvider: ProductIdProvider,
    private val productDetailsStore: ProductDetailsStore,
) : ProductDetailsFeature {
    private val sourceMap = mutableMapOf<String, SkuDetails>()

    override suspend fun fetchProductDetails(): Unit = connection.useBillingClient {
        coroutineScope {
            val inappProductIds = productIdProvider.getKnownInappProductIds()
            if (inappProductIds.isEmpty()) {
                Timber
                    .tag(Constants.LIBRARY_TAG)
                    .w("Can't fetch '${SkuType.INAPP}' product details. No product identifiers provided.")
            } else {
                launch {
                    internalFetchProductDetails(
                        inappProductIds,
                        SkuType.INAPP
                    )
                }
            }

            val subscriptionProductIds = productIdProvider.getKnownSubscriptionProductIds()
            if (subscriptionProductIds.isEmpty()) {
                Timber
                    .tag(Constants.LIBRARY_TAG)
                    .w("Can't fetch '${SkuType.SUBS}' product details. No product identifiers provided.")
            } else {
                if (isFeatureSupported(FeatureType.SUBSCRIPTIONS)) {
                    launch {
                        internalFetchProductDetails(
                            productIdProvider.getKnownSubscriptionProductIds(),
                            SkuType.SUBS
                        )
                    }
                }
            }
        }
    }

    private suspend fun BillingClient.internalFetchProductDetails(
        skus: Set<String>,
        @SkuType type: String
    ) {
        Timber.tag(Constants.LIBRARY_TAG).d("Fetching '$type' $skus products details...")
        val params = prepareSkuDetailsParams(skus, type)
        val skuDetailsList = getSkuDetails(params)
        Timber.tag(Constants.LIBRARY_TAG).d("'$type' product details fetched: $skuDetailsList")
        skuDetailsList?.forEach {
            val productId = it.sku
            sourceMap[productId] = it
            val domainProductDetails = it.toProductDetails()
            productDetailsStore.update(productId, domainProductDetails)
        }
    }

    override fun productDetails(productId: String): StateFlow<ProductDetails?> =
        productDetailsStore.get(productId)

    private fun SkuDetails.toProductDetails(): ProductDetails =
        ProductDetails(
            productId = sku,
            title = title,
            description = description,
            price = price
        )

    override fun prepareBillingFlowParams(key: String): BillingFlowParams =
        BillingFlowParams.newBuilder()
            .setSkuDetails(sourceMap.getValue(key))
            .build()

    private fun prepareSkuDetailsParams(
        skus: Set<String>,
        @SkuType type: String
    ): SkuDetailsParams = SkuDetailsParams.newBuilder()
        .setSkusList(skus.toList())
        .setType(type)
        .build()
}