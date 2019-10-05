package com.ms8.smartirhub.android.remote_control.button.creation

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.*
import com.ms8.smartirhub.android.remote_control.button.models.Button
import com.ms8.smartirhub.android.remote_control.command.creation.CommandCreator
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import org.jetbrains.anko.layoutInflater
import java.lang.Exception

class ButtonCreator {
/*
----------------------------------------------
    Public Listeners
----------------------------------------------
*/
    var onCreateDialogDismiss: () -> Unit = {}
    var onCreateDialogShow: () -> Unit = {}
    var onCreationComplete: (completedButton: Button) -> Unit = {}
    var onRequestCommandFromRemote: (remote: RemoteProfile) -> Unit = {}
        set(value) {
            field = value
            commandCreator.onRequestCommandFromRemote = field
        }

/*
----------------------------------------------
    Private Listeners
----------------------------------------------
*/

    private var onDismiss = {
        if (!isTransitioning) {
            dialogState = ButtonDialogState.CHOOSE_TYPE
            onCreateDialogDismiss()

            // todo check if screen rotation causes onDismiss to be called. If so, this will remove tempData when we don't want to
            AppState.tempData.tempButton.set(null)
        }
    }

/*
----------------------------------------------
    State Variables
----------------------------------------------
*/

    enum class ButtonDialogState(var value: Int) {CHOOSE_TYPE(1), SETUP_BUTTON(2),  COMMAND_FROM(3), NEW_COMMAND(4)}
    var dialogState : ButtonDialogState  = ButtonDialogState.CHOOSE_TYPE
    set(value) {
        field = value

        // we want to keep the internal CommandCreator object in sync with what this ButtonCreator is doing
        if (field == ButtonDialogState.COMMAND_FROM) {
            commandCreator.dialogState = CommandCreator.CommandDialogState.COMMAND_FROM
        } else if (field == ButtonDialogState.NEW_COMMAND) {
            commandCreator.dialogState = CommandCreator.CommandDialogState.NEW_COMMAND
        }
    }

    var commandCreator : CommandCreator = CommandCreator()
    var arrayPosition  : Int            = 0

    var createButtonDialog : BottomSheetDialog? = null

    private var isTransitioning : Boolean = false

/*
----------------------------------------------
    Public Accessors
----------------------------------------------
*/

    fun showBottomDialog(context: Context) {
        when (dialogState) {
            ButtonDialogState.CHOOSE_TYPE -> { showChooseButtonTypeDialog(context) }
            ButtonDialogState.SETUP_BUTTON -> { showButtonSetupDialog(context) }
            ButtonDialogState.COMMAND_FROM,
            ButtonDialogState.NEW_COMMAND -> { showCommandFromDialog(context, 1) } //todo set position from saved variable
        }
        onCreateDialogShow()
    }

    fun dismissBottomDialog() {
        createButtonDialog?.dismiss()
        createButtonDialog = null

        // note: onCreateDialogDismiss() callback is called via the setOnDismissListener
    }

/*
----------------------------------------------
    Display Functions
----------------------------------------------
*/

    private fun showChooseButtonTypeDialog(context: Context) {
    // change state
        dialogState = ButtonDialogState.CHOOSE_TYPE

    // set up bottom sheet dialog
        val buttonTypesView = context.layoutInflater.inflate(R.layout.v_button_types, null)
        createButtonDialog = BottomSheetDialog(context)
        val buttonTypeBinding = DataBindingUtil.bind<VButtonTypesBinding>(buttonTypesView)
        createButtonDialog?.setContentView(buttonTypesView)
        createButtonDialog?.setOnDismissListener { onDismiss() }

    // set up list of button types
        buttonTypeBinding?.list?.adapter = ButtonTypeAdapter()
        buttonTypeBinding?.list?.layoutManager = GridLayoutManager(context, 2)

        createButtonDialog?.show()
    }

