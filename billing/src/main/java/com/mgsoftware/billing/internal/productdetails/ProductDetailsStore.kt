package com.mgsoftware.billing.internal.productdetails

import com.mgsoftware.billing.api.model.ProductDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

internal class ProductDetailsStore {
    private val data =
        mutableMapOf<String, MutableStateFlow<ProductDetails?>>()

    fun get(productId: String): StateFlow<ProductDetails?> =
        data.getOrPut(
            productId
        ) { MutableStateFlow(null) }

    suspend fun update(productId: String, newValue: ProductDetails?) =
        data
            .getOrPut(
                productId
            ) { MutableStateFlow(null) }
            .update { newValue }
}