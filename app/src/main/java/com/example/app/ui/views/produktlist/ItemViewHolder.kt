package com.example.app.ui.views.produktlist

import android.view.ViewGroup
import com.example.app.R
import com.example.app.databinding.LayoutProductListItemBinding
import com.example.app.ui.common.recyclerviewadapter.RecyclerViewHolder
import com.example.app.utility.Values

class ItemViewHolder(
    layoutId: Int,
    parent: ViewGroup,
    private val values: Values,
    onClick: (position: Int) -> Unit
) : RecyclerViewHolder<Item, LayoutProductListItemBinding>(layoutId, parent) {

    init {
        binding.root.setOnClickListener {
            onClick(adapterPosition)
        }
    }

    override fun bind(item: Item) {
        val sb = StringBuilder()
        sb.appendLine("sku=${item.sku}")
        binding.textView.text = sb.toString()

        if (item.canBePurchased)
            binding.cardView.setCardBackgroundColor(values.getColor(R.color.green_500))
        else
            binding.cardView.setCardBackgroundColor(values.getColor(R.color.red_500))

        binding.priceLabel.text = item.price
    }
}