    @SuppressLint("LogNotTimber")
    private fun showButtonSetupDialog(context: Context) {
        // change state
        dialogState = ButtonDialogState.SETUP_BUTTON

        val button = AppState.tempData.tempButton.get() ?: return

        when (button.type) {
        // Button Types requiring 1-action setup:
            Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND,
            Button.Companion.ButtonStyle.STYLE_BTN_NO_MARGIN ->
            {
                isTransitioning = true
                dismissBottomDialog()
                dialogState = ButtonDialogState.COMMAND_FROM
                showCommandFromDialog(context, 0)
                isTransitioning = false
            }
        // Button Types requiring 2-action setup:
            Button.Companion.ButtonStyle.STYLE_BTN_INCREMENTER_VERTICAL ->
            {
            // create bottom sheet
                val bottomSheetView = context.layoutInflater.inflate(R.layout.v_create_sheet, null)
                val bottomSheetBinding : VCreateSheetBinding? = DataBindingUtil.bind(bottomSheetView)

            // button setup view
                val setupButtonView = context.layoutInflater.inflate(R.layout.v_button_setup, bottomSheetBinding?.createSheetContent)
                val buttonContainerBinding : VButtonSetupBinding? = DataBindingUtil.bind(setupButtonView)

            // incrementer button setup (goes inside setup button container)
                val incrementerButtonView = context.layoutInflater.inflate(R.layout.v_rmt_btn_inc_vert_setup, buttonContainerBinding?.buttonContainer)
                val incrementerBinding : VRmtBtnIncVertSetupBinding? = DataBindingUtil.bind(incrementerButtonView)

            // add views to their respective containers
            // the generic bottom 'create' sheet contains the 'setupButtonView' which contains the 'incrementerButtonView'
                buttonContainerBinding?.buttonContainer?.addView(incrementerButtonView)
                bottomSheetBinding?.createSheetContent?.addView(setupButtonView)

            // setup inner-most view
                incrementerBinding?.txtButtonName?.hint = context.getString(R.string.button_name_hint)

                incrementerBinding?.btnTop?.setupProperties(button.properties[0])
                incrementerBinding?.btnTop?.setOnClickListener { transitionToCommandFromDialog(it.context, 0) }

                incrementerBinding?.btnBottom?.setupProperties(button.properties[1])
                incrementerBinding?.btnBottom?.setOnClickListener { transitionToCommandFromDialog(it.context, 1) }

            // setup middle view
                buttonContainerBinding?.btnCreate?.setOnClickListener {
                    AppState.tempData.tempButton.get()?.let { b ->
                        if (b.commands[0].actions.size > 0 && b.commands[1].actions.size > 0) {
                            createButton(b)
                        }
                        else {
                            AlertDialog.Builder(it.context)
                                .setTitle(it.context.getString(R.string.are_you_sure))
                                .setMessage(it.context.getString(R.string.button_setup_not_complete))
                                .setPositiveButton(R.string.create) { _: DialogInterface, _: Int -> createButton(b)}
                                .setNegativeButton(R.string.cancel) { i: DialogInterface, _: Int -> i.dismiss()}
                                .show()
                        }
                    }
                }

            // setup outer-most view
                bottomSheetBinding?.tvTitle?.text = context.getString(R.string.button_setup_title)

            // set content view and show dialog
                createButtonDialog = BottomSheetDialog(context)
                createButtonDialog?.setContentView(bottomSheetView)
                createButtonDialog?.setOnDismissListener { onDismiss() }
                createButtonDialog?.show()
            }
        // Button Types requiring 4-action setup:
            Button.Companion.ButtonStyle.STYLE_BTN_RADIAL ->
            {
            // create bottom sheet
                val bottomSheetView = context.layoutInflater.inflate(R.layout.v_create_sheet, null)
                val bottomSheetBinding : VCreateSheetBinding? = DataBindingUtil.bind(bottomSheetView)

            // button setup view
                val setupButtonView = context.layoutInflater.inflate(R.layout.v_button_setup, bottomSheetBinding?.createSheetContent)
                val buttonContainerBinding : VButtonSetupBinding? = DataBindingUtil.bind(setupButtonView)

            // button setup (goes inside setup button container)
                val radialButtonView = context.layoutInflater.inflate(R.layout.v_rmt_btn_radial_setup, buttonContainerBinding?.buttonContainer)
                val radialBinding : VRmtBtnRadialSetupBinding? = DataBindingUtil.bind(radialButtonView)

            // add views to their respective containers
            // The generic bottom 'create' sheet contains the 'setupButtonView' which contains the 'incrementerButtonView'
                buttonContainerBinding?.buttonContainer?.addView(radialButtonView)
                bottomSheetBinding?.createSheetContent?.addView(setupButtonView)

            // remove center button
                radialBinding?.btnCenter?.visibility = View.GONE

            // setup inner-most view
                radialBinding?.btnTop?.setupProperties(button.properties[0])
                radialBinding?.btnTop?.setOnClickListener { transitionToCommandFromDialog(it.context, 0) }

                radialBinding?.btnEnd?.setupProperties(button.properties[1])
                radialBinding?.btnEnd?.setOnClickListener { transitionToCommandFromDialog(it.context, 1) }

                radialBinding?.btnBottom?.setupProperties(button.properties[2])
                radialBinding?.btnBottom?.setOnClickListener { transitionToCommandFromDialog(it.context, 2) }

                radialBinding?.btnStart?.setupProperties(button.properties[3])
                radialBinding?.btnStart?.setOnClickListener { transitionToCommandFromDialog(it.context, 3) }

            // setup middle view
                buttonContainerBinding?.btnCreate?.setOnClickListener {
                    AppState.tempData.tempButton.get()?.let { b ->
                        if (b.commands[0].actions.size > 0
                            && b.commands[1].actions.size > 0
                            && b.commands[2].actions.size > 0
                            && b.commands[3].actions.size > 0
                        ) {
                            createButton(b)
                        }
                        else {
                            AlertDialog.Builder(it.context)
                                .setTitle(it.context.getString(R.string.are_you_sure))
                                .setMessage(it.context.getString(R.string.button_setup_not_complete))
                                .setPositiveButton(R.string.create) { _: DialogInterface, _: Int -> createButton(b)}
                                .setNegativeButton(R.string.cancel) { i: DialogInterface, _: Int -> i.dismiss()}
                                .show()
                        }
                    }
                }
            // setup outer-most view
                bottomSheetBinding?.tvTitle?.text = context.getString(R.string.button_setup_title)

            // set content view and show dialog
                createButtonDialog = BottomSheetDialog(context)
                createButtonDialog?.setContentView(bottomSheetView)
                createButtonDialog?.setOnDismissListener { onDismiss() }
                createButtonDialog?.show()
            }
        // Button Types requiring 5-action setup:
            Button.Companion.ButtonStyle.STYLE_BTN_RADIAL_W_CENTER ->
            {
            // create bottom sheet
                val bottomSheetView = context.layoutInflater.inflate(R.layout.v_create_sheet, null)
                val bottomSheetBinding : VCreateSheetBinding? = DataBindingUtil.bind(bottomSheetView)

            // button setup view
                val setupButtonView = context.layoutInflater.inflate(R.layout.v_button_setup, bottomSheetBinding?.createSheetContent)
                val buttonContainerBinding : VButtonSetupBinding? = DataBindingUtil.bind(setupButtonView)

            // button setup (goes inside setup button container)
                val radialButtonView = context.layoutInflater.inflate(R.layout.v_rmt_btn_radial_setup, buttonContainerBinding?.buttonContainer)
                val radialBinding : VRmtBtnRadialSetupBinding? = DataBindingUtil.bind(radialButtonView)

            // add views to their respective containers
            // The generic bottom 'create' sheet contains the 'setupButtonView' which contains the 'incrementerButtonView'
                buttonContainerBinding?.buttonContainer?.addView(radialButtonView)
                bottomSheetBinding?.createSheetContent?.addView(setupButtonView)

            // setup inner-most view
                radialBinding?.btnTop?.setupProperties(button.properties[0])
                radialBinding?.btnTop?.setOnClickListener { transitionToCommandFromDialog(it.context, 0) }

                radialBinding?.btnEnd?.setupProperties(button.properties[1])
                radialBinding?.btnEnd?.setOnClickListener { transitionToCommandFromDialog(it.context, 1) }

                radialBinding?.btnBottom?.setupProperties(button.properties[2])
                radialBinding?.btnBottom?.setOnClickListener { transitionToCommandFromDialog(it.context, 2) }

                radialBinding?.btnStart?.setupProperties(button.properties[3])
                radialBinding?.btnStart?.setOnClickListener { transitionToCommandFromDialog(it.context, 3) }

                radialBinding?.btnCenter?.setupProperties(button.properties[4])
                radialBinding?.btnCenter?.setOnClickListener { transitionToCommandFromDialog(it.context, 4) }

            // setup middle view
                buttonContainerBinding?.btnCreate?.setOnClickListener {
                    AppState.tempData.tempButton.get()?.let { b ->
                        if (b.commands[0].actions.size > 0
                            && b.commands[1].actions.size > 0
                            && b.commands[2].actions.size > 0
                            && b.commands[3].actions.size > 0
                            && b.commands[4].actions.size > 0
                        ) {
                            createButton(b)
                        }
                        else {
                            AlertDialog.Builder(it.context)
                                .setTitle(it.context.getString(R.string.are_you_sure))
                                .setMessage(it.context.getString(R.string.button_setup_not_complete))
                                .setPositiveButton(R.string.create) { _: DialogInterface, _: Int -> createButton(b)}
                                .setNegativeButton(R.string.cancel) { i: DialogInterface, _: Int -> i.dismiss()}
                                .show()
                        }
                    }
                }
            // setup outer-most view
                bottomSheetBinding?.tvTitle?.text = context.getString(R.string.button_setup_title)

            // set content view and show dialog
                createButtonDialog = BottomSheetDialog(context)
                createButtonDialog?.setContentView(bottomSheetView)
                createButtonDialog?.setOnDismissListener { onDismiss() }
                createButtonDialog?.show()
            }
        // Button Types requiring no setup:
            Button.Companion.ButtonStyle.STYLE_SPACE ->
            {
                createButton(Button(Button.Companion.ButtonStyle.STYLE_SPACE))
            }
        // Invalid button to setup:
            Button.Companion.ButtonStyle.STYLE_CREATE_BUTTON -> Log.e("ButtonCreator", "showButtonSetupDialog - Cannot create 'create button' button!")
        }
    }

