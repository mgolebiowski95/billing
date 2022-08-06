package com.mgsoftware.billing

data class SkuDetailsSnippet(
    val sku: String,
    val type: String,
    val price: String,
    val title: String,
    var description: String,
)