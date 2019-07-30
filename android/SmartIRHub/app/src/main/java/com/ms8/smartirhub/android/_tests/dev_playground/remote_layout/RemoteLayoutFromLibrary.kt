package com.ms8.smartirhub.android._tests.dev_playground.remote_layout

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AGVRecyclerViewAdapter
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricItem
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricRecyclerView
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricRecyclerViewAdapter
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview_k.Utils
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.IMG_ADD
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.IMG_SUBTRACT
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.STYLE_BTN_INCREMENTER_VERTICAL
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.STYLE_BTN_SINGLE_ACTION
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Properties.BgStyle
import com.ms8.smartirhub.android.remote_control.views.ButtonView

class RemoteLayoutFromLibrary(context: Context, attrs: AttributeSet): AsymmetricRecyclerView(context, attrs) {

    fun setupAdapter() {
        TempData.tempRemoteProfile.buttons
            .apply {
                for (i in 0 until 50) {
                    add(
                        RemoteProfile.Button()
                        .apply {
                            name = "Button $i"
                            when (i) {
                                0,1  -> {
                                    columnSpan = 2
                                }
                                2 -> {
                                    rowSpan = 2
                                    style = STYLE_BTN_INCREMENTER_VERTICAL
                                    properties[0].bgStyle = BgStyle.BG_ROUND_RECT_TOP
                                    properties[0].marginTop = 16
                                    properties[0].marginStart = 16
                                    properties[0].marginEnd = 16
                                    properties[0].marginBottom = 0
                                    properties[0].image = IMG_ADD

                                    properties.add(RemoteProfile.Button.Properties()
                                        .apply {
                                            bgStyle = BgStyle.BG_ROUND_RECT_BOTTOM
                                            marginTop = 0
                                            marginStart = 16
                                            marginBottom = 16
                                            marginEnd = 16
                                            image = IMG_SUBTRACT
                                        })

                                    name = "VOL"
                                }
                                7 -> {
                                    rowSpan = 2
                                    style = STYLE_BTN_INCREMENTER_VERTICAL
                                    properties[0].bgStyle = BgStyle.BG_ROUND_RECT_TOP
                                    properties[0].marginTop = 16
                                    properties[0].marginStart = 16
                                    properties[0].marginEnd = 16
                                    properties[0].marginBottom = 0
                                    properties[0].image = IMG_ADD

                                    properties.add(RemoteProfile.Button.Properties()
                                        .apply {
                                            bgStyle = BgStyle.BG_ROUND_RECT_BOTTOM
                                            marginTop = 0
                                            marginStart = 16
                                            marginBottom = 16
                                            marginEnd = 16
                                            image = IMG_SUBTRACT
                                        })

                                    name = "CH"
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

            return DemoItem(b.columnSpan, b.rowSpan, position)
        }

        override fun getItemViewType(position: Int): Int {
            return TempData.tempRemoteProfile.buttons[position].style
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
        when (viewType) {
            STYLE_BTN_SINGLE_ACTION -> LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_base, parent, false)
            STYLE_BTN_INCREMENTER_VERTICAL -> LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_inc_vert, parent, false)
            else -> LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_base, parent, false)
        }
    ) {
        var button: RemoteProfile.Button? = null

        fun bind(position: Int) {
            button = TempData.tempRemoteProfile.buttons[position]
            if (button == null)
                Log.w("TEST##", "BUTTON WAS NULL @ $position")
            button?.let { b ->
                when (b.style) {
                    STYLE_BTN_SINGLE_ACTION -> bindSingleActionButton(b)
                    STYLE_BTN_INCREMENTER_VERTICAL -> bindIncrementerButton(b)
                    else -> bindSingleActionButton(b)
                }
//                buttonView.properties = b.properties
//                buttonView.buttonText = b.name
//
//                itemView.visibility = if (b.properties.bgStyle != BgStyle.BG_INVISIBLE)
//                        View.VISIBLE
//                    else
//                        View.INVISIBLE
//                itemView.invalidate()
            }
        }

        private fun bindIncrementerButton(button: RemoteProfile.Button) {
            val topButtonView = itemView.findViewById<ButtonView>(R.id.btnTop)
            val bottomButtonView = itemView.findViewById<ButtonView>(R.id.btnBottom)
            val buttonText = itemView.findViewById<TextView>(R.id.txtButtonName)

            // set top button properties
            topButtonView.properties = button.properties[0]
            topButtonView.setOnClickListener { Log.d("TEST", "TOP BUTTON CLICKED") }
            // set bottom button properties
            bottomButtonView.properties = button.properties[1]
            bottomButtonView.setOnClickListener { Log.d("TEST", "BOTTOM BUTTON CLICKED") }
            // set middle textView text
            buttonText.text = button.name
            // Backwards compatible autoSizeText
            TextViewCompat.setAutoSizeTextTypeWithDefaults(buttonText, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
        }

        private fun bindSingleActionButton(button: RemoteProfile.Button) {
            val buttonView = itemView.findViewById<ButtonView>(R.id.btnBackground)

            // set button properties
            buttonView.properties = button.properties[0]
            // set button text
            buttonView.buttonText = button.name
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