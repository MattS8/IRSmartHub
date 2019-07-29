package com.ms8.smartirhub.android._tests.dev_playground.remote_layout

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AGVRecyclerViewAdapter
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricItem
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricRecyclerView
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricRecyclerViewAdapter
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview_k.Utils
import com.ms8.smartirhub.android.custom_views.ButtonView
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.models.firestore.RemoteProfile
import com.ms8.smartirhub.android.models.firestore.RemoteProfile.Button.Properties.BgStyle

class RemoteLayoutFromLibrary(context: Context, attrs: AttributeSet): AsymmetricRecyclerView(context, attrs) {

    fun setupAdapter() {
        TempData.tempRemoteProfile.buttons
            .apply {
                for (i in 0 until 50) {
                    add(RemoteProfile.Button()
                        .apply {
                            name = "Button $i"
                            when (i) {
                                0,1  -> {
                                    properties.columnSpan = 2
                                }
                                2 -> {
                                    style = RemoteProfile.Button.STYLE_BTN_INCREMENTER_VERTICAL
                                    properties.bgStyle = BgStyle.BG_ROUND_RECT_TOP
                                    properties.marginTop = 16
                                    properties.marginStart = 16
                                    properties.marginEnd = 16
                                    properties.marginBottom = 0
                                    properties.rowSpan = 2
                                }
                                8 -> {
                                    style = RemoteProfile.Button.STYLE_BTN_SINGLE_ACTION
                                    properties.bgStyle = BgStyle.BG_ROUND_RECT_BOTTOM
                                    properties.marginTop = 0
                                    properties.marginStart = 16
                                    properties.marginEnd = 16
                                    properties.marginBottom = 16
                                }
                            }
                        })
                }
            }

        adapter = AsymmetricRecyclerViewAdapter(context, this, RemoteLayoutFromLibraryAdapter())
    }

    init {
        setRequestedColumnCount(3)
        //isDebugging = true
        requestedHorizontalSpacing = Utils.dpToPx(context, 0f)
        //addItemDecoration(SpacesItemDecoration(Utils.dpToPx(context, 3f)))
    }


    class RemoteLayoutFromLibraryAdapter: AGVRecyclerViewAdapter<RemoteLayoutFromLibraryViewHolder>() {

        override fun getItem(position: Int): AsymmetricItem {
            val b = TempData.tempRemoteProfile.buttons[position]

            return DemoItem(b.properties.columnSpan, b.properties.rowSpan, position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RemoteLayoutFromLibraryViewHolder {
            return RemoteLayoutFromLibraryViewHolder(parent, viewType)
        }

        override fun getItemCount() =
            TempData.tempRemoteProfile.buttons.size

        override fun onBindViewHolder(holder: RemoteLayoutFromLibraryViewHolder, position: Int) {
            Log.d("TEST", "Binding ${TempData.tempRemoteProfile.buttons[position]?.name}")
            holder.bind(position)
        }
    }

    class RemoteLayoutFromLibraryViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_base, parent, false)
    ) {
        var button: RemoteProfile.Button? = null
        //var buttonText: TextView = itemView.findViewById(R.id.btnText)
        var buttonView: ButtonView = itemView.findViewById(R.id.btnBackground)

        fun bind(position: Int) {
            button = TempData.tempRemoteProfile.buttons[position]
            if (button == null)
                Log.w("TEST##", "BUTTON WAS NULL @ $position")
            button?.let { b ->
                buttonView.properties = b.properties
                buttonView.buttonText = b.name

                itemView.visibility = if (b.properties.bgStyle != BgStyle.BG_INVISIBLE)
                        View.VISIBLE
                    else
                        View.INVISIBLE
                itemView.invalidate()
            }
        }
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
}