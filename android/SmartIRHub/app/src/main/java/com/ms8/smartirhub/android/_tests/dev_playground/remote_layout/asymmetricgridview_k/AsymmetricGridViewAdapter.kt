package com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview_k

import android.content.Context
import android.database.DataSetObserver
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListAdapter
import android.widget.WrapperListAdapter
import androidx.recyclerview.widget.RecyclerView


class AsymmetricGridViewAdapter(
    context: Context,
    listView: AsymmetricGridView,
    private val wrappedAdapter: ListAdapter
)
    : BaseAdapter(), AGVBaseAdapter<RecyclerView.ViewHolder>, WrapperListAdapter {

    private val adapterImpl: AdapterImpl = AdapterImpl(context, this, listView)

    override val actualItemCount: Int
        get() = wrappedAdapter.count

    internal inner class GridDataSetObserver : DataSetObserver() {
        override fun onChanged() {
            recalculateItemsPerRow()
        }

        override fun onInvalidated() {
            recalculateItemsPerRow()
        }
    }

    init {
        wrappedAdapter.registerDataSetObserver(GridDataSetObserver())
    }

    override fun getItem(position: Int): AsymmetricItem {
        return wrappedAdapter.getItem(position) as AsymmetricItem
    }

    override fun onCreateAsymmetricViewHolder(
        position: Int, parent: ViewGroup, viewType: Int
    ): AsymmetricViewHolder<RecyclerView.ViewHolder> {
        return AsymmetricViewHolder(wrappedAdapter.getView(position, null, parent))
    }

    override fun onBindAsymmetricViewHolder(holder: AsymmetricViewHolder<*>, parent: ViewGroup, position: Int) {
        wrappedAdapter.getView(position, holder.itemView, parent)
    }

    override fun getItemId(position: Int): Long {
        return wrappedAdapter.getItemId(position)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val viewHolder = adapterImpl.onCreateViewHolder()
        adapterImpl.onBindViewHolder(viewHolder, position, parent)
        return viewHolder.itemView
    }

    override fun getCount(): Int {
        // Returns the row count for ListView display purposes
        return adapterImpl.rowCount
    }

    override fun getItemViewType(actualIndex: Int): Int {
        return wrappedAdapter.getItemViewType(actualIndex)
    }

    override fun getWrappedAdapter(): ListAdapter {
        return wrappedAdapter
    }

    internal fun recalculateItemsPerRow() {
        adapterImpl.recalculateItemsPerRow()
    }
}