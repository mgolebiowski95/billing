package com.mgsoftware.billing.api.model

data class ProductDetails(
    val productId: String,
    val title: String,
    val description: String,
    val price: String
)