package com.example.app.billing

import com.mgsoftware.billing.ProductDetailsSnippet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BillingRepositoryImpl : BillingRepository {
    private val _productDetailsSnippetList = MutableStateFlow<Set<ProductDetailsSnippet>>(emptySet())
    override val productDetailsSnippetList: StateFlow<Set<ProductDetailsSnippet>>
        get() = _productDetailsSnippetList
    private val _purchasesList = MutableStateFlow<Set<String>>(emptySet())
    override val purchasesList: StateFlow<Set<String>>
        get() = _purchasesList

    override suspend fun updateProductDetailsSnippetList(value: Set<ProductDetailsSnippet>) {
        _productDetailsSnippetList.emit(value)
    }

    override suspend fun updatePurchasesList(value: Set<String>) {
        _purchasesList.emit(value)
    }
}