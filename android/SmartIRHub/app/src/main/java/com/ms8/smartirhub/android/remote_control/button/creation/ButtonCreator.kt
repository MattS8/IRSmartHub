package com.ms8.smartirhub.android.remote_control.button.creation

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Parcel
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.ms8.smartirhub.android.utils.extensions.hideKeyboard
import org.jetbrains.anko.layoutInflater
import java.lang.Exception

class ButtonCreator {
    /*
    ----------------------------------------------
        Public Listeners
    ----------------------------------------------
    */
    var onCreateDialogDismiss: (fromBackPressed: Boolean) -> Unit = {}
    var onCreateDialogShow: () -> Unit = {}
    var onCreationComplete: (completedButton: Button) -> Unit = {}
    var onRequestCommandFromRemote: (remote: RemoteProfile) -> Unit = {}
        set(value) {
            field = value
            commandCreator.requestedCommandFromRemoteListener = field
        }
    var onRequestActionsFromRemote: (remote: RemoteProfile) -> Unit = {}
        set(value) {
            field = value
            commandCreator.requestedActionsFromRemoteListener = field
        }

/*
----------------------------------------------
    Private Listeners
----------------------------------------------
*/

    private var onDismiss = {
        Log.d("t#", "buttonCreator - onDismiss (isTransitioning = $isTransitioning)")
        if (!isTransitioning) {
            AppState.tempData.tempRemoteProfile.isCreatingNewButton.set(false)
            onCreateDialogDismiss(isBackPressed)
            if (isBackPressed)
                isBackPressed = false
            createButtonDialog = null
            dialogState = ButtonDialogState.CHOOSE_TYPE
        }
        else
            isTransitioning = false
    }

    private var onCommandDialogDismissed: (fromBackPressed: Boolean) -> Unit = {fromBackPressed ->
        if (fromBackPressed) context?.let { showButtonSetupDialog(it) }
        else onCreateDialogDismiss(fromBackPressed)
    }

    private var onCommandCreated: () -> Unit = {
        context?.let { c ->
            // set new command to current editing button
            AppState.tempData.tempButton.get()?.let {tempButton ->
                AppState.tempData.tempCommand?.let { tempCommand ->
                    tempButton.commands[arrayPosition] = tempCommand
                    AppState.tempData.tempCommand = null
                }
            }

            dismissBottomDialog(true)
            showButtonSetupDialog(c)
        }
    }

    private val buttonNameKeyListener = { view : View, keyCode : Int, keyEvent : KeyEvent ->
        var ret = false
        Log.d("Test", "buttonNameKeyListener - keyCode = $keyCode | keyEvent = ${keyEvent.action} (ACTION_DOWN = ${KeyEvent.ACTION_DOWN})")
        if (keyEvent.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
            AppState.tempData.tempButton.get()?.name =  (view as EditText).text.toString()
            view.hideKeyboard()
            ret = true
        }
        ret
    }

/*
----------------------------------------------
    State Variables
----------------------------------------------
*/

    class State(val buttonDialogState: ButtonDialogState, val commandState: CommandCreator.State, val arrayPosition: Int)

    enum class ButtonDialogState(var value: Int) {CHOOSE_TYPE(1), SETUP_BUTTON(2),  SETUP_COMMAND(3)}
    private var dialogState : ButtonDialogState  = ButtonDialogState.CHOOSE_TYPE

    private var commandCreator : CommandCreator = CommandCreator()
        .apply {
            dialogDismissedListener = onCommandDialogDismissed
            commandCreatedListener = this@ButtonCreator.onCommandCreated
        }
    private var arrayPosition  : Int = 0

    private var createButtonDialog : BottomSheetDialog? = null

    private var isTransitioning : Boolean = false

    // not a true 'state' variable, but allows for onDismiss to denote when dismissing via onBackPressed or via swipe
    private var isBackPressed : Boolean = false

    var context : Context? = null
    set(value) {
        field = value
        commandCreator.context = field

        if (context == null){
            dismissBottomDialog(true)
        }
    }

/*
----------------------------------------------
    Public Accessors
----------------------------------------------
*/

    fun setState(state: State) {
        dialogState = state.buttonDialogState
        commandCreator.setState(state.commandState)
        arrayPosition = state.arrayPosition
    }

