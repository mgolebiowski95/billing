package com.mgsoftware.billing.api.model

data class ProductInfo(
    val productDetails: ProductDetails,
    val isPurchased: Boolean,
    val canPurchase: Boolean,
)
