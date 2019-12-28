package com.ms8.smartirhub.android.remote_control.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.Observable
import androidx.databinding.ObservableList
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.FRemoteCurrentBinding
import com.ms8.smartirhub.android.remote_control.button.creation.ButtonCreator
import com.ms8.smartirhub.android.remote_control.button.models.Button
import com.ms8.smartirhub.android.remote_control.creation.RemoteCreator

class RemoteLayout {
    var binding: FRemoteCurrentBinding? = null
    set(value) {
        field = value
        if (field != null) {
            field?.remoteLayout?.let {
                buttonCreator.context = it.context
                it.topPadding = topPadding
            }
            setupAdapter()
            applyBackgroundTint()
        } else {
            buttonCreator.context = null
        }
    }

    var onAddNewButton = {}

    private var isListening : Boolean = false

    val buttonCreator = ButtonCreator()

    private val remoteLayoutAdapter = RemoteLayoutView.RemoteLayoutAdapter()

    var topPadding = 0
    set(value) {
        field = value
        binding?.remoteLayout?.topPadding = topPadding
    }

    fun applyBackgroundTint() {
        val isInEditMode = AppState.tempData.tempRemoteProfile.inEditMode.get()
        binding?.remoteLayout?.apply {
            backgroundTintList = if (isInEditMode)
                ContextCompat.getColorStateList(context, R.color.colorRemoteBG_Editing)
            else
                ContextCompat.getColorStateList(context, R.color.colorRemoteBG)
        }
    }

/*
-----------------------------------------------
    Listener Hell Below
-----------------------------------------------
*/

    // Is triggered whenever the user enters/exits "edit mode" on the remote (i.e. by clicking the center edit button)
    private val editModeListener  =  object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            applyBackgroundTint()
            remoteLayoutAdapter.notifyDataSetChanged()
        }
    }

    // Is triggered whenever tempRemoteProfile change's 'isCreatingNewButton' (i.e. when a user clicks 'add button'
    private val addButtonListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            if (AppState.tempData.tempRemoteProfile.isCreatingNewButton.get() && AppState.tempData.tempRemoteProfile.inEditMode.get()){
                Log.d("TEST", "addButton DETECTED")
                //onAddNewButton()
                buttonCreator.showBottomDialog()
            }
        }
    }

    // Is triggered whenever the button creation process is complete (i.e. whenever a button is added to tempRemoteProfile.buttons)
    private val buttonListener = object : ObservableList.OnListChangedCallback<ObservableList<Button>>() {
        override fun onChanged(sender: ObservableList<Button>?) {
            binding?.remoteLayout?.adapter?.notifyDataSetChanged()
            updateRemoteLayoutAdapter()
            checkPromptVisibility()
        }

        override fun onItemRangeRemoved(sender: ObservableList<Button>?, positionStart: Int, itemCount: Int) {
            remoteLayoutAdapter.notifyItemRangeRemoved(positionStart, itemCount)
            updateRemoteLayoutAdapter()
            checkPromptVisibility()
        }

        override fun onItemRangeMoved(sender: ObservableList<Button>?, fromPosition: Int, toPosition: Int, itemCount: Int) {
            binding?.remoteLayout?.adapter?.notifyDataSetChanged()
            checkPromptVisibility()
            updateRemoteLayoutAdapter()
        }

        override fun onItemRangeInserted(sender: ObservableList<Button>?, positionStart: Int, itemCount: Int) {
            binding?.remoteLayout?.adapter?.notifyItemRangeInserted(positionStart, itemCount)
            checkPromptVisibility()
            updateRemoteLayoutAdapter()
        }

        override fun onItemRangeChanged(sender: ObservableList<Button>?, positionStart: Int, itemCount: Int) {
            binding?.remoteLayout?.adapter?.notifyItemRangeChanged(positionStart, itemCount)
            checkPromptVisibility()
            updateRemoteLayoutAdapter()
        }
    }

    // todo - this is a quick fix to a bug where the adapter doesn't adhere to itemCount
    private fun updateRemoteLayoutAdapter() {
        binding?.remoteLayout?.setupAdapter(remoteLayoutAdapter)
        binding?.remoteLayout?.invalidate()
    }

    private fun checkPromptVisibility() {
        val isEmptyRemote = AppState.tempData.tempRemoteProfile.buttons.size == 0
                && !AppState.tempData.tempRemoteProfile.inEditMode.get()
        val isFirstTimeRemote = isEmptyRemote
                && AppState.userData.remotes.size == 0


        when {
            isFirstTimeRemote ->
            {
                binding?.apply {
                    txtCreateFirstRemoteP1.apply {
                        visibility = View.VISIBLE
                        setText(R.string.new_remote_prompt)
                        setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(this.context, R.drawable.ic_new_remote_icon), null)
                    }
                    txtCreateFirstRemoteP2.apply {
                        visibility = View.VISIBLE
                        setText(R.string.icon_period)
                    }
                }
            }
            isEmptyRemote ->
            {
                binding?.apply {
                    txtCreateFirstRemoteP1.apply {
                        visibility = View.VISIBLE
                        setText(R.string.add_buttons_prompt)
                        setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(this.context, R.drawable.ic_mode_edit_white_24dp), null)

                    }
                    txtCreateFirstRemoteP2.apply {
                        visibility = View.VISIBLE
                        setText(R.string.icon_period)
                    }
                }
            }
            else ->
            {
                binding?.apply {
                    txtCreateFirstRemoteP1.visibility = View.GONE
                    txtCreateFirstRemoteP2.visibility = View.GONE
                }
            }
        }
    }

    @SuppressLint("LogNotTimber")
    fun setupAdapter() {
        if (binding == null) {
            Log.w("RemoteLayout", "setupAdapter - attempted to set up adapter without binding!")
            return
        }

        binding?.remoteLayout?.setupAdapter(remoteLayoutAdapter)?.also {
            checkPromptVisibility()
        }
    }

    fun startListening() {
        if (!isListening) {
            isListening = true
            // ButtonCreator Listeners
            //AppState.tempData.tempRemoteProfile.isCreatingNewButton.addOnPropertyChangedCallback(addButtonListener)
            AppState.tempData.tempRemoteProfile.buttons.addOnListChangedCallback(buttonListener)
            AppState.tempData.tempRemoteProfile.inEditMode.addOnPropertyChangedCallback(editModeListener)
        }
    }

    fun stopListening() {
        isListening = false
        // ButtonCreator listeners
        //AppState.tempData.tempRemoteProfile.isCreatingNewButton.removeOnPropertyChangedCallback(addButtonListener)
        AppState.tempData.tempRemoteProfile.buttons.removeOnListChangedCallback(buttonListener)
        AppState.tempData.tempRemoteProfile.inEditMode.removeOnPropertyChangedCallback(editModeListener)
    }
}