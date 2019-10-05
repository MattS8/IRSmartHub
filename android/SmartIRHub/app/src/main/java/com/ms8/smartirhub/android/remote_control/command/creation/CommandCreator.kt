package com.ms8.smartirhub.android.remote_control.command.creation

import android.content.Context
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import com.diegodobelo.expandingview.ExpandingList
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.VCreateSheetBinding
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import org.jetbrains.anko.layoutInflater

class CommandCreator {

/*
----------------------------------------------
    Public Listeners
----------------------------------------------
*/
    var onRequestCommandFromRemote: (remote: RemoteProfile) -> Unit = {}
    var onDialogDismiss : () -> Unit = {}

/*
----------------------------------------------
    Private Listeners
----------------------------------------------
*/

    private fun onDismiss() {
        if (!isTransitioning) {
            onDialogDismiss()
        }

        dialogState = CommandDialogState.COMMAND_FROM
        createCommandDialog = null
    }

/*
----------------------------------------------
    State Variables
----------------------------------------------
*/
    enum class CommandDialogState(var value: Int) { COMMAND_FROM(0), NEW_COMMAND(1) }
    var dialogState = CommandDialogState.COMMAND_FROM
    var arrayPosition = 0

    var createCommandDialog : BottomSheetDialog? = null
    private var isTransitioning = false

/*
----------------------------------------------
    Public Accessors
----------------------------------------------
*/

    fun showBottomDialog(context: Context, arrayPosition: Int) {
        this.arrayPosition = arrayPosition
        when (dialogState) {
            CommandDialogState.COMMAND_FROM -> showCommandFromDialog(context)
            CommandDialogState.NEW_COMMAND -> showNewCommandDialog(context)
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
        val commandFromList : ExpandingList = context.layoutInflater.inflate(R.layout.v_from_remote_list, bottomSheetBinding?.createSheetContent) as ExpandingList

        // add views to their respective containers
        bottomSheetBinding?.createSheetContent?.addView(commandFromList)

        // set up bottom sheet
        bottomSheetBinding?.tvTitle?.text = context.getText(R.string.choose_button_command_from)

        // set up list of commands
        // first we create a group of all user remotes
        val fromUserRemotesItem = commandFromList.createNewItem(R.layout.v_from_remote_expanding_item)
        fromUserRemotesItem.findViewById<TextView>(R.id.headerTitle).text = commandFromList.context.getString(R.string.from_user_remotes)
        fromUserRemotesItem.createSubItems(AppState.userData.remotes.size)
        AppState.userData.remotes.values.forEachIndexed { index, remote ->
            val subItemView = fromUserRemotesItem.getSubItemView(index)
            subItemView.findViewById<TextView>(R.id.tvTitle).text = remote.name
            subItemView.findViewById<TextView>(R.id.tvSubtitle).text = getOwnerString(subItemView.context, remote.name)
            // callback is used to start activity for result (need to show a remote view where user can click on a button to get that command)
            subItemView.setOnClickListener { onRequestCommandFromRemote(remote) }
        }

        // then we list template remotes, allowing a user to pick a generic command from generic remotes - todo


        // new command
        val newCommand = commandFromList.createNewItem(R.layout.v_from_remote_expanding_item)
        newCommand.findViewById<TextView>(R.id.headerTitle).text = context.getString(R.string.create_new_command)
        newCommand.setOnClickListener { transitionToShowNewCommandDialog(it.context) }

        // create dialog
        createCommandDialog = BottomSheetDialog(context)
        createCommandDialog?.setContentView(bottomSheetView)
        createCommandDialog?.setOnDismissListener { onDismiss() }
        createCommandDialog?.show()
    }

    private fun showNewCommandDialog(context: Context) {
        // change state
        dialogState = CommandDialogState.NEW_COMMAND

        // create bottom sheet
        val bottomSheetView = context.layoutInflater.inflate(R.layout.v_create_sheet, null)
        val bottomSheetBinding : VCreateSheetBinding? = DataBindingUtil.bind(bottomSheetView)

        // setup bottom sheet
        bottomSheetBinding?.tvTitle?.text = context.getString(R.string.new_command)

        // create action sequence view - todo


        // create dialog
        createCommandDialog = BottomSheetDialog(context)
        createCommandDialog?.setContentView(bottomSheetView)
        createCommandDialog?.setOnDismissListener { onDismiss() }
        createCommandDialog?.show()
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
        isTransitioning = true
        createCommandDialog?.dismiss()
        showNewCommandDialog(context)
        isTransitioning = false
    }

/*
----------------------------------------------
  Adapters
----------------------------------------------
*/



}