    private fun showCommandFromDialog(context: Context, position: Int) {
    //change state
        dialogState = ButtonDialogState.COMMAND_FROM

    // show command creator
        commandCreator.showBottomDialog(context, position)
    }

    // -------- Helper functions --------

    private fun getOwnerString(context: Context, name: String): CharSequence? {
        return if (name == AppState.userData.user.username.get())
            "${context.getString(R.string.remote_owner)} ${context.getString(R.string.you)}"
        else
            "${context.getString(R.string.remote_owner)} $name"
    }

    private fun transitionToCommandFromDialog(context: Context, position: Int) {
        isTransitioning = true
        dismissBottomDialog()
        showCommandFromDialog(context, position)
        isTransitioning = false
    }


    private fun createButton(button: Button) {
        // add the temp button to the current remote
        AppState.tempData.tempRemoteProfile.buttons.add(button)

        // call the onCreationComplete listener now
        onCreationComplete(button)

        // dismissing the creation window will automatically reset tempButton
        dismissBottomDialog()
    }

    companion object {
        fun stateFromIntVal(intVal : Int) = ButtonDialogState.values().associateBy(ButtonDialogState::value)[intVal]
    }

/*
----------------------------------------------
    Adapters
----------------------------------------------
*/

    inner class ButtonTypeAdapter : RecyclerView.Adapter<ButtonTypeAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(parent)
        }

        override fun getItemCount(): Int {
            return Button.Companion.ButtonStyle.values().size - 1
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val buttonImage =  holder.itemView.findViewById<ImageView>(R.id.btnImage)
            val buttonTitle = holder.itemView.findViewById<TextView>(R.id.tvButtonType)
            val buttonType = Button.buttonStyleFromInt(position) ?: throw Exception("Unknown button type ($position)")

            buttonTitle.text = Button.nameFromStyle(holder.itemView.context, buttonType)
            Glide.with(buttonImage).load(Button.imageResourceFromStyle(buttonType)).into(buttonImage)
            holder.itemView.setOnClickListener {
                AppState.tempData.tempButton.get()?.type = buttonType

                // transition to 'setup button dialog'
                isTransitioning = true
                dismissBottomDialog()
                showButtonSetupDialog(it.context)
                isTransitioning = false
            }
        }

        inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.v_item_button_type, null))
    }

}