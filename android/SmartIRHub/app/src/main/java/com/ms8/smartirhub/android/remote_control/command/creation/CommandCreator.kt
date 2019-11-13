package com.ms8.smartirhub.android.remote_control.command.creation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.*
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.firebase.RealtimeDatabaseFunctions
import com.ms8.smartirhub.android.learn_signal.AdvancedSignalInfoActivity
import com.ms8.smartirhub.android.models.firestore.Hub
import com.ms8.smartirhub.android.models.firestore.Hub.Companion.DEFAULT_HUB
import com.ms8.smartirhub.android.models.firestore.IrSignal
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import kotlinx.android.synthetic.main.v_pair_signal_sheet.view.*
import org.jetbrains.anko.layoutInflater

class CommandCreator {

/*
----------------------------------------------
    Public Listeners
----------------------------------------------
*/
    var requestedCommandFromRemoteListener: (remote: RemoteProfile) -> Unit = {}
    var requestedActionsFromRemoteListener: (remote: RemoteProfile) -> Unit = {}
    var dialogDismissedListener : (fromBackPress: Boolean) -> Unit = {}
    var newCommandSelectedListener : () -> Unit = {}
    var existingCommandSelectedListener : () -> Unit = {}
    var commandCreatedListener : () -> Unit = {}

/*
----------------------------------------------
    Private Listeners
----------------------------------------------
*/

    private fun onDismiss() {
        if (!isTransitioning) {
            Log.d("t#", "dismissing commandCreator (isBackPressed = $isBackPressed)")
            dialogDismissedListener(isBackPressed)
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
    class State(val commandDialogState: CommandDialogState,
                val arrayPosition: Int,
                val selectedHubUID: String?,
                val isPairing: Boolean,
                val isSavingIrSignal: Boolean,
                val isSavingCommand: Boolean)

    enum class CommandDialogState(var value: Int) { COMMAND_FROM(0), NEW_COMMAND(1), PAIR_SIGNAL(2), ACTION_FROM(3) }
    private var dialogState = CommandDialogState.COMMAND_FROM
    private var arrayPosition = 0
    private var selectedHub : Hub? = null

    private var createCommandDialog : BottomSheetDialog? = null
    private var isTransitioning = false
    private var isPairing = false
    private var isSavingIrSignal = false
    private var isSavingCommand = false

    // not a true 'state' variable, but allows for onDismiss to denote when dismissing via onBackPressed or via swipe
    private var isBackPressed = false

    // global bindings - these need to be declared here so that listeners can enact updates to the UI
    private var pairingBinding : VPairSignalSheetBinding? = null
    private var pairSignalInfoBinding : VPairSignalInfoBinding? = null
    private var instructionsBinding : VPairSignalInstructionsBinding? = null
    private var newCommandSheetBinding : VNewCommandBinding? = null

/*
----------------------------------------------
    Public Accessors
----------------------------------------------
*/

    @SuppressLint("LogNotTimber")
    fun notifyActionsSelectedFromRemote() {
        if (dialogState != CommandDialogState.ACTION_FROM) {
            Log.e("CommandCreator", "notifyActionsSelectedFromRemote - notified of actions selection, but state is currently $dialogState")
            return
        }

        // transition back to 'newCommand dialog'
        dismissDialog(true)

        context?.let { showNewCommandDialog(it) }
    }

    @SuppressLint("LogNotTimber")
    fun notifyCommandSelectedFromRemote() {
        if (dialogState != CommandDialogState.COMMAND_FROM) {
            Log.e("CommandCreator", "notifyCommandSelectedFromRemote - notified of command selection, but state is currently $dialogState")
            return
        }

        onCommandCreated()
    }

    var context : Context? = null
    set(value) {
        field = value
        when {
            field == null ->
            {
                removePairingCallbacks()
                removeSignalSavingCallbacks()
                pairingBinding = null
                pairSignalInfoBinding = null

                dismissDialog(true)
            }
            isPairing -> addPairingCallbacks()
            isSavingIrSignal -> addSignalSavingCallbacks()
        }
    }

    fun setState(state: State) {
        dialogState = state.commandDialogState
        arrayPosition = state.arrayPosition
        selectedHub = AppState.userData.hubs[state.selectedHubUID]
        isPairing = state.isPairing
        isSavingIrSignal = state.isSavingIrSignal
        isSavingCommand = state.isSavingCommand
    }

    fun getState() : State {
        return State(dialogState,
            arrayPosition,
            selectedHub?.uid ?: AppState.userData.user.defaultHub,
            isPairing,
            isSavingIrSignal,
            isSavingCommand)
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
                    CommandDialogState.PAIR_SIGNAL -> showPairSignalDialog()
                    CommandDialogState.ACTION_FROM -> showActionsFromDialog()
                }
            }
        }
    }

    fun onBackPressed() {
        when (dialogState) {
            CommandDialogState.COMMAND_FROM -> commandFromOnBackPressed()
            CommandDialogState.NEW_COMMAND -> newCommandOnBackPressed()
            CommandDialogState.PAIR_SIGNAL -> pairSignalOnBackPressed()
            CommandDialogState.ACTION_FROM -> actionsFromOnBackPressed()
        }
    }

