package com.example.app.ui.common.recyclerviewadapter

import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

abstract class RecyclerViewAdapter<T> private constructor(private val diffCallback: DiffCallback<T> = DiffCallback()) :
    ListAdapter<T, RecyclerViewHolder<T, ViewDataBinding>>(diffCallback) {

    constructor() : this(DiffCallback())

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        diffCallback.setAdapter(this)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        diffCallback.setAdapter(null)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerViewHolder<T, ViewDataBinding> {

        @Suppress("UNCHECKED_CAST")
        return viewHolder(parent, viewType) as RecyclerViewHolder<T, ViewDataBinding>
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder<T, ViewDataBinding>, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        return viewType(getItem(position))
    }

    abstract fun viewType(item: T): Int

    abstract fun viewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerViewHolder<*, ViewDataBinding>

    open fun areItemsTheSame(oldItem: T, newItem: T): Boolean = false

    open fun areContentsTheSame(oldItem: T, newItem: T): Boolean = false

    private class DiffCallback<T> : DiffUtil.ItemCallback<T>() {
        private var adapter: RecyclerViewAdapter<T>? = null

        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean =
            adapter?.areItemsTheSame(oldItem, newItem) ?: false

        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean =
            adapter?.areContentsTheSame(oldItem, newItem) ?: false

        fun setAdapter(adapter: RecyclerViewAdapter<T>?) {
            this.adapter = adapter
        }
    }
}