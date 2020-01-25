package com.ms8.smartirhub.android.main_view.fragments


import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.FRemoteCurrentBinding
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.firebase.RealtimeDatabaseFunctions
import com.ms8.smartirhub.android.main_view.MainViewActivity
import com.ms8.smartirhub.android.remote_control.button.creation.ButtonCreator
import com.ms8.smartirhub.android.remote_control.button.creation.ButtonCreator.Companion.NEW_BUTTON
import com.ms8.smartirhub.android.remote_control.button.models.Button
import com.ms8.smartirhub.android.remote_control.command.creation.GetFromRemoteActivity
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import com.ms8.smartirhub.android.remote_control.views.RemoteLayout
import com.ms8.smartirhub.android.utils.RequestCodes
import com.ms8.smartirhub.android.utils.extensions.getActionBarSize


class OLD_RemoteFragment : MainFragment() {
    val remoteLayout  = RemoteLayout()
    var binding : FRemoteCurrentBinding? = null
    private var screenHeight = 800

/*
-----------------------------------------------
    Listener Hell Below
-----------------------------------------------
*/

    // Is called when ButtonCreator requests a command from a particular remote
    private val requestCommandFromRemoteListener : (RemoteProfile) -> Unit = { remote ->
        val intent = Intent(context, GetFromRemoteActivity::class.java)
        intent.putExtra(GetFromRemoteActivity.EXTRA_REMOTE_UID, remote.uid)
        intent.putExtra(GetFromRemoteActivity.EXTRA_TYPE, GetFromRemoteActivity.Companion.ResultType.COMMAND)
        startActivityForResult(intent, RequestCodes.GET_COMMAND_FROM_REMOTE)
    }

    // Is called when ButtonCreator requests an action from a particular remote
    private val requestActionFromRemoteListener : (RemoteProfile) -> Unit = { remote ->
        val intent = Intent(context, GetFromRemoteActivity::class.java)
        intent.putExtra(GetFromRemoteActivity.EXTRA_REMOTE_UID, remote.uid)
        intent.putExtra(GetFromRemoteActivity.EXTRA_TYPE, GetFromRemoteActivity.Companion.ResultType.ACTIONS)
        startActivityForResult(intent, RequestCodes.GET_ACTIONS_FROM_REMOTE)
    }

    fun onButtonPressed(buttonPosition: Int, command: RemoteProfile.Command?) {
        if (AppState.tempData.tempRemoteProfile.inEditMode.get())
        {
            Log.d("TEST", "Editing button $buttonPosition")
            AppState.tempData.tempRemoteProfile.newButtonPosition = buttonPosition
            if (buttonPosition != NEW_BUTTON)
                AppState.tempData.tempButton.set(AppState.tempData.tempRemoteProfile.buttons[buttonPosition])
            remoteLayout.buttonCreator.showBottomDialog()
        } else
        {
            RealtimeDatabaseFunctions.sendCommandToHub(command)
            Log.d("TEST", "Sending command from button $buttonPosition")
        }
    }

/*
    ----------------------------------------------
        Overridden Functions
    ----------------------------------------------
*/
    override fun newInstance(): MainFragment { return OLD_RemoteFragment() }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCodes.GET_COMMAND_FROM_REMOTE) {
            // Only need to worry about enabling "create new button". The tempButton
            //  should already be added at the end of a successful process
            AppState.tempData.tempRemoteProfile.isCreatingNewButton.set(false)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FRemoteCurrentBinding.inflate(inflater, null, false)
        remoteLayout.binding = binding
        remoteLayout.buttonCreator.apply {
            //onCreationComplete = remoteCreationCompleteListener
            onRequestCommandFromRemote = requestCommandFromRemoteListener
            onRequestActionsFromRemote = requestActionFromRemoteListener
        }

        // Set top padding to account for toolbar
        activity?.getActionBarSize()?.let { remoteLayout.topPadding = it }

        // Set scroll listener to notify when to hide UI Elements
        remoteLayout.binding?.remoteLayout?.apply{
            addOnScrollListener((activity as MainViewActivity).showHideUIElementsScrollListener)
        }

        // Get screen height
        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)?.let {
            screenHeight = displayMetrics.heightPixels
        }

        return remoteLayout.binding!!.root
    }

    override fun onResume() {
        super.onResume()
        remoteLayout.startListening()
        remoteLayout.setButtonPressedListener { buttonPosition, command -> onButtonPressed(buttonPosition, command) }
        remoteLayout.binding = binding
        remoteLayout.buttonCreator.apply {
            //onCreationComplete = remoteCreationCompleteListener
            onRequestCommandFromRemote = requestCommandFromRemoteListener
            onRequestActionsFromRemote = requestActionFromRemoteListener
        }
        //AppState.tempData.tempRemoteProfile.buttons.addOnListChangedCallback(updateNameListener)
    }

    override fun onPause() {
        super.onPause()
        remoteLayout.stopListening()
        remoteLayout.setButtonPressedListener { _, _ ->  }
        remoteLayout.binding = null
        remoteLayout.buttonCreator.apply {
            onCreationComplete = {}
            onRequestCommandFromRemote = {}
            onRequestActionsFromRemote = {}
        }
    }

    companion object {
        const val recyclerViewTag = "myRemoteRV"
    }

    override fun toString() = "Remote Fragment"
}

