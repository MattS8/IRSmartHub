package com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview_k

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

interface AGVBaseAdapter<T : RecyclerView.ViewHolder> {
    val actualItemCount: Int
    fun getItem(position: Int): AsymmetricItem
    fun notifyDataSetChanged()
    fun getItemViewType(actualIndex: Int): Int
    fun onCreateAsymmetricViewHolder(position: Int, parent: ViewGroup, viewType: Int): AsymmetricViewHolder<T>
    fun onBindAsymmetricViewHolder(holder: AsymmetricViewHolder<*>, parent: ViewGroup, position: Int)
}