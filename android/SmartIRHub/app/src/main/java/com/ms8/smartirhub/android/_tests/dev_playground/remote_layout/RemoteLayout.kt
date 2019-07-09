package com.ms8.smartirhub.android._tests.dev_playground.remote_layout

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager.GAP_HANDLING_NONE
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeRecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.layout_manager.RemoteLayoutManager
import com.ms8.smartirhub.android.models.firestore.RemoteProfile

class RemoteLayout(
    context      : Context,
    attributeSet : AttributeSet?    = null
) : DragDropSwipeRecyclerView(context, attributeSet) {

    var widthCount: Int = 4
    set(value) {
        field = value
        layoutManager = getRemoteLayoutManager()
        adapter = getRemoteAdapter()
    }

    var remoteProfile: RemoteProfile? = null
    set(value) {
        field = value
        (adapter as RemoteLayoutAdapter?)?.apply {
            dataSet = field?.buttons?.toList() ?: emptyList()
            notifyDataSetChanged()
        }
    }

    private fun getRemoteAdapter(): RemoteLayoutAdapter {
        return RemoteLayoutAdapter(remoteProfile?.buttons?.toList() ?: emptyList())
            .apply {
                remoteProperties = object : RemoteLayoutAdapter.RemoteLayoutProperties {
                    override fun getWidth() = width
                }
            }
    }

    private fun getRemoteLayoutManager() : RemoteLayoutManager {
        return RemoteLayoutManager(widthCount, VERTICAL or HORIZONTAL)
            .apply {
                gapStrategy = GAP_HANDLING_NONE
            }
    }

    init {
        orientation = ListOrientation.GRID_LIST_WITH_HORIZONTAL_SWIPING
        orientation?.removeSwipeDirectionFlag(ListOrientation.DirectionFlag.LEFT)
        orientation?.removeSwipeDirectionFlag(ListOrientation.DirectionFlag.RIGHT)
        layoutManager = getRemoteLayoutManager()
        itemLayoutId = R.layout.v_rmt_btn_base
        adapter = getRemoteAdapter()
    }

    class RemoteLayoutAdapter(dataSet: List<RemoteProfile.Button> = emptyList())
        : DragDropSwipeAdapter<RemoteProfile.Button, RemoteLayoutAdapter.ViewHolder>(dataSet) {

        var remoteProperties : RemoteLayoutProperties? = null

        override fun getViewHolder(itemView: View): ViewHolder = ViewHolder(itemView)

        override fun onBindViewHolder(item: RemoteProfile.Button, viewHolder: ViewHolder, position: Int) {
            viewHolder.innerView.text = item.name
            when (position) {
                2,6,8 -> {
                    remoteProperties?.getWidth()?.let {
                        val layoutParams = viewHolder.innerView.layoutParams
                        layoutParams.width = it/3
                        layoutParams.height = it/2
                        viewHolder.itemView.layoutParams = layoutParams
                        viewHolder.itemView.invalidate()
                    }
                }
            }
        }

        override fun getViewToTouchToStartDraggingItem(
            item: RemoteProfile.Button,
            viewHolder: ViewHolder,
            position: Int
        ): View? {
            return null
        }

        interface RemoteLayoutProperties {
            fun getWidth() : Int
        }

        class ViewHolder(itemView: View) : DragDropSwipeAdapter.ViewHolder(itemView) {
            val innerView: TextView = itemView.findViewById(R.id.btnRmtInnerView)
        }
    }
}