    fun getState() : State {
        return State(dialogState, commandCreator.getState(), arrayPosition)
    }

    @SuppressLint("LogNotTimber")
    fun showBottomDialog() {
        if (createButtonDialog != null) {
            Log.w("ButtonCreator", "showBottomDialog - called while createButtonDialog is not null!")
            dismissBottomDialog(true)
        }

        when (context) {
            null -> Log.e("ButtonCreator", "showBottomDialog - Context was not set")
            else ->
            {
                Log.d("TEST", "dialogState = $dialogState")
                when (dialogState) {
                    ButtonDialogState.CHOOSE_TYPE -> { showChooseButtonTypeDialog(context!!) }
                    ButtonDialogState.SETUP_BUTTON -> { showButtonSetupDialog(context!!) }
                    ButtonDialogState.SETUP_COMMAND -> { commandCreator.showBottomDialog(arrayPosition) }
                }
                onCreateDialogShow()
            }
        }
    }

/*
----------------------------------------------
    Display Functions
----------------------------------------------
*/

    private fun createDialogView(context: Context, bottomSheetView: View) {
        // using a custom onBackPressed to handle navigating the different stages of creation process
        createButtonDialog = object : BottomSheetDialog(context) {
            override fun onBackPressed() {
                this@ButtonCreator.onBackPressed()
            }
        }
        createButtonDialog?.setCancelable(false)
        createButtonDialog?.setContentView(bottomSheetView)
        createButtonDialog?.setOnDismissListener { onDismiss() }
        createButtonDialog?.show()
    }

    private fun createButtonClicked(button : Button, isMissingCommands : () -> Boolean) {
        Log.d("Test", "Checking if missing command... ${isMissingCommands()}")
        if (isMissingCommands()) {
            context?.let {
                AlertDialog.Builder(it)
                    .setTitle(it.getString(R.string.are_you_sure))
                    .setMessage(it.getString(R.string.button_setup_not_complete))
                    .setPositiveButton(R.string.create) { _: DialogInterface, _: Int -> createButton(button)}
                    .setNegativeButton(R.string.cancel) { i: DialogInterface, _: Int -> i.dismiss()}
                    .show()
            }
        } else {
            createButton(button)
        }
    }

    private fun showChooseButtonTypeDialog(context: Context) {
    // change state
        dialogState = ButtonDialogState.CHOOSE_TYPE

    // set up bottom sheet dialog
        // using a custom onBackPressed to handle navigating the different stages of creation process
        val buttonTypesView = context.layoutInflater.inflate(R.layout.v_button_types, null)
        val buttonTypeBinding = DataBindingUtil.bind<VButtonTypesBinding>(buttonTypesView)


    // set up list of button types
        buttonTypeBinding?.list?.adapter = ButtonTypeAdapter()
        buttonTypeBinding?.list?.layoutManager = GridLayoutManager(context, 2)

        createDialogView(context, buttonTypesView)
    }

