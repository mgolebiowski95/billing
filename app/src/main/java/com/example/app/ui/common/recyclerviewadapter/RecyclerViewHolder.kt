package com.example.app.ui.common.recyclerviewadapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

abstract class RecyclerViewHolder<in T, out VB : ViewDataBinding> private constructor(
    val binding: VB
) : RecyclerView.ViewHolder(binding.root) {

    constructor(layoutId: Int, parent: ViewGroup) : this(
        DataBindingUtil.inflate<VB>(LayoutInflater.from(parent.context), layoutId, parent, false)
    )

    abstract fun bind(item: T)
}