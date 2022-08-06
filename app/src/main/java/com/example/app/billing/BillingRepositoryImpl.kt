package com.example.app.billing

import com.mgsoftware.billing.SkuDetailsSnippet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BillingRepositoryImpl : BillingRepository {
    private val _skuDetailsSnippetList = MutableStateFlow<Set<SkuDetailsSnippet>>(emptySet())
    override val skuDetailsSnippetList: StateFlow<Set<SkuDetailsSnippet>>
        get() = _skuDetailsSnippetList
    private val _purchasesList = MutableStateFlow<Set<String>>(emptySet())
    override val purchasesList: StateFlow<Set<String>>
        get() = _purchasesList

    override suspend fun updateSkuDetailsSnippetList(value: Set<SkuDetailsSnippet>) {
        _skuDetailsSnippetList.emit(value)
    }

    override suspend fun updatePurchasesList(value: Set<String>) {
        _purchasesList.emit(value)
    }
}