package com.ms8.smartirhub.android._tests.dev_playground.remote_layout

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AGVRecyclerViewAdapter
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricItem
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricRecyclerView
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricRecyclerViewAdapter
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview_k.Utils
import com.ms8.smartirhub.android.custom_views.ButtonView
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.models.firestore.RemoteProfile
import com.ms8.smartirhub.android.models.firestore.RemoteProfile.Button.Properties.*
import com.wajahatkarim3.easyvalidation.core.view_ktx.validUrl
import org.jetbrains.anko.backgroundResource

class RemoteLayoutFromLibrary(context: Context, attrs: AttributeSet): AsymmetricRecyclerView(context, attrs) {

    fun setupAdapter() {
        TempData.tempRemoteProfile.buttons
            .apply {
                for (i in 0 until 50) {
                    add(RemoteProfile.Button()
                        .apply {
                            name = "Button $i"
                            when (i) {
                                4 -> {
                                    style = RemoteProfile.Button.STYLE_BUTTON
                                    properties.bgStyle = BgStyle.BG_ROUND_RECT_TOP
                                    properties.marginTop = 16
                                    properties.marginStart = 16
                                    properties.marginEnd = 16
                                    properties.marginBottom = 0
                                }
                                8 -> {
                                    style = RemoteProfile.Button.STYLE_BUTTON
                                    properties.bgStyle = BgStyle.BG_ROUND_RECT_BOTTOM
                                    properties.marginTop = 0
                                    properties.marginStart = 16
                                    properties.marginEnd = 16
                                    properties.marginBottom = 16
                                }
                                1,2 -> {
                                    style = RemoteProfile.Button.STYLE_SPACE
                                    properties.bgStyle = BgStyle.BG_INVISIBLE
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
        var buttonText: TextView = itemView.findViewById(R.id.btnText)
        var buttonBackground: ButtonView = itemView.findViewById(R.id.btnBackground)

        fun bind(position: Int) {
            button = TempData.tempRemoteProfile.buttons[position]
            button?.let { b ->
                buttonText.text = b.name
                val layoutParams = buttonBackground.layoutParams as MarginLayoutParams
                Log.d("TEST", "Setting bottom margin to ${Utils.dpToPx(itemView.context, b.properties.marginStart.toFloat())} for button $position")
                layoutParams.setMargins(
                    Utils.dpToPx(itemView.context, b.properties.marginStart.toFloat()),
                    Utils.dpToPx(itemView.context, b.properties.marginTop.toFloat()),
                    Utils.dpToPx(itemView.context, b.properties.marginEnd.toFloat()),
                    Utils.dpToPx(itemView.context, b.properties.marginBottom.toFloat())
                    )
                when (b.properties.bgStyle) {
                    BgStyle.BG_CIRCLE -> {
                        buttonBackground.backgroundResource = R.drawable.btn_bg_circle
                    }
                    BgStyle.BG_ROUND_RECT -> {
                        buttonBackground.backgroundResource = R.drawable.btn_bg_round_rect
                    }
                    BgStyle.BG_ROUND_RECT_BOTTOM -> {
                        buttonBackground.backgroundResource = R.drawable.btn_bg_round_bottom
                    }
                    BgStyle.BG_ROUND_RECT_TOP -> {
                        buttonBackground.backgroundResource = R.drawable.btn_bg_round_top
                    }
                    BgStyle.BG_CUSTOM_IMAGE -> {
                        val url = b.properties.bgUrl
                        if (url.validUrl()) {
                            Glide.with(buttonBackground).load(url).into(object : CustomTarget<Drawable>() {
                                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                    buttonBackground.background = resource
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {}
                            })
                        }
                    }
                    BgStyle.BG_INVISIBLE -> {
                        buttonBackground.backgroundResource = 0
                    }
                }
                buttonBackground.layoutParams = layoutParams
                buttonBackground.outlineProvider = button?.properties?.let { ButtonView.ButtonOutlineProvider(it) }
                itemView.visibility = if (button?.properties?.bgStyle != BgStyle.BG_INVISIBLE)
                        View.VISIBLE
                    else
                        View.INVISIBLE
                itemView.invalidate()
            }




//            when (button?.style ?: RemoteProfile.Button.STYLE_SPACE) {
//                RemoteProfile.Button.STYLE_SPACE -> {
//                    Log.d("TEST", "Hiding space at pos $position")
//                    itemView.visibility = View.INVISIBLE
//                    itemView.isEnabled = false
//                }
//                else -> {
//                    itemView.visibility = View.VISIBLE
//                    itemView.isEnabled = true
//                }
//            }
//            var lp = itemView.layoutParams
            itemView.invalidate()
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