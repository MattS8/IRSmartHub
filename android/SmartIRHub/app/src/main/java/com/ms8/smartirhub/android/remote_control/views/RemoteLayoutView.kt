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
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import androidx.databinding.Observable
import androidx.databinding.ObservableList
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AGVRecyclerViewAdapter
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricItem
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricRecyclerView
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricRecyclerViewAdapter
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.remote_control.views.asymmetric_gridview.Utils
import com.ms8.smartirhub.android.firebase.RealtimeDatabaseFunctions
import com.ms8.smartirhub.android.remote_control.button.models.Button
import com.ms8.smartirhub.android.remote_control.button.views.RemoteButtonView


class RemoteLayoutView(context: Context, attrs: AttributeSet): AsymmetricRecyclerView(context, attrs) {

    var topPadding = Utils.dpToPx(context, 56f)

    private var isListening : Boolean = false
    private val buttonListener = object : ObservableList.OnListChangedCallback<ObservableList<Button>>() {
        override fun onChanged(sender: ObservableList<Button>?) {
            adapter?.notifyDataSetChanged()
        }

        override fun onItemRangeRemoved(sender: ObservableList<Button>?, positionStart: Int, itemCount: Int) {
            adapter?.notifyItemRangeRemoved(positionStart, itemCount)
        }

        override fun onItemRangeMoved(sender: ObservableList<Button>?, fromPosition: Int, toPosition: Int, itemCount: Int) {
            adapter?.notifyDataSetChanged()
        }

        override fun onItemRangeInserted(sender: ObservableList<Button>?, positionStart: Int, itemCount: Int) {
            adapter?.notifyItemRangeInserted(positionStart, itemCount)
        }

        override fun onItemRangeChanged(sender: ObservableList<Button>?, positionStart: Int, itemCount: Int) {
            adapter?.notifyItemRangeChanged(positionStart, itemCount)
        }
    }
    private val editModeListener  =  object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) { remoteLayoutAdapter.notifyDataSetChanged() }
    }
    private val remoteLayoutAdapter =
        RemoteLayoutAdapter()


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

    fun setupAdapter() {
        adapter = AsymmetricRecyclerViewAdapter(context, this, remoteLayoutAdapter)
    }

    fun startListening() {
        if (!isListening) {
            isListening = true
            AppState.tempData.tempRemoteProfile.buttons.addOnListChangedCallback(buttonListener)
            AppState.tempData.tempRemoteProfile.inEditMode.addOnPropertyChangedCallback(editModeListener)
        }
    }

    fun stopListening() {
        AppState.tempData.tempRemoteProfile.buttons.removeOnListChangedCallback(buttonListener)
        AppState.tempData.tempRemoteProfile.inEditMode.removeOnPropertyChangedCallback(editModeListener)
    }

    class RemoteLayoutAdapter: AGVRecyclerViewAdapter<ButtonViewHolder>() {

        override fun getItem(position: Int): AsymmetricItem {
            return when {
            // get normal item
                position < AppState.tempData.tempRemoteProfile.buttons.size -> {
                    val b = AppState.tempData.tempRemoteProfile.buttons[position]
                    DemoItem(
                        b.columnSpan,
                        b.rowSpan,
                        position
                    )
                }
            // get 'Add Button' item
                 else -> {
                     DemoItem(
                         2,
                         1,
                         position
                     )
                 }
            }
        }

        override fun getItemViewType(position: Int) : Int {
            return when {
                // get button view type
                position < AppState.tempData.tempRemoteProfile.buttons.size -> {
                    AppState.tempData.tempRemoteProfile.buttons[position].style.value
                }
                else -> {
                // get 'Add Button' view type
                    Button.Companion.ButtonStyle.STYLE_CREATE_BUTTON.value
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
            return ButtonViewHolder(
                parent,
                viewType
            )
        }

        override fun getItemCount() : Int {
            return AppState.tempData.tempRemoteProfile.buttons.size + if (AppState.tempData.tempRemoteProfile.inEditMode.get()) 1 else 0
        }

        override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
            holder.bind(position)
        }


    }

    class ButtonViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder(
        when (viewType) {
            Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND.value -> LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_base, parent, false)
            Button.Companion.ButtonStyle.STYLE_BTN_INCREMENTER_VERTICAL.value -> LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_inc_vert, parent, false)
            Button.Companion.ButtonStyle.STYLE_BTN_RADIAL_W_CENTER.value -> LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_radial_w_center_btn, parent, false)
            Button.Companion.ButtonStyle.STYLE_BTN_RADIAL.value -> LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_radial_w_center_btn, parent, false)
            Button.Companion.ButtonStyle.STYLE_CREATE_BUTTON.value -> LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_create_new, parent, false)

            else -> LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_base, parent, false)
        }
    ) {
        var button: Button? = null

        fun bind(position: Int) {
            if (position >= AppState.tempData.tempRemoteProfile.buttons.size) {
                bindCreateNewButton()
            } else {
                button = AppState.tempData.tempRemoteProfile.buttons[position]
                button?.let { b ->
                    when (b.style) {
                        Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND -> bindSingleActionButton(b)
                        Button.Companion.ButtonStyle.STYLE_BTN_INCREMENTER_VERTICAL -> bindIncrementerButton(b)
                        Button.Companion.ButtonStyle.STYLE_BTN_RADIAL_W_CENTER -> bindRadialButton(b, true)
                        Button.Companion.ButtonStyle.STYLE_BTN_RADIAL -> bindRadialButton(b, false)
                        else -> bindSingleActionButton(b)
                    }
                }
            }
        }

        private fun bindCreateNewButton() {
            val createButtonView = itemView.findViewById<ConstraintLayout>(R.id.btnRmtCreateNew)
            createButtonView.setOnClickListener {
                if (!AppState.tempData.tempRemoteProfile.isCreatingNewButton.get())
                    AppState.tempData.tempRemoteProfile.isCreatingNewButton.set(true)
            }
        }

        private fun bindRadialButton(button: Button, withCenterButton: Boolean) {
            val topButtonView = itemView.findViewById<RemoteButtonView>(R.id.btnTop)
            val bottomButtonView = itemView.findViewById<RemoteButtonView>(R.id.btnBottom)
            val startButtonView = itemView.findViewById<RemoteButtonView>(R.id.btnStart)
            val endButtonView = itemView.findViewById<RemoteButtonView>(R.id.btnEnd)
            val centerButtonView = itemView.findViewById<RemoteButtonView>(R.id.btnCenter)

            // set top button properties
            topButtonView.properties = button.properties[0]
            topButtonView.setOnClickListener {
                RealtimeDatabaseFunctions.sendCommandToHub(button.commands[0])
                Log.d("TEST", "TOP BUTTON CLICKED")
            }
            // set bottom button properties
            endButtonView.properties = button.properties[1]
            endButtonView.setOnClickListener {
                RealtimeDatabaseFunctions.sendCommandToHub(button.commands[1])
                Log.d("TEST", "END BUTTON CLICKED")
            }
            // set start button properties
            bottomButtonView.properties = button.properties[2]
            bottomButtonView.setOnClickListener {
                RealtimeDatabaseFunctions.sendCommandToHub(button.commands[2])
                Log.d("TEST", "BOTTOM BUTTON CLICKED")
            }
            // set end button properties
            startButtonView.properties = button.properties[3]
            startButtonView.setOnClickListener {
                RealtimeDatabaseFunctions.sendCommandToHub(button.commands[3])
                Log.d("TEST", "START BUTTON CLICKED")
            }

            if (withCenterButton) {
                // make sure button is visible and enabled
                centerButtonView.visibility = View.VISIBLE
                centerButtonView.isEnabled = true
                // set center button properties
                centerButtonView.properties = button.properties[4]
                centerButtonView.setOnClickListener {
                    RealtimeDatabaseFunctions.sendCommandToHub(button.commands[4])
                }
                // set center button text
                centerButtonView.buttonText = button.name
            } else {
                // make sure button is visible and enabled
                centerButtonView.visibility = View.INVISIBLE
                centerButtonView.isEnabled = false
            }
        }

        private fun bindIncrementerButton(button: Button) {
            val topButtonView = itemView.findViewById<RemoteButtonView>(R.id.btnTop)
            val bottomButtonView = itemView.findViewById<RemoteButtonView>(R.id.btnBottom)
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

        private fun bindSingleActionButton(button: Button) {
            val buttonView = itemView.findViewById<RemoteButtonView>(R.id.btnBackground)

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

    inner class TopSpaceDecoration : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
            super.getItemOffsets(outRect, view, parent, state)

            if (parent.getChildAdapterPosition(view) == 0)
                outRect.top = topPadding
        }
    }
}