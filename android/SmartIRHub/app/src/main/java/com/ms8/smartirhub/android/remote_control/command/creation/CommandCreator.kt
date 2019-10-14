package com.ms8.smartirhub.android.remote_control.command.creation

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.util.Log
import android.view.View
import android.widget.*
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.VCreateSheetBinding
import com.ms8.smartirhub.android.databinding.VFromRemoteListBinding
import com.ms8.smartirhub.android.databinding.VPairInstructionsBinding
import com.ms8.smartirhub.android.models.firestore.Hub
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import org.jetbrains.anko.layoutInflater

class CommandCreator {

/*
----------------------------------------------
    Public Listeners
----------------------------------------------
*/
    var onRequestCommandFromRemote: (remote: RemoteProfile) -> Unit = {}
    var onDialogDismiss : (fromBackPress: Boolean) -> Unit = {}
    var onNewCommandSelected : () -> Unit = {}
    var onCommandFromSelected : () -> Unit = {}

/*
----------------------------------------------
    Private Listeners
----------------------------------------------
*/

    private fun onDismiss() {
        if (!isTransitioning) {
            Log.d("t#", "dismissing commandCreator (isBackPressed = $isBackPressed)")
            onDialogDismiss(isBackPressed)
            if (isBackPressed)
                isBackPressed = false
        }
        else
            isTransitioning = false
    }

/*
----------------------------------------------
    State Variables
----------------------------------------------
*/
    class State(val commandDialogState: CommandDialogState, val arrayPosition: Int, val selectedHubUID: String?)

    enum class CommandDialogState(var value: Int) { COMMAND_FROM(0), NEW_COMMAND(1), PAIR_SIGNAL(2) }
    private var dialogState = CommandDialogState.COMMAND_FROM
    private var arrayPosition = 0
    private var selectedHub : Hub? = null

    private var createCommandDialog : BottomSheetDialog? = null
    private var isTransitioning = false

    // not a true 'state' variable, but allows for onDismiss to denote when dismissing via onBackPressed or via swipe
    private var isBackPressed = false

/*
----------------------------------------------
    Public Accessors
----------------------------------------------
*/

    var context : Context? = null

    fun setState(state: State) {
        dialogState = state.commandDialogState
        arrayPosition = state.arrayPosition
        selectedHub = AppState.userData.hubs[state.selectedHubUID]
    }

    fun getState() : State {
        return State(dialogState, arrayPosition, selectedHub?.uid)
    }

    @SuppressLint("LogNotTimber")
    fun showBottomDialog(arrayPosition: Int) {
        this.arrayPosition = arrayPosition

        when (context) {
            null -> Log.e("CommandCreator", "showBottomDialog - context not set!")
            else ->
            {
                when (dialogState) {
                    CommandDialogState.COMMAND_FROM -> showCommandFromDialog(context!!)
                    CommandDialogState.NEW_COMMAND -> showNewCommandDialog(context!!)
                    CommandDialogState.PAIR_SIGNAL -> showPairSignalDialog(context!!)
                }
            }
        }
    }

    fun onBackPressed() {
        Log.d("t#", "commandCreator - onBackPressed (context = $context)")
        when (dialogState) {
            CommandDialogState.COMMAND_FROM ->
            {
                Log.d("t#", "command from")
                createCommandDialog?.let {
                    isBackPressed = true
                    it.dismiss()
                }
            }
            CommandDialogState.NEW_COMMAND ->
            {
                Log.d("t#", "new command")
                createCommandDialog?.let {
                    isTransitioning = true
                    it.dismiss()
                }

                // discard any temp command in the making
                AppState.tempData.tempCommand = null

                showCommandFromDialog(context!!)
            }
            CommandDialogState.PAIR_SIGNAL ->
            {
                if (AppState.tempData.tempCommand?.actions?.size ?: 0 > 0) {
                    createCommandDialog?.let {
                        isTransitioning = true
                        it.dismiss()
                    }
                    showCommandFromDialog(context!!)
                } else {
                    createCommandDialog?.let {
                        isTransitioning = true
                        it.dismiss()
                    }
                    showNewCommandDialog(context!!)
                }
            }
        }
    }

/*
----------------------------------------------
    Display Functions
----------------------------------------------
*/