    @SuppressLint("LogNotTimber")
    private fun showButtonSetupDialog(context: Context) {
        // change state
        dialogState = ButtonDialogState.SETUP_BUTTON

        val button = AppState.tempData.tempButton.get() ?: return

        when (button.type) {
        // Button Types requiring 1-action setup:
            Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND ->
            {
                val bottomSheetBinding = VCreateSheetBinding.inflate(context.layoutInflater, null, false)
                val buttonContainerBinding = VButtonSetupBinding.inflate(context.layoutInflater, bottomSheetBinding.createSheetContent, true)
                val roundButtonBinding = VRmtBtnSingleSetupBinding.inflate(context.layoutInflater, buttonContainerBinding?.buttonContainer, true)

                roundButtonBinding.etButtonLabel.setHint(R.string.button_name_hint)
                roundButtonBinding.btnRound.setOnClickListener { transitionToCommandDialog(0) }

                // setup outer-most view
                bottomSheetBinding?.tvTitle?.text = context.getString(R.string.button_setup_title)

                buttonContainerBinding.btnCreate.setOnClickListener {
                    AppState.tempData.tempButton.get()?.let { b ->
                        b.properties[0].text = roundButtonBinding.etButtonLabel.text.toString()
                        createButtonClicked(b) {b.commands[0].actions.size > 0}
                    }
                }

                createDialogView(context, bottomSheetBinding.root)
            }
            Button.Companion.ButtonStyle.STYLE_BTN_NO_MARGIN ->
            {
                val bottomSheetBinding = VCreateSheetBinding.inflate(context.layoutInflater, null, false)
                val buttonContainerBinding = VButtonSetupBinding.inflate(context.layoutInflater, bottomSheetBinding.createSheetContent, true)
                val fullButtonBinding = VRmtBtnFullSetupBinding.inflate(context.layoutInflater, buttonContainerBinding?.buttonContainer, true)

                fullButtonBinding.etButtonLabel.apply {
                    setText(AppState.tempData.tempButton.get()?.name)
                    setOnKeyListener(buttonNameKeyListener)
                }
                fullButtonBinding.btnFull.setOnClickListener { transitionToCommandDialog(0) }

                // setup outer-most view
                bottomSheetBinding?.tvTitle?.text = context.getString(R.string.button_setup_title)

                buttonContainerBinding.btnCreate.setOnClickListener {
                    AppState.tempData.tempButton.get()?.let { b ->
                        b.properties[0].text = fullButtonBinding.etButtonLabel.text.toString()
                        createButtonClicked(b) {b.commands[0].actions.size > 0}
                    }
                }

                createDialogView(context, bottomSheetBinding.root)
            }
        // Button Types requiring 2-action setup:
            Button.Companion.ButtonStyle.STYLE_BTN_INCREMENTER_VERTICAL ->
            {
                Log.d("#T", "Setting up Incrementer Vertical Setup...")
            // create bottom sheet
                val bottomSheetView = context.layoutInflater.inflate(R.layout.v_create_sheet, null)
                val bottomSheetBinding : VCreateSheetBinding? = DataBindingUtil.bind(bottomSheetView)

            // button setup view
                //val setupButtonView = context.layoutInflater.inflate(R.layout.v_button_setup, bottomSheetBinding?.createSheetContent)
                val buttonContainerBinding : VButtonSetupBinding? = DataBindingUtil.inflate(context.layoutInflater, R.layout.v_button_setup, bottomSheetBinding?.createSheetContent, true)

            // incrementer button setup (goes inside setup button container)
                //val incrementerButtonView = context.layoutInflater.inflate(R.layout.v_rmt_btn_inc_vert_setup, buttonContainerBinding?.buttonContainer)
                val incrementerBinding : VRmtBtnIncVertSetupBinding? = DataBindingUtil.inflate(context.layoutInflater, R.layout.v_rmt_btn_inc_vert_setup, buttonContainerBinding?.buttonContainer, true)

            // setup inner-most view

                incrementerBinding?.btnTop?.setupProperties(button.properties[0])
                incrementerBinding?.btnTop?.setOnClickListener { transitionToCommandDialog(0) }

                incrementerBinding?.btnBottom?.setupProperties(button.properties[2])
                incrementerBinding?.btnBottom?.setOnClickListener { transitionToCommandDialog(1) }

            // setup middle view
                buttonContainerBinding?.btnCreate?.setOnClickListener {
                    AppState.tempData.tempButton.get()?.let { b ->
                        incrementerBinding?.etTopLabel?.text?.toString()?.let { b.properties[0].text = it }
                        incrementerBinding?.txtButtonName?.editText?.text?.toString()?.let { b.properties[1].text = it }
                        incrementerBinding?.etBottomLabel?.text?.toString()?.let { b.properties[2].text = it }

                        createButtonClicked(b) {b.commands[0].actions.size > 0 && b.commands[1].actions.size > 0}
                    }
                }

            // setup outer-most view
                bottomSheetBinding?.tvTitle?.text = context.getString(R.string.button_setup_title)

            // set content view and show dialog
                createDialogView(context, bottomSheetView)
            }
        // Button Types requiring 4-action setup:
            Button.Companion.ButtonStyle.STYLE_BTN_RADIAL ->
            {
            // create bottom sheet
                val bottomSheetView = context.layoutInflater.inflate(R.layout.v_create_sheet, null)
                val bottomSheetBinding : VCreateSheetBinding? = DataBindingUtil.bind(bottomSheetView)

            // button setup view
                val buttonContainerBinding : VButtonSetupBinding? = DataBindingUtil.inflate(context.layoutInflater, R.layout.v_button_setup, bottomSheetBinding?.createSheetContent, true)


            // button setup (goes inside setup button container)
                val radialBinding : VRmtBtnRadialSetupBinding? = DataBindingUtil.inflate(context.layoutInflater, R.layout.v_rmt_btn_radial_setup, buttonContainerBinding?.buttonContainer, true)
                radialBinding?.etButtonLabel?.visibility = View.GONE
            // remove center button
                radialBinding?.btnCenter?.visibility = View.GONE

            // setup inner-most view
                radialBinding?.btnTop?.setupProperties(button.properties[0])
                radialBinding?.btnTop?.setOnClickListener { transitionToCommandDialog(0) }

                radialBinding?.btnEnd?.setupProperties(button.properties[1])
                radialBinding?.btnEnd?.setOnClickListener { transitionToCommandDialog(1) }

                radialBinding?.btnBottom?.setupProperties(button.properties[2])
                radialBinding?.btnBottom?.setOnClickListener { transitionToCommandDialog(2) }

                radialBinding?.btnStart?.setupProperties(button.properties[3])
                radialBinding?.btnStart?.setOnClickListener { transitionToCommandDialog(3) }

            // setup middle view
                buttonContainerBinding?.btnCreate?.setOnClickListener {
                    AppState.tempData.tempButton.get()?.let { b ->
                        createButtonClicked(b) {b.commands[0].actions.size > 0
                                && b.commands[1].actions.size > 0
                                && b.commands[2].actions.size > 0
                                && b.commands[3].actions.size > 0}
                    }
                }
            // setup outer-most view
                bottomSheetBinding?.tvTitle?.text = context.getString(R.string.button_setup_title)

            // set content view and show dialog
                createDialogView(context, bottomSheetView)
                Log.d("#T", "Now showing Incrementer Vertical Setup...")
            }
        // Button Types requiring 5-action setup:
            Button.Companion.ButtonStyle.STYLE_BTN_RADIAL_W_CENTER ->
            {
            // create bottom sheet
                val bottomSheetView = context.layoutInflater.inflate(R.layout.v_create_sheet, null)
                val bottomSheetBinding : VCreateSheetBinding? = DataBindingUtil.bind(bottomSheetView)

            // button setup view
                val buttonContainerBinding : VButtonSetupBinding? = DataBindingUtil.inflate(context.layoutInflater, R.layout.v_button_setup, bottomSheetBinding?.createSheetContent, true)

            // button setup (goes inside setup button container)
                val radialBinding : VRmtBtnRadialSetupBinding? = DataBindingUtil.inflate(context.layoutInflater, R.layout.v_rmt_btn_radial_setup, buttonContainerBinding?.buttonContainer, true)
                radialBinding?.etButtonLabel?.visibility = View.VISIBLE

            // setup inner-most view
                radialBinding?.btnTop?.setupProperties(button.properties[0])
                radialBinding?.btnTop?.setOnClickListener { transitionToCommandDialog(0) }

                radialBinding?.btnEnd?.setupProperties(button.properties[1])
                radialBinding?.btnEnd?.setOnClickListener { transitionToCommandDialog(1) }

                radialBinding?.btnBottom?.setupProperties(button.properties[2])
                radialBinding?.btnBottom?.setOnClickListener { transitionToCommandDialog(2) }

                radialBinding?.btnStart?.setupProperties(button.properties[3])
                radialBinding?.btnStart?.setOnClickListener { transitionToCommandDialog(3) }

                radialBinding?.btnCenter?.setupProperties(button.properties[4])
                radialBinding?.btnCenter?.setOnClickListener { transitionToCommandDialog(4) }

            // setup middle view
                buttonContainerBinding?.btnCreate?.setOnClickListener {
                    AppState.tempData.tempButton.get()?.let { b ->
                        radialBinding?.etButtonLabel?.text?.toString()?.let { b.properties[4].text = it }
                        createButtonClicked(b) {b.commands[0].actions.size > 0
                                && b.commands[1].actions.size > 0
                                && b.commands[2].actions.size > 0
                                && b.commands[3].actions.size > 0
                                && b.commands[4].actions.size > 0}
                    }
                }
            // setup outer-most view
                bottomSheetBinding?.tvTitle?.text = context.getString(R.string.button_setup_title)

            // set content view and show dialog
                createDialogView(context, bottomSheetView)
            }
        // Button Types requiring no setup:
            Button.Companion.ButtonStyle.STYLE_SPACE ->
            {
                // This is needed because, since no new dialog is created, the logic to set isTransitioning after dialog dismissal is not called
                isTransitioning = false
                createButton(Button(Button.Companion.ButtonStyle.STYLE_SPACE))
            }
        // Invalid button to setup:
            Button.Companion.ButtonStyle.STYLE_CREATE_BUTTON -> Log.e("ButtonCreator", "showButtonSetupDialog - Cannot create 'create button' button!")
        }
    }

