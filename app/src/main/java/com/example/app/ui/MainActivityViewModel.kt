package com.example.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.billing.AppProductIdProvider
import com.example.app.ui.views.produktlist.Item
import com.example.app.utility.Values
import com.mgsoftware.billing.api.BillingManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class MainActivityViewModel(
    private val billingManager: BillingManager,
    private val values: Values
) : ViewModel() {
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> get() = _items

    init {
        viewModelScope.launch {
            combine(
                billingManager.getProductInfo(AppProductIdProvider.GOLD_MONTHLY),
                billingManager.getProductInfo(AppProductIdProvider.GAS),
                billingManager.getProductInfo(AppProductIdProvider.PREMIUM_CAR),
            ) { a, b, c ->
                listOf(a, b, c)
            }
                .distinctUntilChanged()
                .debounce(1000L)
                .map {
                    it.map {
                        Item(
                            it.productDetails.productId,
                            it.productDetails.price,
                            it.isPurchased,
                            it.canPurchase
                        )
                    }
                }
                .collectLatest {
                    _items.emit(it)
                }
        }
    }
}