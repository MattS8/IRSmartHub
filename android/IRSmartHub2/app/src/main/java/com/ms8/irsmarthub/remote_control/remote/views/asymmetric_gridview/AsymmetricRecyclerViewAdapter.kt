package com.ms8.irsmarthub.remote_control.remote.views.asymmetric_gridview

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class AsymmetricRecyclerViewAdapter<T : RecyclerView.ViewHolder>(
    context: Context, private val recyclerView: AsymmetricRecyclerView,
    private val wrappedAdapter: AGVRecyclerViewAdapter<T>
) : RecyclerView.Adapter<AdapterImpl.ViewHolder>(),
    AGVBaseAdapter<T> {
    private val adapterImpl: AdapterImpl =
        AdapterImpl(
            context,
            this,
            recyclerView
        )

    override val actualItemCount: Int
        get() = wrappedAdapter.itemCount

    init {
        wrappedAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                recalculateItemsPerRow()
            }
        })
    }

    override fun getItemCount() = adapterImpl.rowCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterImpl.ViewHolder {
        return adapterImpl.onCreateViewHolder()
    }

    override fun onBindViewHolder(holder: AdapterImpl.ViewHolder, position: Int) {
        adapterImpl.onBindViewHolder(holder, position, recyclerView)
    }

    override fun getItem(position: Int): AsymmetricItem {
        return wrappedAdapter.getItem(position)
    }

    override fun onCreateAsymmetricViewHolder(
        position: Int, parent: ViewGroup, viewType: Int
    ): AsymmetricViewHolder<T> {
        return AsymmetricViewHolder(
            wrappedAdapter.onCreateViewHolder(parent, viewType)
        )
    }

    override fun onBindAsymmetricViewHolder(holder: AsymmetricViewHolder<*>, parent: ViewGroup, position: Int) {
        holder.wrappedViewHolder?.let {
            wrappedAdapter.onBindViewHolder(it as T, position)
        }
    }

//    override fun onBindAsymmetricViewHolder(holder: AsymmetricViewHolder<T>, parent: ViewGroup, position: Int) {
//        holder.wrappedViewHolder?.let {
//            wrappedAdapter.onBindViewHolder(it, position)
//        }
//    }

    override fun getItemViewType(actualIndex: Int): Int {
        return wrappedAdapter.getItemViewType(actualIndex)
    }

    internal fun recalculateItemsPerRow() {
        adapterImpl.recalculateItemsPerRow()
    }
}