    // -------- Helper functions --------

    private fun dismissBottomDialog(transitioning: Boolean = false) {
        Log.d("Test", "dismiss... ${createButtonDialog == null}")
        createButtonDialog?.let {
            Log.d("TEST", "DIALOG WASN'T NULL (transitionting = $transitioning)")
            isTransitioning = transitioning
            it.dismiss()
            createButtonDialog = null
        }
    }

    private fun transitionToCommandDialog(position: Int) {
        dismissBottomDialog(true)
        dialogState = ButtonDialogState.SETUP_COMMAND
        arrayPosition = position
        commandCreator.showBottomDialog(arrayPosition)
    }


    private fun createButton(button: Button) {
        val buttonPosition = AppState.tempData.tempRemoteProfile.newButtonPosition
        if (buttonPosition == NEW_BUTTON)
        {
            Log.d("TEST", "Creating new button...")
            // add the temp button to the current remote
            AppState.tempData.tempRemoteProfile.buttons.add(button)
        } else
        {
            Log.d("TEST", "Updating button $buttonPosition...")
            AppState.tempData.tempRemoteProfile.buttons[buttonPosition] = button
        }

        // call the onCreationComplete listener now
        onCreationComplete(button)

        // reset tempButton and change isCreatingNewButton to false
        AppState.tempData.tempButton.set(null)
        AppState.tempData.tempRemoteProfile.isCreatingNewButton.set(false)

        // reset link to button that was being edited/created - probably not needed, but will keep a consistent result if I mess up in calling "create/edit button"
        AppState.tempData.tempRemoteProfile.newButtonPosition = NEW_BUTTON

        dismissBottomDialog()
    }

