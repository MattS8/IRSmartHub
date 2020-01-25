package com.ms8.irsmarthub.remote_control.remote.views.asymmetric_gridview

import androidx.recyclerview.widget.RecyclerView

abstract class AGVRecyclerViewAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
    abstract fun getItem(position: Int): AsymmetricItem
}