package com.ms8.irsmarthub.remote_control.remote.views

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.ms8.irsmarthub.R
import com.ms8.irsmarthub.remote_control.button.models.Button
import com.ms8.irsmarthub.remote_control.button.views.RemoteButtonView
import com.ms8.irsmarthub.remote_control.command.models.Command
import com.ms8.irsmarthub.remote_control.remote.views.asymmetric_gridview.AGVRecyclerViewAdapter
import com.ms8.irsmarthub.remote_control.remote.views.asymmetric_gridview.AsymmetricItem
import org.jetbrains.anko.textColor

class RemoteLayoutAdapter: AGVRecyclerViewAdapter<RemoteLayoutAdapter.ButtonViewHolder>() {
    var buttons: ArrayList<Button> = ArrayList()
    set(value) {
        field = value
        // todo - change to more elegant solution
        //  check for difference and notify accordingly
        notifyDataSetChanged()
    }

    // Controls the state in which the adapter is in
    var inEditMode = false
    set(value) {
        field = value
        notifyDataSetChanged()
    }

    // Listener that is called whenever a button is pressed
    private var onButtonPressed: (buttonPosition: Int, command: Command?) -> Unit = { _: Int, _: Command? -> }

    fun setButtonPressedListener(listener: (buttonPosition: Int, command: Command?) -> Unit) {
        onButtonPressed = listener
    }

/*
----------------------------------------------
    Overridden Functions
----------------------------------------------
*/
    override fun getItem(position: Int): AsymmetricItem {
        return when {
            // get normal item
            position < buttons.size -> {
                val b = buttons[position]
                RemoteLayoutView.AsymItem(
                    b.columnSpan,
                    b.rowSpan,
                    position
                )
            }
            // get 'Add Button' item
            else -> {
                RemoteLayoutView.AsymItem(
                    4,
                    2,
                    position
                )
            }
        }
    }

    override fun getItemViewType(position: Int) : Int {
        return if (position < buttons.size)
            buttons[position].type.ordinal
        else
            Button.Companion.Type.STYLE_CREATE_BUTTON.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        return ButtonViewHolder(
            parent,
            viewType
        )
    }

    override fun getItemCount() : Int {
        return buttons.size + if (inEditMode) 1 else 0
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        holder.bind(position)
    }

/*
----------------------------------------------
    View Holder
----------------------------------------------
*/
    inner class ButtonViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder(
        when (viewType) {
            Button.Companion.Type.BASIC.ordinal -> LayoutInflater.from(parent.context).inflate(
                R.layout.v_rmt_btn_base, parent, false)
            Button.Companion.Type.STYLE_BTN_INCREMENTER_VERTICAL.ordinal -> LayoutInflater.from(parent.context).inflate(
                R.layout.v_rmt_btn_inc_vert, parent, false)
            Button.Companion.Type.STYLE_BTN_RADIAL_W_CENTER.ordinal -> LayoutInflater.from(parent.context).inflate(
                R.layout.v_rmt_btn_radial, parent, false)
            Button.Companion.Type.STYLE_BTN_RADIAL.ordinal -> LayoutInflater.from(parent.context).inflate(
                R.layout.v_rmt_btn_radial, parent, false)
            Button.Companion.Type.STYLE_CREATE_BUTTON.ordinal -> LayoutInflater.from(parent.context).inflate(
                R.layout.v_rmt_btn_create_new, parent, false)
            Button.Companion.Type.STYLE_SPACE.ordinal -> FrameLayout(parent.context)

            else -> LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_base, parent, false)
        }
    ) {
        fun bind(position: Int) {
            when (if (position >= buttons.size) Button.Companion.Type.STYLE_CREATE_BUTTON else buttons[position].type) {
                Button.Companion.Type.BASIC -> bindSingleActionButton(buttons[position], position)
                Button.Companion.Type.STYLE_BTN_INCREMENTER_VERTICAL -> bindIncrementerButton(buttons[position], position)
                Button.Companion.Type.STYLE_SPACE -> bindSpaceButton()
                Button.Companion.Type.STYLE_BTN_RADIAL -> bindRadialButton(buttons[position], false, position)
                Button.Companion.Type.STYLE_BTN_RADIAL_W_CENTER -> bindRadialButton(buttons[position], true, position)
                Button.Companion.Type.STYLE_CREATE_BUTTON -> bindCreateNewButton()
            }
        }

        private fun bindCreateNewButton() {
            itemView.findViewById<android.widget.Button>(R.id.btnRmtCreateNew)
                .setOnClickListener {
                    Log.d("TEST", "CLICKED")
                    onButtonPressed(NEW_BUTTON, null)
                }
        }

        private fun bindRadialButton(
            button: Button,
            withCenterButton: Boolean,
            position: Int
        ) {
            val views: ArrayList<RemoteButtonView> = arrayListOf(
                itemView.findViewById(R.id.btnTop),
                itemView.findViewById(R.id.btnBottom),
                itemView.findViewById(R.id.btnStart),
                itemView.findViewById(R.id.btnEnd),
                itemView.findViewById(R.id.btnCenter)
                )

            views.forEachIndexed { index, buttonView ->
                if (index == 4) {
                    buttonView.apply {
                        isEnabled = withCenterButton
                        visibility = if (withCenterButton) View.VISIBLE else View.INVISIBLE
                    }
                } else {
                    buttonView.apply {
                        setupProperties(button.properties[index])
                        setOnClickListener { onButtonPressed(position, button.commands[index]) }
                    }
                }
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
            topButtonView.apply {
                setupProperties(button.properties[0])
                setOnClickListener { onButtonPressed(position, button.commands[0]) }
            }

            // set bottom button properties
            bottomButtonView.apply {
                setupProperties(button.properties[2])
                setOnClickListener { onButtonPressed(position, button.commands[2]) }
            }

            // set middle textView text
            buttonText.apply {
                text = button.properties[1].text
                // Backwards compatible autoSizeText
                TextViewCompat.setAutoSizeTextTypeWithDefaults(this, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
            }
        }

        private fun bindSingleActionButton(
            button: Button,
            position: Int
        ) {
            val buttonView = itemView.findViewById<RemoteButtonView>(R.id.btnBackground)

            // set button properties
            buttonView.setupProperties(button.properties[0])
            // set button text
            buttonView.setOnClickListener { onButtonPressed(position, button.commands[0]) }
        }

        private fun bindSpaceButton() {
            //todo - Is there anything needed to make the space "button" look like it should?
        }
    }

/*
----------------------------------------------
    Companion Object
----------------------------------------------
*/
    companion object {
        const val NEW_BUTTON = -1
    }
}