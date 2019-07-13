package com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview_k

import androidx.recyclerview.widget.RecyclerView

abstract class AGVRecyclerViewAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
    abstract fun getItem(position: Int): AsymmetricItem
}