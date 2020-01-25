package com.ms8.irsmarthub.remote_control.remote.views.asymmetric_gridview

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