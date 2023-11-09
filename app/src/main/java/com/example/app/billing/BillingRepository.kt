package com.example.app.billing

import com.mgsoftware.billing.ProductDetailsSnippet
import kotlinx.coroutines.flow.StateFlow

interface BillingRepository {
    val productDetailsSnippetList: StateFlow<Set<ProductDetailsSnippet>>
    val purchasesList: StateFlow<Set<String>>

    suspend fun updateProductDetailsSnippetList(value: Set<ProductDetailsSnippet>)

    suspend fun updatePurchasesList(value: Set<String>)
}