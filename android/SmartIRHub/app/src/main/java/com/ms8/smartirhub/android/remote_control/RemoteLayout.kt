package com.ms8.smartirhub.android.remote_control

import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.databinding.ObservableList
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AGVRecyclerViewAdapter
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricItem
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricRecyclerView
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricRecyclerViewAdapter
import com.ms8.smartirhub.android.remote_control.views.asymmetric_gridview.Utils
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.firebase.RealtimeDatabaseFunctions
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.STYLE_BTN_INCREMENTER_VERTICAL
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.STYLE_BTN_RADIAL
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.STYLE_BTN_RADIAL_W_CENTER
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.STYLE_BTN_SINGLE_ACTION
import com.ms8.smartirhub.android.remote_control.views.ButtonView

class RemoteLayout(context: Context, attrs: AttributeSet): AsymmetricRecyclerView(context, attrs) {

    private var lastTopChild = 0
    var topPadding = Utils.dpToPx(context, 56f)

    private var isListening : Boolean = false
    private val buttonListener = object : ObservableList.OnListChangedCallback<ObservableList<RemoteProfile.Button>>() {
        override fun onChanged(sender: ObservableList<RemoteProfile.Button>?) {
            adapter?.notifyDataSetChanged()
        }

        override fun onItemRangeRemoved(sender: ObservableList<RemoteProfile.Button>?, positionStart: Int, itemCount: Int) {
            adapter?.notifyItemRangeRemoved(positionStart, itemCount)
            findLastTopChild()
        }

        override fun onItemRangeMoved(sender: ObservableList<RemoteProfile.Button>?, fromPosition: Int, toPosition: Int, itemCount: Int) {
            adapter?.notifyDataSetChanged()
            findLastTopChild()
        }

        override fun onItemRangeInserted(sender: ObservableList<RemoteProfile.Button>?, positionStart: Int, itemCount: Int) {
            adapter?.notifyItemRangeInserted(positionStart, itemCount)
            findLastTopChild()
        }

        override fun onItemRangeChanged(sender: ObservableList<RemoteProfile.Button>?, positionStart: Int, itemCount: Int) {
            adapter?.notifyItemRangeChanged(positionStart, itemCount)
            findLastTopChild()
        }
    }

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
        //addItemDecoration(SpacesItemDecoration(Utils.dpToPx(context, 3f)))
    }

    fun findLastTopChild() {

        var spanTotal = 0
        var inspectingPos = 0
        do {
            spanTotal += TempData.tempRemoteProfile.buttons[inspectingPos++].columnSpan
        } while (spanTotal <  numColumns && inspectingPos < TempData.tempRemoteProfile.buttons.size)
        lastTopChild = inspectingPos
        Log.d("TEST##", "Finding lastTopChild... $lastTopChild")
    }

    fun setupAdapter() {
        Log.d("TEST###", "Setting up adapter!@!!")
        adapter = AsymmetricRecyclerViewAdapter(context, this, RemoteLayoutAdapter())
    }

    fun startListening() {
        if (!isListening) {
            isListening = true
            TempData.tempRemoteProfile.buttons.addOnListChangedCallback(buttonListener)

            // update last child on top row if TempRemoteProfile already has some buttons
            if (TempData.tempRemoteProfile.buttons.size > 0)
                findLastTopChild()
        }
    }

    fun stopListening() {
        TempData.tempRemoteProfile.buttons.removeOnListChangedCallback(buttonListener)
    }

    class RemoteLayoutAdapter: AGVRecyclerViewAdapter<ButtonViewHolder>() {

        override fun getItem(position: Int): AsymmetricItem {
            val b = TempData.tempRemoteProfile.buttons[position]
            return DemoItem(
                b.columnSpan,
                b.rowSpan,
                position
            )
        }

        override fun getItemViewType(position: Int)
                =  TempData.tempRemoteProfile.buttons[position].style

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
            return ButtonViewHolder(parent, viewType)
        }

        override fun getItemCount() =
            TempData.tempRemoteProfile.buttons.size

        override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
            holder.bind(position)
        }


    }

    class ButtonViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder(
        when (viewType) {
            STYLE_BTN_SINGLE_ACTION -> LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_base, parent, false)
            STYLE_BTN_INCREMENTER_VERTICAL -> LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_inc_vert, parent, false)
            STYLE_BTN_RADIAL_W_CENTER -> LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_radial_w_center_btn, parent, false)
            STYLE_BTN_RADIAL -> LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_radial_w_center_btn, parent, false)

            else -> LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_base, parent, false)
        }
    ) {
        var button: RemoteProfile.Button? = null

        fun bind(position: Int) {
            button = TempData.tempRemoteProfile.buttons[position]
            button?.let { b ->
                when (b.style) {
                    STYLE_BTN_SINGLE_ACTION -> bindSingleActionButton(b)
                    STYLE_BTN_INCREMENTER_VERTICAL -> bindIncrementerButton(b)
                    STYLE_BTN_RADIAL_W_CENTER -> bindRadialButton(b, true)
                    STYLE_BTN_RADIAL -> bindRadialButton(b, false)
                    else -> bindSingleActionButton(b)
                }
            }
        }

        private fun bindRadialButton(button: RemoteProfile.Button, withCenterButton: Boolean) {
            val topButtonView = itemView.findViewById<ButtonView>(R.id.btnTop)
            val bottomButtonView = itemView.findViewById<ButtonView>(R.id.btnBottom)
            val startButtonView = itemView.findViewById<ButtonView>(R.id.btnStart)
            val endButtonView = itemView.findViewById<ButtonView>(R.id.btnEnd)
            val centerButtonView = itemView.findViewById<ButtonView>(R.id.btnCenter)

            // set top button properties
            topButtonView.properties = button.properties[0]
            topButtonView.setOnClickListener {
                RealtimeDatabaseFunctions.sendCommandToHub(button.command[0])
                Log.d("TEST", "TOP BUTTON CLICKED")
            }
            // set bottom button properties
            endButtonView.properties = button.properties[1]
            endButtonView.setOnClickListener {
                RealtimeDatabaseFunctions.sendCommandToHub(button.command[1])
                Log.d("TEST", "END BUTTON CLICKED")
            }
            // set start button properties
            bottomButtonView.properties = button.properties[2]
            bottomButtonView.setOnClickListener {
                RealtimeDatabaseFunctions.sendCommandToHub(button.command[2])
                Log.d("TEST", "BOTTOM BUTTON CLICKED")
            }
            // set end button properties
            startButtonView.properties = button.properties[3]
            startButtonView.setOnClickListener {
                RealtimeDatabaseFunctions.sendCommandToHub(button.command[3])
                Log.d("TEST", "START BUTTON CLICKED")
            }

            if (withCenterButton) {
                // make sure button is visible and enabled
                centerButtonView.visibility = View.VISIBLE
                centerButtonView.isEnabled = true
                // set center button properties
                centerButtonView.properties = button.properties[4]
                centerButtonView.setOnClickListener {
                    RealtimeDatabaseFunctions.sendCommandToHub(button.command[4])
                }
                // set center button text
                centerButtonView.buttonText = button.name
            } else {
                // make sure button is visible and enabled
                centerButtonView.visibility = View.INVISIBLE
                centerButtonView.isEnabled = false
            }
        }

        private fun bindIncrementerButton(button: RemoteProfile.Button) {
            val topButtonView = itemView.findViewById<ButtonView>(R.id.btnTop)
            val bottomButtonView = itemView.findViewById<ButtonView>(R.id.btnBottom)
            val buttonText = itemView.findViewById<TextView>(R.id.txtButtonName)

            // set top button properties
            topButtonView.properties = button.properties[0]
            //todo replace with proper onClick
            topButtonView.setOnClickListener { Log.d("TEST", "TOP BUTTON CLICKED") }
            // set bottom button properties
            bottomButtonView.properties = button.properties[1]
            //todo replace with proper onClick
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
            //todo replace with proper onClick
            buttonView.setOnClickListener { Log.d("TEST", "BUTTON CLICKED") }
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

    inner class TopSpaceDecoration() : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
            super.getItemOffsets(outRect, view, parent, state)

            if (parent.getChildAdapterPosition(view) == 0)
                outRect.top = topPadding
        }
    }
}