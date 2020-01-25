package com.ms8.irsmarthub.remote_control.remote.views.asymmetric_gridview

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ListAdapter
import android.widget.ListView
import androidx.annotation.NonNull

open class AsymmetricGridView(context: Context, attrs: AttributeSet) : ListView(context, attrs),
    AsymmetricView {
    override val divHeight = 0
    //protected var onItemClickListener: AdapterView.OnItemClickListener? = null
    //protected var onItemLongClickListener: AdapterView.OnItemLongClickListener? = null
    protected var gridAdapter: AsymmetricGridViewAdapter? = null
    private val viewImpl: AsymmetricViewImpl =
        AsymmetricViewImpl(context)

    override var requestedHorizontalSpacing: Int
        get() = viewImpl.requestedHorizontalSpacing
        set(spacing) {
            viewImpl.requestedHorizontalSpacing = spacing
        }

    override val numColumns: Int
        get() = viewImpl.numColumns

    override val columnWidth: Int
        get() = viewImpl.getColumnWidth(availableSpace)

    private val availableSpace: Int
        get() = measuredWidth - paddingLeft - paddingRight

    override var isAllowReordering: Boolean
        get() = viewImpl.isAllowReordering
        set(allowReordering) {
            viewImpl.isAllowReordering = allowReordering
            if (gridAdapter != null) {
                gridAdapter!!.recalculateItemsPerRow()
            }
        }

    override var isDebugging: Boolean
        get() = viewImpl.isDebugging
        set(debugging) {
            viewImpl.isDebugging = debugging
        }

    init {

        val vto = viewTreeObserver
        vto?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {

                viewTreeObserver.removeGlobalOnLayoutListener(this)
                viewImpl.determineColumns(availableSpace)
                if (gridAdapter != null) {
                    gridAdapter!!.recalculateItemsPerRow()
                }
            }
        })
    }

//    override fun setOnItemClickListener(listener: OnItemClickListener?) {
//        super.setOnItemClickListener(listener)
//    }

//    override fun setOnItemClickListener(listener: OnItemClickListener?) {
//        onItemClickListener = listener
//    }

    override fun fireOnItemClick(position: Int, v: View) {
        if (onItemClickListener != null) {
            onItemClickListener!!.onItemClick(this, v, position, v.id.toLong())
        }
    }

//    override fun setOnItemLongClickListener(listener: AdapterView.OnItemLongClickListener) {
//        onItemLongClickListener = listener
//    }

    override fun fireOnItemLongClick(position: Int, v: View): Boolean {
        return onItemLongClickListener != null && onItemLongClickListener!!
            .onItemLongClick(this, v, position, v.id.toLong())
    }

    override fun setAdapter(@NonNull adapter: ListAdapter) {
        if (adapter !is AsymmetricGridViewAdapter) {
            throw UnsupportedOperationException(
                "Adapter must be an instance of AsymmetricGridViewAdapter"
            )
        }

        gridAdapter = adapter
        super.setAdapter(adapter)

        gridAdapter!!.recalculateItemsPerRow()
    }

    fun setRequestedColumnWidth(width: Int) {
        viewImpl.requestedColumnWidth = width
    }

    fun setRequestedColumnCount(requestedColumnCount: Int) {
        viewImpl.requestedColumnCount = requestedColumnCount
    }

    fun determineColumns() {
        viewImpl.determineColumns(availableSpace)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewImpl.determineColumns(availableSpace)
    }

    @NonNull
    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        return superState?.let { viewImpl.onSaveInstanceState(it) }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is AsymmetricViewImpl.SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)

        viewImpl.onRestoreInstanceState(state)

        setSelectionFromTop(20, 0)
    }
}