package com.ms8.smartirhub.android.remote_control.button.creation

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Parcel
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
            onCreateDialogDismiss(isBackPressed)
            if (isBackPressed)
                isBackPressed = false
        }
        else
            isTransitioning = false
    }

    private var onCommandDialogDismissed: (fromBackPressed: Boolean) -> Unit = {fromBackPressed ->
        if (fromBackPressed) this@ButtonCreator.onBackPressed()
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

            createButtonDialog?.let {
                isTransitioning = true
                dismissBottomDialog()
            }
            showButtonSetupDialog(c)
        }
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
            isTransitioning = true
            dismissBottomDialog()
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
        if (createButtonDialog != null)
            Log.w("ButtonCreator", "showBottomDialog - called while createButtonDialog is not null!")

        when (context) {
            null -> Log.e("ButtonCreator", "showBottomDialog - Context was not set")
            else ->
            {
                when (dialogState) {
                    ButtonDialogState.CHOOSE_TYPE -> { showChooseButtonTypeDialog(context!!) }
                    ButtonDialogState.SETUP_BUTTON -> { showButtonSetupDialog(context!!) }
                    ButtonDialogState.SETUP_COMMAND -> { commandCreator.showBottomDialog(arrayPosition) }
                }
                onCreateDialogShow()
            }
        }
    }

    fun dismissBottomDialog() {
        createButtonDialog?.dismiss()
        createButtonDialog = null
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
        // using a custom onBackPressed to handle navigating the different stages of creation process
        val buttonTypesView = context.layoutInflater.inflate(R.layout.v_button_types, null)
        createButtonDialog = object : BottomSheetDialog(context) {
            override fun onBackPressed() {
                this@ButtonCreator.onBackPressed()
            }
        }
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
                transitionToCommandDialog(0)
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
                incrementerBinding?.txtButtonName?.hint = context.getString(R.string.button_name_hint)

                incrementerBinding?.btnTop?.setupProperties(button.properties[0])
                incrementerBinding?.btnTop?.setOnClickListener { transitionToCommandDialog(0) }

                incrementerBinding?.btnBottom?.setupProperties(button.properties[1])
                incrementerBinding?.btnBottom?.setOnClickListener { transitionToCommandDialog(1) }

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
                // using a custom onBackPressed to handle navigating the different stages of creation process
                createButtonDialog = object : BottomSheetDialog(context) {
                    override fun onBackPressed() {
                        this@ButtonCreator.onBackPressed()
                    }
                }
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
                val buttonContainerBinding : VButtonSetupBinding? = DataBindingUtil.inflate(context.layoutInflater, R.layout.v_button_setup, bottomSheetBinding?.createSheetContent, true)


            // button setup (goes inside setup button container)
                val radialBinding : VRmtBtnRadialSetupBinding? = DataBindingUtil.inflate(context.layoutInflater, R.layout.v_rmt_btn_radial_setup, buttonContainerBinding?.buttonContainer, true)

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
                // using a custom onBackPressed to handle navigating the different stages of creation process
                createButtonDialog = object : BottomSheetDialog(context) {
                    override fun onBackPressed() {
                        this@ButtonCreator.onBackPressed()
                    }
                }
                createButtonDialog?.setContentView(bottomSheetView)
                createButtonDialog?.setOnDismissListener { onDismiss() }
                createButtonDialog?.show()

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
                // using a custom onBackPressed to handle navigating the different stages of creation process
                createButtonDialog = object : BottomSheetDialog(context) {
                    override fun onBackPressed() {
                        this@ButtonCreator.onBackPressed()
                    }
                }
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

    // -------- Helper functions --------

    private fun transitionToCommandDialog(position: Int) {
        isTransitioning = true
        dismissBottomDialog()
        dialogState = ButtonDialogState.SETUP_COMMAND
        arrayPosition = position
        commandCreator.showBottomDialog(arrayPosition)
    }


    private fun createButton(button: Button) {
        // add the temp button to the current remote
        AppState.tempData.tempRemoteProfile.buttons.add(button)

        // call the onCreationComplete listener now
        onCreationComplete(button)

        // reset tempButton and change isCreatingNewButton to false
        AppState.tempData.tempButton.set(null)
        AppState.tempData.isCreatingNewButton.set(false)

        dismissBottomDialog()
    }

    private fun onBackPressed() {
        Log.d("t#", "buttonCreator - onBackPressed! context = $context")
        when (dialogState) {
            ButtonDialogState.CHOOSE_TYPE ->
            {
                isBackPressed = true
                dismissBottomDialog()
            }
            ButtonDialogState.SETUP_BUTTON ->
            {
                isTransitioning = true
                dismissBottomDialog()
                context?.let { showChooseButtonTypeDialog(it) }
            }
            ButtonDialogState.SETUP_COMMAND ->
            {
                context?.let { showButtonSetupDialog(it) }
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
            Glide.with(buttonImage).load(Button.imageResourceFromStyle(buttonType)).into(buttonImage)
            holder.itemView.setOnClickListener {
                AppState.tempData.tempButton.set(Button(buttonType))

                // transition to 'setup button dialog'
                isTransitioning = true
                dismissBottomDialog()
                showButtonSetupDialog(it.context)
            }
        }

        inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.v_item_button_type, null))
    }

}