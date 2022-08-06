package com.example.app.billing

import com.mgsoftware.billing.SkuDetailsSnippet
import kotlinx.coroutines.flow.StateFlow

interface BillingRepository {
    val skuDetailsSnippetList: StateFlow<Set<SkuDetailsSnippet>>
    val purchasesList: StateFlow<Set<String>>

    suspend fun updateSkuDetailsSnippetList(value: Set<SkuDetailsSnippet>)

    suspend fun updatePurchasesList(value: Set<String>)
}