/*
----------------------------------------------
    Display Functions
----------------------------------------------
*/

    private fun showActionsFromDialog() {
        // change state
        dialogState = CommandDialogState.ACTION_FROM

        context?.let { c ->
            // create bottom sheet
            val bottomSheetView = c.layoutInflater.inflate(R.layout.v_create_sheet, null)
            val bottomSheetBinding : VCreateSheetBinding? = DataBindingUtil.bind(bottomSheetView)

            // setup bottom sheet
            bottomSheetBinding?.tvTitle?.setText(R.string.action_from_title)

            // create actions from list
            val actionsFromListBinding = VFromRemoteListBinding.inflate(c.layoutInflater, bottomSheetBinding?.createSheetContent, true)

            // first we create a group of all user remotes
            val fromUserRemotesItem = actionsFromListBinding.expandingList.createNewItem(R.layout.v_from_remote_expanding_item)
            //  - set user icon and color
            fromUserRemotesItem.setIndicatorIconRes(R.drawable.ic_my_remotes_icon)
            fromUserRemotesItem.setIndicatorColorRes(R.color.colorPrimary)
            // - set title
            fromUserRemotesItem.findViewById<TextView>(R.id.headerTitle).setText(R.string.from_user_remotes)
            // - create one sub item for each remote the user has access to
            fromUserRemotesItem.createSubItems(AppState.userData.remotes.size)
            AppState.userData.remotes.values.forEachIndexed { index, remote ->
                val subItemView = fromUserRemotesItem.getSubItemView(index)
                subItemView.findViewById<TextView>(R.id.tvTitle).text = remote.name
                subItemView.findViewById<TextView>(R.id.tvSubtitle).text = getOwnerString(subItemView.context, remote.name)
                // callback is used to start activity for result (need to show a remote view where user can click on a button to get that command)
                subItemView.setOnClickListener { requestedActionsFromRemoteListener(remote) }
            }

            // todo - then we list template remotes, allowing a user to pick a generic actions from generic remotes

            // finally we allow the user to pair a new IR signal
            val pairIrSignal = actionsFromListBinding.expandingList.createNewItem(R.layout.v_from_remote_expanding_item)
            // - set new command icon
            pairIrSignal.setIndicatorIconRes(R.drawable.ic_add_white_24dp)
            // - set title
            pairIrSignal.findViewById<TextView>(R.id.headerTitle).setText(R.string.pair_signal)
            // - set listeners for entire view and inner view
            pairIrSignal.findViewById<TextView>(R.id.headerTitle).setOnClickListener { transitionToPairDialog() }
            pairIrSignal.setOnClickListener { transitionToPairDialog() }

            // create dialog
            // using a custom onBackPressed to handle navigating the different stages of creation process
            createCommandDialog = object : BottomSheetDialog(c) {
                override fun onBackPressed() {
                    this@CommandCreator.onBackPressed()
                }
            }
            createCommandDialog?.setContentView(bottomSheetView)
            createCommandDialog?.setOnDismissListener { onDismiss() }
            createCommandDialog?.show()
        }
    }

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
            subItemView.setOnClickListener { requestedCommandFromRemoteListener(remote) }
        }

        // todo - then we list template remotes, allowing a user to pick a generic command from generic remotes

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
        createDialogView(context, bottomSheetView)

        // run callback
        existingCommandSelectedListener()
    }

    private fun showNewCommandDialog(context: Context) {
        // check if we need to show initial "pair" dialog instead
        if (AppState.tempData.tempCommand?.actions?.size ?: 0 == 0) {
            if (AppState.tempData.tempCommand == null)
                AppState.tempData.tempCommand = RemoteProfile.Command()
            showPairSignalDialog()
            return
        }

        // change state
        dialogState = CommandDialogState.NEW_COMMAND

        // create bottom sheet
        val bottomSheetView = context.layoutInflater.inflate(R.layout.v_create_sheet, null)
        val bottomSheetBinding : VCreateSheetBinding? = DataBindingUtil.bind(bottomSheetView)

        // setup bottom sheet
        bottomSheetBinding?.tvTitle?.setText(R.string.new_command_title)

        // setup new command sheet
        newCommandSheetBinding = VNewCommandBinding.inflate(context.layoutInflater, bottomSheetBinding?.createSheetContent, true)

        newCommandSheetBinding?.apply {
            rvCommandActions.apply {
                adapter = ActionSequenceAdapter(
                    object :  ActionSequenceAdapter.ActionSequenceAdapterCallbacks {
                        override fun addNewAction() {
                            dismissDialog(true)
                            showActionsFromDialog()
                        }

                        override fun startEditAction(action: RemoteProfile.Command.Action, position: Int) {
                            Log.d("todo", "todo") //todo show edit view
                        }

                        override fun onNoActionsLeft() {
                            onBackPressed()
                        }
                    },
                    AppState.tempData.tempCommand?.actions ?: ArrayList())
                layoutManager = LinearLayoutManager(context)
            }
            btnPos.apply {
                setText(R.string.create)
                setOnClickListener { onCreateNewCommand() }
            }
            btnNeg.apply {
                setText(R.string.cancel)
                setOnClickListener { onNewCommandDiscarded() }
            }
        }



        // create dialog
        // using a custom onBackPressed to handle navigating the different stages of creation process
        createDialogView(context, bottomSheetView)

        // run callback
        newCommandSelectedListener()
    }


    @SuppressLint("LogNotTimber")
    private fun showPairSignalDialog() {
        if (AppState.userData.hubs.size == 0) {
            // todo - show 'no hubs' alert dialog and then direct user to set up a new hub
            Log.w("CommandCreator", "showPairSignalDialog - user has not associated with a hub yet!")
            // return
        }

        // change state
        dialogState = CommandDialogState.PAIR_SIGNAL

        context?.let { context ->
            // create bottom sheet
            val bottomSheetView = context.layoutInflater.inflate(R.layout.v_create_sheet, null)
            val bottomSheetBinding : VCreateSheetBinding? = DataBindingUtil.bind(bottomSheetView)

            // setup bottom sheet
            bottomSheetBinding?.tvTitle?.text = context.getString(R.string.pairing_instructions_title)

            // create pairing dialog
            pairingBinding = VPairSignalSheetBinding.inflate(context.layoutInflater, bottomSheetBinding?.createSheetContent, true)

            // check for an error caught while creator view was hidden
            if (AppState.errorData.pairSignalError.get() != null) {
                showPairingErrorDialog()

                // no longer pairing
                isPairing = false

                // remove any listeners from pairing process
                removePairingCallbacks()

                // stop showing pairing animation
                pairingBinding!!.btnPos.revertAnimation()

                // clear error data to show that we've seen the error
                AppState.errorData.pairSignalError.set(null)

                // also clean up any tempData that is no longer valid based on seeing an error
                AppState.tempData.tempSignal.set(null)
            }

            // determine what views to show based if currently pairing/pairing complete/pairing failed
            if (AppState.tempData.tempSignal.get()?.containsAllRawData() == true) {
                // no longer pairing
                isPairing = false

                // pairing process was completed previously
                inflateSignalInfoView()

                // stop showing pairing animation
                pairingBinding?.btnPos?.revertAnimation()
            } else {
                // pairing process hasn't begun or is ongoing
                pairingBinding?.let {
                    inflateSignalInstructionsView()
                    if (isPairing)
                        it.btnPos.startAnimation()
                }
            }

            // create dialog
            // using a custom onBackPressed to handle navigating the different stages of creation process
            createDialogView(context, bottomSheetView)
        }
    }

    @SuppressLint("LogNotTimber")
    private fun showPairingErrorDialog() {
        context?.let { c ->
            if (AppState.errorData.pairSignalError.get() == null)
                Log.e("CommandCreator", "showPairingErrorDialog - pairSignalError was null!")

            AlertDialog.Builder(c)
                .setIcon(android.R.drawable.stat_notify_error)
                .setTitle(AppState.errorData.pairSignalError.get()?.titleID ?: R.string.err_title)
                .setPositiveButton(android.R.string.ok) { p0, _ -> p0?.dismiss() }
                .setMessage(AppState.errorData.pairSignalError.get()?.messageID ?: R.string.err_unknown_desc)
                .show()

            pairingBinding?.let { binding ->
                binding.btnPos.revertAnimation()
                binding.btnNeg.visibility = View.GONE
            }
        }
    }

    @SuppressLint("LogNotTimber")
    private fun showSavingSignalErrorDialog() {
        context?.let { c ->
            if (AppState.errorData.saveSignalError.get() == null)
                Log.e("CommandCreator", "showSavingSignalErrorDialog - saveSignalError was null!")

            AlertDialog.Builder(c)
                .setIcon(android.R.drawable.stat_notify_error)
                .setTitle(R.string.err_title)
                .setPositiveButton(android.R.string.ok) {p0, _ -> p0?.dismiss()}
                .setMessage(R.string.err_unknown_desc)
                .show()
        }
    }

    private fun dismissDialog(transitioning : Boolean = false) {
        createCommandDialog?.let {
            isTransitioning = transitioning
            it.dismiss()
            createCommandDialog = null
        }
    }

    @SuppressLint("LogNotTimber")
    private fun inflateSignalInfoView() {
        Log.d("CommandCreator", "inflateSignalInfoView - inflating...")

        pairSignalInfoBinding = VPairSignalInfoBinding.inflate(
            context!!.layoutInflater,
            pairingBinding?.pairingContent,
            true
        )
        // populate signal info
        AppState.tempData.tempSignal.get()?.let { irSignal ->
            pairSignalInfoBinding?.apply {
                tvSigType.text = irSignal.encodingType.toString()
                tvSigCode.text = irSignal.code
                btnShowAdvancedInfo.setOnClickListener {
                    it.context.startActivity(Intent(context, AdvancedSignalInfoActivity::class.java))
                }
            }
        }

        pairingBinding?.apply {
            // show cancel button
            btnNeg.apply {
                setText(android.R.string.cancel)
                visibility = View.VISIBLE
                setOnClickListener {
                    if (isSavingIrSignal) stopSaveSignalProcess()
                    else onPairedSignalDiscarded()
                }
            }

            // set btnPos to 'save' function
            btnPos.apply {
                setText(R.string.save)
                setOnClickListener { beginSignalSavingProcess() }
                revertAnimation {pairingBinding?.btnPos?.setText(R.string.save)}
            }
        }
    }

    private fun inflateSignalInstructionsView() {
        Log.d("CommandCreator", "inflateSignalInstructionsView - inflating...")
        pairingBinding?.btnNeg?.visibility = View.GONE

        context?.let { c ->
            instructionsBinding = VPairSignalInstructionsBinding.inflate(
                c.layoutInflater,
                pairingBinding?.pairingContent,
                true)
            instructionsBinding?.apply {
                if (AppState.userData.hubs.size <= 1) {
                    // Since there's only 1 hub associated with the user, there's no reason to show hub spinner
                    hubsSpinner.visibility = View.GONE
                    tvHubsSpinner.visibility = View.GONE
                } else {
                    // More than one associated hub means we need to allow the user to select which hub to target
                    hubsSpinner.visibility = View.VISIBLE
                    tvHubsSpinner.visibility = View.VISIBLE

                    setupHubsSpinner(hubsSpinner)
                }
            }

            // set btnPos to 'pair' function
            pairingBinding?.btnPos
                ?.apply {
                    btnPos.setText(R.string.pair)
                    revertAnimation { pairingBinding?.btnPos?.setText(R.string.pair) }
                    setOnClickListener { beginPairingProcess() }
                }
        }
    }
