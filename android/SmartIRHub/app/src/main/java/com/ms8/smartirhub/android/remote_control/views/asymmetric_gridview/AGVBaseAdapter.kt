package com.ms8.smartirhub.android.remote_control.views.asymmetric_gridview

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