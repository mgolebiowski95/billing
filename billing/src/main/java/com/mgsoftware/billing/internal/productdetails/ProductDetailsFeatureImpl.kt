package com.mgsoftware.billing.internal.productdetails

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.mgsoftware.billing.api.ProductIdProvider
import com.mgsoftware.billing.api.model.Constants
import com.mgsoftware.billing.api.model.FeatureType
import com.mgsoftware.billing.internal.connection.BillingConnection
import com.mgsoftware.billing.utils.getProductDetails
import com.mgsoftware.billing.utils.isFeatureSupported
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

internal class ProductDetailsFeatureImpl(
    private val connection: BillingConnection,
    private val productIdProvider: ProductIdProvider,
    private val productDetailsStore: ProductDetailsStore,
) : ProductDetailsFeature {
    private val sourceMap = mutableMapOf<String, ProductDetails>()

    override suspend fun fetchProductDetails(): Unit = connection.useBillingClient {
        coroutineScope {
            val inappProductIds = productIdProvider.getKnownInappProductIds()
            if (inappProductIds.isEmpty()) {
                Timber
                    .tag(Constants.LIBRARY_TAG)
                    .w("Can't fetch '${ProductType.INAPP}' product details. No product identifiers provided.")
            } else {
                launch {
                    internalFetchProductDetails(
                        inappProductIds,
                        ProductType.INAPP
                    )
                }
            }

            val subscriptionProductIds = productIdProvider.getKnownSubscriptionProductIds()
            if (subscriptionProductIds.isEmpty()) {
                Timber
                    .tag(Constants.LIBRARY_TAG)
                    .w("Can't fetch '${ProductType.SUBS}' product details. No product identifiers provided.")
            } else {
                if (isFeatureSupported(FeatureType.SUBSCRIPTIONS)) {
                    launch {
                        internalFetchProductDetails(
                            subscriptionProductIds,
                            ProductType.SUBS
                        )
                    }
                }
            }
        }
    }

    private suspend fun BillingClient.internalFetchProductDetails(
        productIds: Set<String>,
        @ProductType type: String
    ) {
        Timber.tag(Constants.LIBRARY_TAG).d("Fetching '$type' $productIds product details...")
        val params = prepareQueryProductDetailsParams(productIds, type)
        val productDetailsList = this.getProductDetails(params)
        Timber
            .tag(Constants.LIBRARY_TAG)
            .d(
                "'$type' product details fetched: ${
                    productDetailsList?.joinToString(
                        separator = ", ",
                        transform = { it.productId }
                    )
                }"
            )
        productDetailsList?.forEach {
            val productId = it.productId
            sourceMap[productId] = it
            val domainProductDetails = it.toProductDetails()
            productDetailsStore.update(productId, domainProductDetails)
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

    override fun productDetails(productId: String): StateFlow<com.mgsoftware.billing.api.model.ProductDetails?> =
        productDetailsStore.get(productId)

    private fun ProductDetails.toProductDetails(): com.mgsoftware.billing.api.model.ProductDetails =
        com.mgsoftware.billing.api.model.ProductDetails(
            productId = productId,
            title = title,
            description = description,
            price = requireNotNull(
                oneTimePurchaseOfferDetails?.formattedPrice
                    ?: subscriptionOfferDetails
                        ?.firstOrNull()
                        ?.pricingPhases
                        ?.pricingPhaseList
                        ?.firstOrNull()
                        ?.formattedPrice
            )
        )

    override fun prepareBillingFlowParams(key: String): BillingFlowParams =
        BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    prepareProductDetailsParams(
                        sourceMap.getValue(key)
                    )
                )
            )
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
}