    private fun onBackPressed() {
        Log.d("t#", "buttonCreator - onBackPressed! dialogState = $dialogState")
        when (dialogState) {
            ButtonDialogState.CHOOSE_TYPE ->
            {
                dismissBottomDialog()
            }
            ButtonDialogState.SETUP_BUTTON ->
            {
                dismissBottomDialog(true)
                context?.let { showChooseButtonTypeDialog(it) }
            }
            ButtonDialogState.SETUP_COMMAND ->
            {
                dismissBottomDialog(true)
                context?.let { showChooseButtonTypeDialog(it) }
            }
        }
    }

    fun onCommandFromRemote() {
        commandCreator.notifyCommandSelectedFromRemote()
    }

    fun onActionsFromRemote() {
        commandCreator.notifyActionsSelectedFromRemote()
    }

    companion object {
        const val NEW_BUTTON = -1

        private fun dialogStateFromInt(intVal : Int) = ButtonDialogState.values().associateBy(ButtonDialogState::value)[intVal]

        fun readStateFromParcel(parcel: Parcel) : State {
            return State(
                dialogStateFromInt(parcel.readInt()) ?: ButtonDialogState.CHOOSE_TYPE,
                CommandCreator.readStateFromParcel(parcel),
                parcel.readInt()
            )
        }

        fun writeToParcel(parcel: Parcel, state: State) {
            parcel.writeInt(state.buttonDialogState.value)
            CommandCreator.writeToParcel(parcel, state.commandState)
            parcel.writeInt(state.arrayPosition)
        }
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
            val buttonResource = Button.imageResourceFromStyle(buttonType)
            if (buttonResource != 0)
                Glide.with(buttonImage).load(buttonResource).into(buttonImage)
            holder.itemView.setOnClickListener {
                Log.d("TEST", "setting buttonType to $buttonType")
                AppState.tempData.tempButton.set(Button(buttonType))

                // transition to 'setup button dialog'
                dismissBottomDialog(true)
                showButtonSetupDialog(it.context)
            }
        }

        inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.v_item_button_type, null))
    }
}