    private fun showCommandFromDialog(context: Context) {
        // change state
        dialogState = CommandDialogState.COMMAND_FROM

        // create bottom sheet
        val bottomSheetView = context.layoutInflater.inflate(R.layout.v_create_sheet, null)
        val bottomSheetBinding : VCreateSheetBinding? = DataBindingUtil.bind(bottomSheetView)

        // create commands list
        val commandFromListBinding : VFromRemoteListBinding = DataBindingUtil.inflate(context.layoutInflater, R.layout.v_from_remote_list, bottomSheetBinding?.createSheetContent,true)

        // set up bottom sheet
        bottomSheetBinding?.tvTitle?.text = context.getText(R.string.choose_button_command_from)

        // set up list of commands

        // first we create a group of all user remotes
        val fromUserRemotesItem = commandFromListBinding.expandingList.createNewItem(R.layout.v_from_remote_expanding_item)
        //  - set user icon and color
        fromUserRemotesItem.setIndicatorIconRes(R.drawable.ic_my_remotes_icon)
        fromUserRemotesItem.setIndicatorColorRes(R.color.colorPrimary)
        // - set title
        fromUserRemotesItem.findViewById<TextView>(R.id.headerTitle).text = context.getString(R.string.from_user_remotes)
        // - create one sub item for each remote the user has access to
        fromUserRemotesItem.createSubItems(AppState.userData.remotes.size)
        AppState.userData.remotes.values.forEachIndexed { index, remote ->
            val subItemView = fromUserRemotesItem.getSubItemView(index)
            subItemView.findViewById<TextView>(R.id.tvTitle).text = remote.name
            subItemView.findViewById<TextView>(R.id.tvSubtitle).text = getOwnerString(subItemView.context, remote.name)
            // callback is used to start activity for result (need to show a remote view where user can click on a button to get that command)
            subItemView.setOnClickListener { onRequestCommandFromRemote(remote) }
        }

        // then we list template remotes, allowing a user to pick a generic command from generic remotes - todo

        // finally we allow the user to create a brand new command
        val newCommand = commandFromListBinding.expandingList.createNewItem(R.layout.v_from_remote_expanding_item)
        // - set new command icon
        newCommand.setIndicatorIconRes(R.drawable.ic_add_white_24dp)
        // - set title
        newCommand.findViewById<TextView>(R.id.headerTitle).text = context.getString(R.string.create_new_command)
        // - set listeners for entire view and inner view
        newCommand.findViewById<TextView>(R.id.headerTitle).setOnClickListener { transitionToShowNewCommandDialog(it.context) }
        newCommand.setOnClickListener { transitionToShowNewCommandDialog(it.context) }

        // create dialog
        // using a custom onBackPressed to handle navigating the different stages of creation process
        createCommandDialog = object : BottomSheetDialog(context) {
            override fun onBackPressed() {
                this@CommandCreator.onBackPressed()
            }
        }
        createCommandDialog?.setContentView(bottomSheetView)
        createCommandDialog?.setOnDismissListener { onDismiss() }
        createCommandDialog?.show()

        // run callback
        onCommandFromSelected()
    }

    private fun showNewCommandDialog(context: Context) {
        // check if we need to show initial "pair" dialog instead
        if (AppState.tempData.tempCommand?.actions?.size ?: 0 > 0) {
            showPairSignalDialog(context)
            return
        }

        // change state
        dialogState = CommandDialogState.NEW_COMMAND

        // create bottom sheet
        val bottomSheetView = context.layoutInflater.inflate(R.layout.v_create_sheet, null)
        val bottomSheetBinding : VCreateSheetBinding? = DataBindingUtil.bind(bottomSheetView)

        // setup bottom sheet
        bottomSheetBinding?.tvTitle?.text = context.getString(R.string.pairing_instructions_title)

        // create action sequence view - todo


        // create dialog
        // using a custom onBackPressed to handle navigating the different stages of creation process
        createCommandDialog = object : BottomSheetDialog(context) {
            override fun onBackPressed() {
                this@CommandCreator.onBackPressed()
            }
        }
        createCommandDialog?.setContentView(bottomSheetView)
        createCommandDialog?.setOnDismissListener { onDismiss() }
        createCommandDialog?.show()

        // run callback
        onNewCommandSelected()
    }

