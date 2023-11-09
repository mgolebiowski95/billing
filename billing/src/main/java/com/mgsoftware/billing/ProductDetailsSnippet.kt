package com.mgsoftware.billing

data class ProductDetailsSnippet(
    val id: String,
    val type: String,
    val price: String,
    val title: String,
    var description: String,
)