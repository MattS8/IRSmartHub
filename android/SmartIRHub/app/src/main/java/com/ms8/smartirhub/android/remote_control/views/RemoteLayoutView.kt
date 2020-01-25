package com.ms8.smartirhub.android.remote_control.views

import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AGVRecyclerViewAdapter
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricItem
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricRecyclerView
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricRecyclerViewAdapter
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.remote_control.views.asymmetric_gridview.Utils
import com.ms8.smartirhub.android.firebase.RealtimeDatabaseFunctions
import com.ms8.smartirhub.android.remote_control.button.creation.ButtonCreator.Companion.NEW_BUTTON
import com.ms8.smartirhub.android.remote_control.button.models.Button
import com.ms8.smartirhub.android.remote_control.button.views.RemoteButtonView
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile


class RemoteLayoutView(context: Context, attrs: AttributeSet): AsymmetricRecyclerView(context, attrs) {
    var topPadding = Utils.dpToPx(context, 56f)

    init {
        setRequestedColumnCount(4)
        //isDebugging = true
        requestedHorizontalSpacing = Utils.dpToPx(context, 0f)
        addItemDecoration(TopSpaceDecoration())
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                view?.let { v ->
                    outline?.setRoundRect(0, 0, width, height, v.resources.getDimension(R.dimen.rmt_corner_radius))
                }
            }
        }
        clipToOutline = true
    }

    fun setupAdapter(remoteLayoutAdapter : RemoteLayoutAdapter) {
        adapter = AsymmetricRecyclerViewAdapter(context, this, remoteLayoutAdapter)
    }

    class DemoItem : AsymmetricItem {
        var _columnSpan: Int = 1
        var _rowSpan: Int = 1
        var _position: Int = 0

        override fun getColumnSpan() = _columnSpan

        override fun getRowSpan() = _rowSpan

        @JvmOverloads
        constructor(columnSpan: Int = 1, rowSpan: Int = 1, position: Int = 0) {
            _columnSpan = columnSpan
            _rowSpan = rowSpan
            _position = position
        }

        constructor(`in`: Parcel) {
            readFromParcel(`in`)
        }

        override fun toString(): String {
            return String.format("%s: %sx%s", _position, _rowSpan, _columnSpan)
        }

        override fun describeContents(): Int {
            return 0
        }

        private fun readFromParcel(`in`: Parcel) {
            _columnSpan = `in`.readInt()
            _rowSpan = `in`.readInt()
            _position = `in`.readInt()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(_columnSpan)
            dest.writeInt(_rowSpan)
            dest.writeInt(_position)
        }

        companion object CREATOR : Parcelable.Creator<DemoItem> {
            override fun createFromParcel(parcel: Parcel): DemoItem {
                return DemoItem(parcel)
            }

            override fun newArray(size: Int): Array<DemoItem?> {
                return arrayOfNulls(size)
            }
        }
    }

    inner class TopSpaceDecoration : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
            super.getItemOffsets(outRect, view, parent, state)

            if (parent.getChildAdapterPosition(view) == 0)
                outRect.top = topPadding
        }
    }
}