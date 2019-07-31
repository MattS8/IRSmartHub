package com.ms8.smartirhub.android.remote_control


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.create_button.CBWalkThroughActivity.Companion.REQ_NEW_BUTTON
import com.ms8.smartirhub.android.databinding.FRemoteCurrentBinding
import com.ms8.smartirhub.android.main_view.MainViewActivity
import com.ms8.smartirhub.android.main_view.fragments.MainFragment

class RemoteFragment : MainFragment() {
    lateinit var binding: FRemoteCurrentBinding
    var waitingForCreateButtonActivity = false
    private var isShowingTemplateSheet = false

/*
    ----------------------------------------------
        Callbacks
    ----------------------------------------------
*/
//    private val remoteLayoutCallback = object : _OLD_RemoteLayout.RemoteLayoutButtonCallback {
//        override fun createNewButton() {
//            if (!waitingForCreateButtonActivity) {
//                waitingForCreateButtonActivity = true
//                startActivityForResult(Intent(activity, CBWalkThroughActivity::class.java), REQ_NEW_BUTTON)
//            }
//        }
//    }

/*
    ----------------------------------------------
        Overridden Functions
    ----------------------------------------------
*/
    override fun newInstance(): MainFragment { return RemoteFragment()
}

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

        // Set top padding to account for toolbar
        val tv = TypedValue()
        if (activity?.theme?.resolveAttribute(android.R.attr.actionBarSize, tv, true) == true)
            binding.remoteLayout.topPadding = TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)

        // Set scroll listener to notify when to hide UI Elements
        binding.remoteLayout.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                activity?.let {
                    if (dy > 0) {
                        (it as MainViewActivity).hideUiElements()
                    } else if (dy < 0) {
                        (it as MainViewActivity).showUiElements()
                    }
                }
            }
        })

        // Set up remote adapter
        binding.remoteLayout.setupAdapter()

        // Set up remote layout
        //binding.remoteLayout.buttonCallback = remoteLayoutCallback

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.remoteLayout.startListening()
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