    private fun showPairSignalDialog(context: Context) {
        if (AppState.userData.hubs.size == 0) {
            // show 'no hubs' alert dialog and then direct user to set up a new hub - todo
            // return
        }

        // change state
        dialogState = CommandDialogState.PAIR_SIGNAL

        // create bottom sheet
        val bottomSheetView = context.layoutInflater.inflate(R.layout.v_create_sheet, null)
        val bottomSheetBinding : VCreateSheetBinding? = DataBindingUtil.bind(bottomSheetView)

        // setup bottom sheet
        bottomSheetBinding?.tvTitle?.text = context.getString(R.string.pairing_instructions_title)

        // create pairing dialog
        val pairingBinding : VPairInstructionsBinding = VPairInstructionsBinding.inflate(context.layoutInflater, bottomSheetBinding?.createSheetContent, true)
        if (AppState.userData.hubs.size <= 1) {
            // Since there's only 1 hub associated with the user, there's no reason to show hub spinner
            pairingBinding.hubsSpinner.visibility = View.GONE
            pairingBinding.tvHubsSpinner.visibility = View.GONE
        } else {
            // More than one associated hub means we need to allow the user to select which hub to target
            pairingBinding.hubsSpinner.visibility = View.VISIBLE
            pairingBinding.tvHubsSpinner.visibility = View.VISIBLE

            // List of hubs isn't ordered. Assumption is that the list ordering won't change
            val hubNames = ArrayList<String>()
                .apply {
                    AppState.userData.hubs.values.forEach { hub ->
                        add(hub.name)
                    }
                }
            pairingBinding.hubsSpinner.adapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, hubNames)
            pairingBinding.hubsSpinner.setOnItemClickListener { _, _, position, _ ->
                selectedHub = AppState.userData.hubs.values.elementAt(position)
            }

            // Sets the spinner selection to the currently selected hub (this is only relevant if this dialog is being recreated from saved state)
            pairingBinding.hubsSpinner.setSelection(getSelectedHubPosition())
        }
        pairingBinding.btnPair.setOnClickListener {
            // start the pairing listening process - todo
        }

        // create dialog
        // using a custom onBackPressed to handle navigating the different stages of creation process
        createCommandDialog = object : BottomSheetDialog(context) {
            override fun onBackPressed() {
                this@CommandCreator.onBackPressed()
            }
        }
        createCommandDialog?.setContentView(bottomSheetView)
        createCommandDialog?.setOnDismissListener { onDismiss() }
        createCommandDialog?.show()
    }

    private fun getSelectedHubPosition(): Int {
        val hub = selectedHub ?: AppState.userData.hubs[AppState.userData.user.defaultHub]
        AppState.userData.hubs.values.forEachIndexed { index, h ->
            if (hub?.uid == h.uid)
                return index
        }

        return 0
    }

/*
----------------------------------------------
  Helper Functions
----------------------------------------------
*/

    private fun getOwnerString(context: Context, name: String): CharSequence? {
        return if (name == AppState.userData.user.username.get())
            "${context.getString(R.string.remote_owner)} ${context.getString(R.string.you)}"
        else
            "${context.getString(R.string.remote_owner)} $name"
    }

    private fun transitionToShowNewCommandDialog(context: Context) {
        createCommandDialog?.let {
            isTransitioning = true
            it.dismiss()
        }
        showNewCommandDialog(context)
    }

    companion object {
        private fun stateFromIntVal(intVal : Int) = CommandDialogState.values().associateBy(CommandDialogState::value)[intVal]
        fun readStateFromParcel(parcel: Parcel): State {
            return State(
                stateFromIntVal(parcel.readInt()) ?: CommandDialogState.COMMAND_FROM,
                parcel.readInt(),
                parcel.readString()
            )
        }

        fun writeToParcel(parcel: Parcel, state: State) {
            parcel.writeInt(state.commandDialogState.value)
            parcel.writeInt(state.arrayPosition)
            parcel.writeString(state.selectedHubUID)
        }
    }

/*
----------------------------------------------
  Adapters
----------------------------------------------
*/



}