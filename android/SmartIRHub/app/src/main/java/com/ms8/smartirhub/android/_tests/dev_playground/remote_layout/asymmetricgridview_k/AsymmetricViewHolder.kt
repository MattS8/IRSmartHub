package com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview_k

import android.view.View
import androidx.recyclerview.widget.RecyclerView

class AsymmetricViewHolder<VH : RecyclerView.ViewHolder> : RecyclerView.ViewHolder {
    internal val wrappedViewHolder: VH?

    constructor(wrappedViewHolder: VH) : super(wrappedViewHolder.itemView) {
        this.wrappedViewHolder = wrappedViewHolder
    }

    constructor(view: View) : super(view) {
        wrappedViewHolder = null
    }
}