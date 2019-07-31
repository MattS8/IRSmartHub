package com.ms8.smartirhub.android.remote_control.views.asymmetric_gridview

import androidx.recyclerview.widget.RecyclerView

abstract class AGVRecyclerViewAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
    abstract fun getItem(position: Int): AsymmetricItem
}