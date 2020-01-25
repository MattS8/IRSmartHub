package com.ms8.smartirhub.android.remote_control.views

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AGVRecyclerViewAdapter
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview.AsymmetricItem
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.remote_control.button.creation.ButtonCreator
import com.ms8.smartirhub.android.remote_control.button.models.Button
import com.ms8.smartirhub.android.remote_control.button.views.RemoteButtonView
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile

class RemoteLayoutAdapter: AGVRecyclerViewAdapter<RemoteLayoutAdapter.ButtonViewHolder>() {
    var onButtonPressed: (buttonPosition: Int, command: RemoteProfile.Command?) -> Unit = { _: Int, _: RemoteProfile.Command? -> }

    override fun getItem(position: Int): AsymmetricItem {
        return when {
            // get normal item
            position < AppState.tempData.tempRemoteProfile.buttons.size -> {
                val b = AppState.tempData.tempRemoteProfile.buttons[position]
                RemoteLayoutView.DemoItem(
                    b.columnSpan,
                    b.rowSpan,
                    position
                )
            }
            // get 'Add Button' item
            else -> {
                RemoteLayoutView.DemoItem(
                    4,
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
                AppState.tempData.tempRemoteProfile.buttons[position].type.value
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

    inner class ButtonViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder(
        when (viewType) {
            Button.Companion.ButtonStyle.STYLE_BTN_NO_MARGIN.value -> {
                LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_base, parent, false)
            }
            Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND.value -> LayoutInflater.from(parent.context).inflate(
                R.layout.v_rmt_btn_base, parent, false)
            Button.Companion.ButtonStyle.STYLE_BTN_INCREMENTER_VERTICAL.value -> LayoutInflater.from(parent.context).inflate(
                R.layout.v_rmt_btn_inc_vert, parent, false)
            Button.Companion.ButtonStyle.STYLE_BTN_RADIAL_W_CENTER.value -> LayoutInflater.from(parent.context).inflate(
                R.layout.v_rmt_btn_radial, parent, false)
            Button.Companion.ButtonStyle.STYLE_BTN_RADIAL.value -> LayoutInflater.from(parent.context).inflate(
                R.layout.v_rmt_btn_radial, parent, false)
            Button.Companion.ButtonStyle.STYLE_CREATE_BUTTON.value -> LayoutInflater.from(parent.context).inflate(
                R.layout.v_rmt_btn_create_new, parent, false)
            Button.Companion.ButtonStyle.STYLE_SPACE.value -> FrameLayout(parent.context)

            else -> LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_base, parent, false)
        }
    ) {
        fun bind(position: Int) {
            Log.d("TEST", "Binding at position: $position")
            if (position >= AppState.tempData.tempRemoteProfile.buttons.size)
            {
                Log.d("TEST", "Binding create new...")
                bindCreateNewButton()
                return
            }

            AppState.tempData.tempRemoteProfile.buttons[position]?.let { button ->
                when (button.type) {
                    Button.Companion.ButtonStyle.STYLE_SPACE -> bindSpaceButton()
                    Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND -> bindSingleActionButton(button, position)
                    Button.Companion.ButtonStyle.STYLE_BTN_INCREMENTER_VERTICAL -> bindIncrementerButton(button, position)
                    Button.Companion.ButtonStyle.STYLE_BTN_RADIAL_W_CENTER -> bindRadialButton(button, true, position)
                    Button.Companion.ButtonStyle.STYLE_BTN_RADIAL -> bindRadialButton(button, false, position)
                    else -> bindSingleActionButton(button, position)
                }
            }
        }

        private fun bindSpaceButton() {
            //todo - Is there anything needed to make the space "button" look like it should?
        }

        private fun bindCreateNewButton() {
            val buttonView = itemView.findViewById<android.widget.Button>(R.id.btnRmtCreateNew)
            Log.d("TEST", "buttonView: $buttonView")
            buttonView.setOnClickListener {
                Log.d("TEST", "CLICKED")
                onButtonPressed(ButtonCreator.NEW_BUTTON, null)
            }
        }

        private fun bindRadialButton(
            button: Button,
            withCenterButton: Boolean,
            position: Int
        ) {
            val topButtonView = itemView.findViewById<RemoteButtonView>(R.id.btnTop)
            val bottomButtonView = itemView.findViewById<RemoteButtonView>(R.id.btnBottom)
            val startButtonView = itemView.findViewById<RemoteButtonView>(R.id.btnStart)
            val endButtonView = itemView.findViewById<RemoteButtonView>(R.id.btnEnd)
            val centerButtonView = itemView.findViewById<RemoteButtonView>(R.id.btnCenter)

            // set top button properties
            topButtonView.setupProperties(button.properties[0])
            topButtonView.setOnClickListener {
                onButtonPressed(position, button.commands[0])
                Log.d("TEST", "TOP BUTTON CLICKED")
            }
            // set bottom button properties
            endButtonView.setupProperties(button.properties[1])
            endButtonView.setOnClickListener {
                onButtonPressed(position, button.commands[1])
                Log.d("TEST", "END BUTTON CLICKED")
            }
            // set start button properties
            bottomButtonView.setupProperties(button.properties[2])
            bottomButtonView.setOnClickListener {
                onButtonPressed(position, button.commands[2])
                Log.d("TEST", "BOTTOM BUTTON CLICKED")
            }
            // set end button properties
            startButtonView.setupProperties(button.properties[3])
            startButtonView.setOnClickListener {
                onButtonPressed(position, button.commands[3])
                Log.d("TEST", "START BUTTON CLICKED")
            }

            if (withCenterButton) {
                // make sure button is visible and enabled
                centerButtonView.visibility = View.VISIBLE
                centerButtonView.isEnabled = true
                // set center button properties
                centerButtonView.setupProperties(button.properties[4])
                centerButtonView.setOnClickListener {
                    onButtonPressed(position, button.commands[4])
                    Log.d("TEST", "CENTER BUTTON CLICKED")
                }
            } else {
                // make sure button is visible and enabled
                centerButtonView.visibility = View.INVISIBLE
                centerButtonView.isEnabled = false
            }
        }

        private fun bindIncrementerButton(
            button: Button,
            position: Int
        ) {
            val topButtonView = itemView.findViewById<RemoteButtonView>(R.id.btnTop)
            val bottomButtonView = itemView.findViewById<RemoteButtonView>(R.id.btnBottom)
            val buttonText = itemView.findViewById<TextView>(R.id.txtButtonName)

            // set top button properties
            topButtonView.setupProperties(button.properties[0])
            topButtonView.setOnClickListener {
                onButtonPressed(position, button.commands[0])
                Log.d("TEST", "TOP BUTTON CLICKED")
            }
            // set bottom button properties
            bottomButtonView.setupProperties(button.properties[2])
            bottomButtonView.setOnClickListener {
                onButtonPressed(position, button.commands[2])
                Log.d("TEST", "BOTTOM BUTTON CLICKED")
            }
            // set middle textView text
            buttonText.text = button.properties[1].text
            // Backwards compatible autoSizeText
            TextViewCompat.setAutoSizeTextTypeWithDefaults(buttonText, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
        }

        private fun bindSingleActionButton(
            button: Button,
            position: Int
        ) {
            val buttonView = itemView.findViewById<RemoteButtonView>(R.id.btnBackground)

            // set button properties
            buttonView.setupProperties(button.properties[0])
            // set button text
            buttonView.setOnClickListener {
                onButtonPressed(position, button.commands[0])
            }
        }
    }
}