package com.example.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.billing.BillingRepository
import com.example.app.ui.views.produktlist.Item
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.stateIn

class MainFragmentViewModel(
    billingRepository: BillingRepository
) : ViewModel() {
    private val _items: Flow<List<Item>> = combineTransform(
        billingRepository.skuDetailsSnippetList,
        billingRepository.purchasesList
    ) { skuDetailsSnippetList, purchasesList ->
        val value = skuDetailsSnippetList.map {
            val element = it.sku
            val item = Item(element, it.price, !purchasesList.contains(element))
            item
        }
        emit(value)
    }
    val items: Flow<List<Item>> =
        _items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
}