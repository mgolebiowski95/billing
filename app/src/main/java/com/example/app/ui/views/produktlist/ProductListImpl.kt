package com.example.app.ui.views.produktlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.databinding.LayoutProductListBinding
import com.example.app.ui.common.view.BaseObservableView
import com.example.app.utility.Values

class ProductListImpl(
    inflater: LayoutInflater,
    parent: ViewGroup?,
    values: Values,
) : BaseObservableView<ProductList.Listener>(), ProductList {
    private val binding = LayoutProductListBinding.inflate(inflater, parent, false)

    private val adapter: Adapter

    init {
        setRootView(binding.root)

        binding.recyclerView.layoutManager =
            LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false)

        adapter = Adapter(
            values,
            onItemClick = { item ->
                getListeners().forEach { it.onItemClick(item) }
            }
        )
        binding.recyclerView.adapter = adapter
    }

    override fun bind(items: List<Item>) {
        adapter.submitList(items)
    }

}