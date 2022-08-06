package com.example.app.ui.views.produktlist

import com.example.app.ui.common.view.IObservableView

interface ProductList : IObservableView<ProductList.Listener> {

    interface Listener {

        fun onItemClick(item: Item)
    }

    fun bind(items: List<Item>)
}