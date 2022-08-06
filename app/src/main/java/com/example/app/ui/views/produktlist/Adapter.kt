package com.example.app.ui.views.produktlist

import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import com.example.app.R
import com.example.app.ui.common.recyclerviewadapter.RecyclerViewAdapter
import com.example.app.ui.common.recyclerviewadapter.RecyclerViewHolder
import com.example.app.utility.Values

class Adapter(
    private val values: Values,
    private val onItemClick: (item: Item) -> Unit
) : RecyclerViewAdapter<Item>() {

    override fun viewType(item: Item): Int {
        return R.layout.layout_product_list_item
    }

    override fun viewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerViewHolder<*, ViewDataBinding> {
        return ItemViewHolder(viewType, parent, values) {
            onItemClick(currentList[it])
        }
    }
}