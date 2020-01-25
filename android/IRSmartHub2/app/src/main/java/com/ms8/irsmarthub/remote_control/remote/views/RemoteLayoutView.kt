package com.ms8.irsmarthub.remote_control.remote.views

import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.recyclerview.widget.RecyclerView
import com.ms8.irsmarthub.R
import com.ms8.irsmarthub.remote_control.remote.views.asymmetric_gridview.AsymmetricItem
import com.ms8.irsmarthub.remote_control.remote.views.asymmetric_gridview.AsymmetricRecyclerView
import com.ms8.irsmarthub.remote_control.remote.views.asymmetric_gridview.AsymmetricRecyclerViewAdapter
import com.ms8.irsmarthub.remote_control.remote.views.asymmetric_gridview.Utils

class RemoteLayoutView(context: Context, attrs: AttributeSet): AsymmetricRecyclerView(context, attrs) {
    var topPadding = Utils.dpToPx(context, 56f)

    init {
        setRequestedColumnCount(4)
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

/*
----------------------------------------------
    Misc Objects
----------------------------------------------
*/
    inner class TopSpaceDecoration : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
            super.getItemOffsets(outRect, view, parent, state)

            if (parent.getChildAdapterPosition(view) == 0)
                outRect.top = topPadding
        }
    }

    class AsymItem : AsymmetricItem {
        var _columnSpan: Int = 1
        var _rowSpan: Int = 1
        var _position: Int = 0

//        override fun getColumnSpan() = _columnSpan
//        override fun getRowSpan() = _rowSpan

        override var columnSpan: Int
            get() = _columnSpan
            set(value) { _columnSpan = value }
        override var rowSpan: Int
            get() = _rowSpan
            set(value) { _rowSpan = value }


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

        companion object CREATOR : Parcelable.Creator<AsymItem> {
            override fun createFromParcel(parcel: Parcel): AsymItem {
                return AsymItem(parcel)
            }

            override fun newArray(size: Int): Array<AsymItem?> {
                return arrayOfNulls(size)
            }
        }
    }
}