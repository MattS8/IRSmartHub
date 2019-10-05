package com.ms8.smartirhub.android.remote_control.creation

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.VCreateFromExistingRemoteBinding
import com.ms8.smartirhub.android.databinding.VCreateRemoteFromBinding
import com.ms8.smartirhub.android.databinding.VItemExistingRemoteBinding
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import org.jetbrains.anko.layoutInflater

class RemoteCreator {
    // Listeners
    var onCreateDialogDismiss: () -> Unit = {}
    var onCreateBlankRemote: () -> Unit = {}
    var onCreateFromDeviceTemplate: () -> Unit = {}
    var onCreateFromExistingRemote: () -> Unit = {}

/*
----------------------------------------------
    State Functions
----------------------------------------------
*/

    // Bottom Dialog State
    enum class RemoteDialogState(var value: Int) { CREATE_FROM(1), TEMPLATES(2), EXISTING_REMOTE(3) }
    var dialogState : RemoteDialogState = RemoteDialogState.CREATE_FROM
    private var createRemoteDialog : BottomSheetDialog? = null
    private var isTransitioning : Boolean = false


    fun showBottomDialog(context: Context) {
        when (dialogState) {
            RemoteDialogState.CREATE_FROM -> { showCreateRemoteDialog(context) }
            RemoteDialogState.TEMPLATES -> { showCreateFromDeviceTemplateDialog(context) }
            RemoteDialogState.EXISTING_REMOTE -> { showCreateFromExistingRemoteDialog(context) }
        }
    }

    fun dismissBottomDialog() {
        // set state if not transitioning
        if(!isTransitioning)
            dialogState = RemoteDialogState.CREATE_FROM

        // dismiss dialog if set
        createRemoteDialog?.dismiss()
        createRemoteDialog = null

    }


/*
----------------------------------------------
    Display Dialog Functions
----------------------------------------------
*/

    private fun showCreateRemoteDialog(context: Context) {
        // change state
        dialogState = RemoteDialogState.CREATE_FROM

        // set up bottom sheet dialog
        val createRemoteView = context.layoutInflater.inflate(R.layout.v_create_remote_from, null)
        createRemoteDialog = BottomSheetDialog(context)
        val createRemoteFromBinding = DataBindingUtil.bind<VCreateRemoteFromBinding>(createRemoteView)
        createRemoteDialog?.setContentView(createRemoteView)
        createRemoteDialog?.setOnDismissListener {
            if (!isTransitioning) {
                dialogState = RemoteDialogState.CREATE_FROM
                onCreateDialogDismiss()
            }
        }

        // set up onClick listeners (device template, existing remote, blank layout)
        createRemoteFromBinding?.tvFromScratch?.setOnClickListener { onBlankRemoteClicked() }
        createRemoteFromBinding?.tvFromDeviceTemplate?.setOnClickListener { v -> onFromDeviceTemplateClicked(v.context) }
        createRemoteFromBinding?.tvFromExistingRemote?.setOnClickListener { v -> onFromExistingRemoteClicked(v.context) }

        // Hide "From Existing Remote" if user doesn't have any
        if (AppState.userData.remotes.size == 0)
            createRemoteFromBinding?.tvFromExistingRemote?.visibility = View.GONE

        createRemoteDialog?.show()
    }

    private fun showCreateFromExistingRemoteDialog(context: Context) {
        // change state
        dialogState = RemoteDialogState.EXISTING_REMOTE

        // set up bottom sheet dialog
        val existingDeviceView = context.layoutInflater.inflate(R.layout.v_create_from_existing_remote, null)
        createRemoteDialog = BottomSheetDialog(context)
        val existingBinding = DataBindingUtil.bind<VCreateFromExistingRemoteBinding>(existingDeviceView)
        createRemoteDialog?.setContentView(existingDeviceView)
        createRemoteDialog?.setOnDismissListener {
            if (!isTransitioning) {
                dialogState = RemoteDialogState.CREATE_FROM
                onCreateDialogDismiss()
            }
        }

        // set up list of existing remotes
        existingBinding?.remotesList?.adapter = ExistingRemotesAdapter(ArrayList(AppState.userData.remotes.values))

    }

    private fun showCreateFromDeviceTemplateDialog(context: Context) {
        //todo show list of device templates, then copy selected template
    }

/*
----------------------------------------------
    OnClick Functions
----------------------------------------------
*/

    private fun onFromExistingRemoteClicked(context: Context) {
        isTransitioning = true
        dismissBottomDialog()
        showCreateFromExistingRemoteDialog(context)
        isTransitioning = false
    }

    private fun onFromDeviceTemplateClicked(context: Context) {
        isTransitioning = true
        createRemoteDialog?.dismiss()
        showCreateFromDeviceTemplateDialog(context)
        isTransitioning = false
    }

    private fun onBlankRemoteClicked() {
        createRemoteDialog?.dismiss()

        // create blank remote in tempData
        AppState.resetTempRemote()

        // set remote to edit mode
        AppState.tempData.tempRemoteProfile.inEditMode.set(true)

        // run listener
        onCreateBlankRemote()
    }

    private fun onExistingRemoteItemClicked(remote: RemoteProfile, titlePrefix: String = "") {
        createRemoteDialog?.dismiss()

        // copy remote into tempData
        AppState.tempData.tempRemoteProfile.copyFrom(remote)

        // append 'Copy of' to beginning of title
        AppState.tempData.tempRemoteProfile.name = titlePrefix + remote.name

        // set remote to edit mode
        AppState.tempData.tempRemoteProfile.inEditMode.set(true)

        // run listener
        onCreateFromExistingRemote()
    }

    private fun onDeviceTemplateItemClicked() {
        createRemoteDialog?.dismiss()

        //todo copy template into tempData

        // set remote to edit mode
        AppState.tempData.tempRemoteProfile.inEditMode.set(true)

        // run listener
        onCreateFromDeviceTemplate
    }

    companion object {
        fun stateFromIntVal(intVal : Int) = RemoteDialogState.values().associateBy(RemoteDialogState::value)[intVal]
    }

/*
----------------------------------------------
    Existing Remotes Adapter
----------------------------------------------
*/

    inner class ExistingRemotesAdapter(existingRemotes: ArrayList<RemoteProfile> = ArrayList()) : RecyclerView.Adapter<ExistingRemotesAdapter.ViewHolder>() {
        var remotes : ArrayList<RemoteProfile> = existingRemotes
        set(value) {
            field = value
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(parent)
        }

        override fun getItemCount(): Int {
            return remotes.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(remotes[position])
        }

        inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.v_item_existing_remote, null)) {
            var binding: VItemExistingRemoteBinding? = DataBindingUtil.bind(itemView)
            var remote: RemoteProfile? = null

            fun bind(remote : RemoteProfile?) {
                this.remote = remote
                remote?.let {
                    binding?.tvTitle?.text = it.name
                    binding?.tvSubtitle?.text = getSubtitle()
                    itemView.setOnClickListener { onExistingRemoteItemClicked(remote, itemView.context.getString(R.string.copy_of) + " ") }
                }
            }

            private fun getSubtitle() : String {
                return itemView.context.getString(R.string.from)  + " " + remote?.ownerUsername
            }
        }
    }


}