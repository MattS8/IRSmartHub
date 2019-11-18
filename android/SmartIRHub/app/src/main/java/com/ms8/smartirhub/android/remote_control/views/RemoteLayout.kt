package com.ms8.smartirhub.android.remote_control.views

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.Observable
import androidx.databinding.ObservableList
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.FRemoteCurrentBinding
import com.ms8.smartirhub.android.remote_control.button.models.Button

class RemoteLayout(context: Context) {
    val binding: FRemoteCurrentBinding = FRemoteCurrentBinding.inflate(LayoutInflater.from(context), null, false)
    private var isListening : Boolean = false

    fun getRemoteView(): View = binding.root

    private val editModeListener  =  object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) { remoteLayoutAdapter.notifyDataSetChanged() }
    }
    private val remoteLayoutAdapter = RemoteLayoutView.RemoteLayoutAdapter()


    private val buttonListener = object : ObservableList.OnListChangedCallback<ObservableList<Button>>() {
        override fun onChanged(sender: ObservableList<Button>?) {
            binding.remoteLayout.adapter?.notifyDataSetChanged()
            updateRemoteLayoutAdapter()
            checkPromptVisibility()
        }

        override fun onItemRangeRemoved(sender: ObservableList<Button>?, positionStart: Int, itemCount: Int) {
            remoteLayoutAdapter.notifyItemRangeRemoved(positionStart, itemCount)
            updateRemoteLayoutAdapter()
            checkPromptVisibility()
        }

        override fun onItemRangeMoved(sender: ObservableList<Button>?, fromPosition: Int, toPosition: Int, itemCount: Int) {
            binding.remoteLayout.adapter?.notifyDataSetChanged()
            checkPromptVisibility()
            updateRemoteLayoutAdapter()
        }

        override fun onItemRangeInserted(sender: ObservableList<Button>?, positionStart: Int, itemCount: Int) {
            binding.remoteLayout.adapter?.notifyItemRangeInserted(positionStart, itemCount)
            checkPromptVisibility()
            updateRemoteLayoutAdapter()
        }

        override fun onItemRangeChanged(sender: ObservableList<Button>?, positionStart: Int, itemCount: Int) {
            binding.remoteLayout.adapter?.notifyItemRangeChanged(positionStart, itemCount)
            checkPromptVisibility()
            updateRemoteLayoutAdapter()
        }
    }

    // todo - this is a quick fix to a bug where the adapter doesn't adhere to itemCount
    private fun updateRemoteLayoutAdapter() {
        binding.remoteLayout.setupAdapter(remoteLayoutAdapter)
        binding.remoteLayout.invalidate()
    }

    private fun checkPromptVisibility() {
        if (AppState.tempData.tempRemoteProfile.buttons.size == 0 && !AppState.tempData.tempRemoteProfile.inEditMode.get()) {
            binding.txtCreateFirstRemoteP1.visibility = View.VISIBLE
            binding.txtCreateFirstRemoteP2.visibility = View.VISIBLE
        } else {
            binding.txtCreateFirstRemoteP1.visibility = View.GONE
            binding.txtCreateFirstRemoteP2.visibility = View.GONE
        }
    }

    fun setupAdapter() = binding.remoteLayout.setupAdapter(remoteLayoutAdapter).also {
        checkPromptVisibility()
    }

    fun startListening() {
        if (!isListening) {
            isListening = true
            AppState.tempData.tempRemoteProfile.buttons.addOnListChangedCallback(buttonListener)
            AppState.tempData.tempRemoteProfile.inEditMode.addOnPropertyChangedCallback(editModeListener)
        }
    }

    fun stopListening() {
        AppState.tempData.tempRemoteProfile.buttons.removeOnListChangedCallback(buttonListener)
        AppState.tempData.tempRemoteProfile.inEditMode.removeOnPropertyChangedCallback(editModeListener)
    }

    fun setTopPadding(padding: Int) {
        binding.remoteLayout.topPadding = padding
    }
}