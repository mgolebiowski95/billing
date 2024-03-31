package com.example.app.ui.views.produktlist

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
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
        sb.appendLine("productId=${item.productId}")
        binding.textView.text = sb.toString()

        val color = when {
            item.isPurchased -> R.color.white
            item.canBePurchased -> R.color.white
            !item.canBePurchased -> R.color.grey_200
            else -> View.NO_ID
        }
        binding.cardView.setCardBackgroundColor(values.getColor(color))
        binding.checkIcon.isVisible = item.isPurchased

        binding.priceLabel.text = item.price
    }
}