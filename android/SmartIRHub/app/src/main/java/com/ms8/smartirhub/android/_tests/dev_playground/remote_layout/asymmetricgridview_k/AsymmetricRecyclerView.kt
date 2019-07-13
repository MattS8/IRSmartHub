package com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview_k

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

open class AsymmetricRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs), AsymmetricView {
    private val viewImpl: AsymmetricViewImpl = AsymmetricViewImpl(context)
    private var adapter: AsymmetricRecyclerViewAdapter<*>? = null

    override var isDebugging: Boolean
        get() = viewImpl.isDebugging
        set(debugging) {
            viewImpl.isDebugging = debugging
        }

    override val numColumns: Int
        get() = viewImpl.numColumns

    override val isAllowReordering: Boolean
        get() = viewImpl.isAllowReordering

    override val columnWidth: Int
        get() = viewImpl.getColumnWidth(availableSpace)

    private val availableSpace: Int
        get() = measuredWidth - paddingLeft - paddingRight

    override val divHeight: Int
        get() = 0

    override var requestedHorizontalSpacing: Int
        get() = viewImpl.requestedHorizontalSpacing
        set(spacing) {
            viewImpl.requestedHorizontalSpacing = spacing
        }

    init {
        layoutManager = LinearLayoutManager(context, VERTICAL, false)

        if (viewTreeObserver != null) {
            viewTreeObserver!!.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {

                    viewTreeObserver.removeGlobalOnLayoutListener(this)
                    viewImpl.determineColumns(availableSpace)
                    if (adapter != null) {
                        adapter!!.recalculateItemsPerRow()
                    }
                }
            })
        }
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        super.setAdapter(adapter)
        if (adapter !is AsymmetricRecyclerViewAdapter<*>) {
            throw UnsupportedOperationException(
                "Adapter must be an instance of AsymmetricRecyclerViewAdapter"
            )
        }

        this.adapter = adapter
        super.setAdapter(adapter)

        this.adapter!!.recalculateItemsPerRow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewImpl.determineColumns(availableSpace)
    }

    override fun fireOnItemClick(index: Int, v: View) {}

    override fun fireOnItemLongClick(index: Int, v: View): Boolean {
        return false
    }

    fun setRequestedColumnCount(requestedColumnCount: Int) {
        viewImpl.requestedColumnCount = requestedColumnCount
    }
}