/*
----------------------------------------------
  Helper Functions
----------------------------------------------
*/

    /* Signal Pairing Helper Functions  */

    private fun addPairingCallbacks() {
        AppState.tempData.tempSignal.addOnPropertyChangedCallback(pairSignalListener)
        AppState.errorData.pairSignalError.addOnPropertyChangedCallback(pairSignalErrorListener)
    }

    private fun removePairingCallbacks() {
        AppState.tempData.tempSignal.removeOnPropertyChangedCallback(pairSignalListener)
        AppState.errorData.pairSignalError.removeOnPropertyChangedCallback(pairSignalErrorListener)
    }

    private val pairSignalListener = object : Observable.OnPropertyChangedCallback() {
        @SuppressLint("LogNotTimber")
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            if (AppState.tempData.tempSignal.get() == null) {
                Log.d("CommandCreator", "pairSignalListener - tempSignal set to null")
            } else if (AppState.tempData.tempSignal.get()?.containsAllRawData() == true) {
                if (isPairing) {
                    isPairing = false
                    pairingBinding
                        ?.let {
                            it.pairingContent.removeAllViews()
                            inflateSignalInfoView()
                        }
                    removePairingCallbacks()
                }
            } else {
                Log.d("CommandCreator", "pairSignalListener - initial signal info received... waiting on raw data")
            }
        }
    }

    private val pairSignalErrorListener  = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            if (AppState.errorData.pairSignalError.get() != null) {
                isPairing = false
                removePairingCallbacks()
                showPairingErrorDialog()
                AppState.errorData.pairSignalError.set(null)
                pairingBinding?.btnPos?.revertAnimation()
            }
        }
    }

    private fun beginPairingProcess() {
        if (isPairing)
            return

        isPairing = true
        RealtimeDatabaseFunctions.sendListenAction2(selectedHub?.uid ?: AppState.userData.user.defaultHub)

        pairingBinding?.btnPos?.startAnimation()

        // show the cancel button to allow users to manually stop pairing process
        pairingBinding?.btnNeg?.visibility = View.VISIBLE
        pairingBinding?.btnNeg?.let { btn ->
            btn.visibility = View.VISIBLE
            btn.setOnClickListener {
                stopPairingProcess()
            }
        }

        addPairingCallbacks()
    }

    private fun stopPairingProcess() {
        isPairing = false
        pairingBinding?.btnPos?.revertAnimation { pairingBinding?.btnPos?.setText(R.string.pair) }
        pairingBinding?.btnNeg?.visibility = View.GONE
        removePairingCallbacks()
    }

    private fun onPairedSignalDiscarded() {
        AppState.tempData.tempSignal.set(null)
        pairingBinding?.let { binding ->
            binding.pairingContent.removeAllViews()
//            context?.let {
//                val tempView = it.layoutInflater.inflate(R.layout.v_pair_signal_info, null, false)
//                binding.pairingContent.addView(tempView)
//            }
            inflateSignalInstructionsView()
            //todo - figure out why v_pair_signal_instructions causes the bottom dialog to attach to the top of screen
        }
    }

    /* Signal Saving Helper Functions  */

    private fun addSignalSavingCallbacks() {
        AppState.tempData.tempSignal.addOnPropertyChangedCallback(saveSignalListener)
        AppState.errorData.saveSignalError.addOnPropertyChangedCallback(saveSignalErrorListener)
    }

    private fun removeSignalSavingCallbacks() {
        AppState.tempData.tempSignal.removeOnPropertyChangedCallback(saveSignalListener)
        AppState.errorData.saveSignalError.removeOnPropertyChangedCallback(saveSignalErrorListener)
    }

    private val saveSignalListener = object : Observable.OnPropertyChangedCallback() {
        @SuppressLint("LogNotTimber")
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            when {
                AppState.tempData.tempSignal.get() == null -> Log.d("CommandCreator", "saveSignalListener - tempSignal set to null")
                AppState.tempData.tempSignal.get()?.uid != "" -> { AppState.tempData.tempSignal.get()?.let { onSignalSaved(it) } }
                else -> Log.w("CommandCreator", "saveSignalListener - tempSignal was changed with invalid uid")
            }
        }
    }

    private val saveSignalErrorListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            if (AppState.errorData.saveSignalError.get() != null) {
                // saving process is done, clean up!
                isSavingIrSignal = false
                removeSignalSavingCallbacks()
                showSavingSignalErrorDialog()
            }
        }
    }

    private fun onSignalSaved(irSignal: IrSignal) {
        // saving process is complete
        isSavingIrSignal = false

        // add new signal to app state local database
        AppState.userData.irSignals[irSignal.uid] = irSignal

        // add to the new command as well
        AppState.tempData.tempCommand?.actions?.add(
            RemoteProfile.Command.Action(
                selectedHub?.uid ?: DEFAULT_HUB,
                irSignal.uid
            )
        )

        // clean up tempIrSignal
        AppState.tempData.tempSignal.set(null)

        // pairing new signal process is complete! back to 'new command' overview
        dialogState = CommandDialogState.NEW_COMMAND
        dismissDialog(true)
        context?.let { showNewCommandDialog(it) }
    }

    private fun beginSignalSavingProcess() {
        // ensure that saving process hasn't already been kicked off
        if (isSavingIrSignal)
            return

        // saving process has begun
        isSavingIrSignal = true
        FirestoreActions.addIrSignal()

        // begin animating to show saving is in progress
        pairingBinding?.btnPos?.startAnimation()

        // add listeners
        addSignalSavingCallbacks()
    }

    private fun stopSaveSignalProcess() {
        isSavingIrSignal = false
        removeSignalSavingCallbacks()
        pairingBinding?.btnPos?.revertAnimation()
    }

    /* On Back Pressed Helper Functions  */

    private fun actionsFromOnBackPressed() {
        dismissDialog(true)
        context?.let { showNewCommandDialog(it) }
    }

    private fun commandFromOnBackPressed() {
        createCommandDialog?.let {
            isBackPressed = true
            dismissDialog(false)
        }
    }

    private fun pairSignalOnBackPressed() {
        when {
        // Pairing in progress
            isPairing -> { stopPairingProcess() }
        // Saving signal in progress
            isSavingIrSignal -> { stopSaveSignalProcess() }
        // Showing paired signal info
            AppState.tempData.tempSignal.get() != null -> { onPairedSignalDiscarded() }
        // Showing pairing instruction
            else -> {
                if (AppState.tempData.tempCommand?.actions?.size ?: 0 > 0) {
                    // go back to "new command" view as this was adding an additional signal to a command
            dismissDialog(true)
                    showNewCommandDialog(context!!)
                } else {
                    // go back to "command from" view as this was the first signal for a new command
            dismissDialog(true)
                    // discard tempData for abandoned command
                    AppState.tempData.tempCommand = null
                    showCommandFromDialog(context!!)
                }
            }
        }
    }

    private fun newCommandOnBackPressed() {
        dismissDialog(true)

        // discard any temp command in the making
        AppState.tempData.tempCommand = null
        showCommandFromDialog(context!!)
    }

    /* Misc Helper Functions  */

    private fun onNewCommandDiscarded() {
        AppState.tempData.tempCommand = null
        dismissDialog(true)
        context?.let { showCommandFromDialog(it) }
    }

    @SuppressLint("LogNotTimber")
    private fun onCreateNewCommand() {
        when (AppState.tempData.tempButton.get()) {
            null -> Log.w("CommandCreator", "onCreateNewCommand - new command created without a button to bind it to!")
            else -> AppState.tempData.tempCommand?.let {
                AppState.tempData.tempButton.get()?.commands?.add(it)
                onCommandCreated()
                AppState.tempData.tempCommand = null
                AppState.tempData.tempSignal.set(null)
            }
        }
    }

    private fun createDialogView(context: Context, bottomSheetView: View) {
        createCommandDialog = object : BottomSheetDialog(context) {
            override fun onBackPressed() {
                this@CommandCreator.onBackPressed()
            }
        }
        createCommandDialog?.setCancelable(false)
        createCommandDialog?.setContentView(bottomSheetView)
        createCommandDialog?.setOnDismissListener { onDismiss() }
        createCommandDialog?.show()
    }

    private fun onCommandCreated() {
        // dismiss dialog in anticipation for something else to continue the process
        dismissDialog(true)

        // reset dialogState to beginning
        dialogState = CommandDialogState.COMMAND_FROM

        // notify that command creation process is complete
        commandCreatedListener()
    }

    private fun setupHubsSpinner(hubsSpinner: Spinner) {
        // List of hubs isn't ordered. Assumption is that the list ordering won't change
        val hubNames = ArrayList<String>()
            .apply {
                AppState.userData.hubs.values.forEach { hub ->
                    add(hub.name)
                }
            }

        hubsSpinner.adapter = ArrayAdapter<String>(hubsSpinner.context, android.R.layout.simple_list_item_1, hubNames)
        hubsSpinner.setOnItemClickListener { _, _, position, _ ->
            selectedHub = AppState.userData.hubs.values.elementAt(position)
        }

        // Sets the spinner selection to the currently selected hub (this is only relevant if this dialog is being recreated from saved state)
        hubsSpinner.setSelection(getSelectedHubPosition())
    }

    private fun getSelectedHubPosition(): Int {
        val hub = selectedHub ?: AppState.userData.hubs[AppState.userData.user.defaultHub]
        AppState.userData.hubs.values.forEachIndexed { index, h ->
            if (hub?.uid == h.uid)
                return index
        }

        return 0
    }

    private fun getOwnerString(context: Context, name: String): CharSequence? {
        return if (name == AppState.userData.user.username.get())
            "${context.getString(R.string.remote_owner)} ${context.getString(R.string.you)}"
        else
            "${context.getString(R.string.remote_owner)} $name"
    }

    private fun transitionToShowNewCommandDialog(context: Context) {
        dismissDialog(true)
        showNewCommandDialog(context)
    }

    private fun transitionToPairDialog() {
        dismissDialog(true)
        showPairSignalDialog()
    }

    companion object {
        private fun stateFromIntVal(intVal : Int) = CommandDialogState.values().associateBy(CommandDialogState::value)[intVal]
        fun readStateFromParcel(parcel: Parcel): State {
            return State(
                stateFromIntVal(parcel.readInt()) ?: CommandDialogState.COMMAND_FROM,
                parcel.readInt(),
                parcel.readString(),
                parcel.readInt() == 1,
                parcel.readInt() == 1,
                parcel.readInt() == 1
            )
        }

        fun writeToParcel(parcel: Parcel, state: State) {
            parcel.writeInt(state.commandDialogState.value)
            parcel.writeInt(state.arrayPosition)
            parcel.writeString(state.selectedHubUID)
            parcel.writeInt(if (state.isPairing) 1 else 0)
            parcel.writeInt(if (state.isSavingIrSignal) 1 else 0)
            parcel.writeInt(if (state.isSavingCommand) 1 else 0)
        }
    }

/*
----------------------------------------------
  Debug
----------------------------------------------
*/



}