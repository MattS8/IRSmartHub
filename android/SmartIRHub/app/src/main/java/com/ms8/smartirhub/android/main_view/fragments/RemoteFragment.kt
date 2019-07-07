package com.ms8.smartirhub.android.main_view.fragments


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.create_button.CBWalkThroughActivity
import com.ms8.smartirhub.android.create_button.CBWalkThroughActivity.Companion.REQ_NEW_BUTTON
import com.ms8.smartirhub.android.custom_views.RemoteLayout
import com.ms8.smartirhub.android.database.LocalData
import com.ms8.smartirhub.android.databinding.FRemoteCurrentBinding

class RemoteFragment : MainViewFragment() {
    lateinit var binding: FRemoteCurrentBinding
    var waitingForCreateButtonActivity = false
    private var isShowingTemplateSheet = false

/*
    ----------------------------------------------
        Callbacks
    ----------------------------------------------
*/
    private val remoteLayoutCallback = object : RemoteLayout.RemoteLayoutButtonCallback {
        override fun createNewButton() {
            if (!waitingForCreateButtonActivity) {
                waitingForCreateButtonActivity = true
                startActivityForResult(Intent(activity, CBWalkThroughActivity::class.java), REQ_NEW_BUTTON)
            }
        }
    }

/*
    ----------------------------------------------
        Overridden Functions
    ----------------------------------------------
*/
    override fun newInstance(): MainViewFragment { return RemoteFragment() }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_WAITING_FOR_BUTTON_ACTIVITY, waitingForCreateButtonActivity)
        outState.putBoolean(KEY_IS_SHOWING_TEMPLATE, isShowingTemplateSheet)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_NEW_BUTTON) {
            // Only need to worry about enabling "create new button". The tempButton
            //  should already be added at the end of a successful process
            waitingForCreateButtonActivity = false
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.f_remote_current, container, false)
        // Restore state
        savedInstanceState?.let { state ->
            Log.d("RemoteFragment", "setting state...")
            waitingForCreateButtonActivity = state.getBoolean(KEY_WAITING_FOR_BUTTON_ACTIVITY)
            isShowingTemplateSheet = state.getBoolean(KEY_IS_SHOWING_TEMPLATE)
        }

        // Set up remote layout
        binding.remoteLayout.buttonCallback = remoteLayoutCallback
        binding.remoteLayout.defaultHub = LocalData.user?.defaultHub

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.remoteLayout.listen()
    }

    override fun onPause() {
        super.onPause()
        binding.remoteLayout.stopListening()
    }

    override fun toString() = "Remote Fragment"

    companion object {
        const val KEY_WAITING_FOR_BUTTON_ACTIVITY = "KEY_BTN_A"
        const val KEY_IS_SHOWING_TEMPLATE = "KEY_SHOW_TEMPLATE"
    }
}

