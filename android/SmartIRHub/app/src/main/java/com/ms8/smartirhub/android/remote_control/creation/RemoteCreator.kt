package com.ms8.smartirhub.android.remote_control.creation

import android.content.Context
import android.view.View
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.VCreateRemoteFromBinding
import org.jetbrains.anko.layoutInflater

class RemoteCreator {
    private var createRemoteDialog : BottomSheetDialog? = null
    private var createRemoteFromBinding : VCreateRemoteFromBinding? = null

    var onCreateDialogDismiss: () -> Unit = {}
    var onCreateBlankRemote: () -> Unit = {}
    var onCreateFromDeviceTemplate: () -> Unit = {}
    var onCreateFromExistingRemote: () -> Unit = {}

    fun showCreateRemoteDialog(context: Context) {
        // set up bottom sheet dialog
        val createRemoteView = context.layoutInflater.inflate(R.layout.v_create_remote_from, null)
        createRemoteDialog = BottomSheetDialog(context)
        createRemoteFromBinding = DataBindingUtil.bind(createRemoteView)
        createRemoteDialog?.setContentView(createRemoteView)
        createRemoteDialog?.setOnDismissListener { onCreateDialogDismiss() }

        // set up onClick listeners (device template, existing remote, blank layout)
        createRemoteFromBinding?.tvFromScratch?.setOnClickListener { createBlankRemote() }
        createRemoteFromBinding?.tvFromDeviceTemplate?.setOnClickListener { createFromDeviceTemplate() }
        createRemoteFromBinding?.tvFromExistingRemote?.setOnClickListener { createFromExistingRemote() }

        // Hide "From Existing Remote" if user doesn't have any
        if (AppState.userData.remotes.size == 0)
            createRemoteFromBinding?.tvFromExistingRemote?.visibility = View.GONE

        createRemoteDialog?.show()
    }

    private fun createFromExistingRemote() {
        createRemoteDialog?.dismiss()

        //todo show list of existing remotes, then copy selected remote

        onCreateFromExistingRemote()
    }

    private fun createFromDeviceTemplate() {
        createRemoteDialog?.dismiss()

        //todo show list of device templates, then copy selected template

        onCreateFromDeviceTemplate()
    }

    private fun createBlankRemote() {
        createRemoteDialog?.dismiss()

        // create blank remote in tempData
        AppState.resetTempRemote()

        // set remote to edit mode
        AppState.tempData.tempRemoteProfile.inEditMode.set(true)

        // run listener
        onCreateBlankRemote()
    }
}