package com.ms8.smartirhub.android.custom_views.bottom_sheets

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableMap
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.andrefrsousa.superbottomsheet.SuperBottomSheetFragment
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.create_remote_profile.RemoteTemplateAdapter
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import com.ms8.smartirhub.android.models.firestore.RemoteProfileTemplate
import com.ms8.smartirhub.android.database.LocalData
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.databinding.VRemoteTemplatesSheetBinding
import com.ms8.smartirhub.android.firebase.FirestoreActions
import org.jetbrains.anko.windowManager
import kotlin.math.min


class RemoteTemplatesSheet : SuperBottomSheetFragment() {
    private var binding: VRemoteTemplatesSheetBinding? = null
    private var awaitingRemoteUID = ""
    var templateSheetCallback: RemoteTemplateSheetCallback? = null

/*
    ----------------------------------------------
        Listeners and Callbacks
    ----------------------------------------------
*/

    private val templateCallback = object : RemoteTemplateAdapter.RemoteTemplateAdapterCallback {
        override fun templateSelected(template: RemoteProfileTemplate) {
            showLoadingView()
            awaitingRemoteUID = template.remoteProfile
            FirestoreActions.getRemoteProfile(awaitingRemoteUID)
            LocalData.remoteProfiles.addOnMapChangedCallback(remoteProfileListener)
            checkForRemote(LocalData.remoteProfiles, awaitingRemoteUID)
        }
    }

    val remoteProfileListener = object : ObservableMap.OnMapChangedCallback<ObservableMap<String, RemoteProfile>, String, RemoteProfile>() {
        override fun onMapChanged(sender: ObservableMap<String, RemoteProfile>?, key: String?) {
            checkForRemote(sender, key)
        }
    }
    @SuppressLint("LogNotTimber")
    private fun checkForRemote(sender: ObservableMap<String, RemoteProfile>?, key: String?) {
        Log.d("RemoteTemplateSheet", "Checking for remote $key")
        sender?.let { newRemoteProfileMap ->
            newRemoteProfileMap.values.forEach { a -> Log.d("RemoteTemplateSheet", a.uid) }
            if (key == awaitingRemoteUID && newRemoteProfileMap.containsKey(key)) {
                Log.d("RemoteTemplateSheet", "found!")
                hideLoadingView()
                awaitingRemoteUID = ""
                templateSheetCallback?.onTemplateSelected(key)
                dismiss()
            }
        }
    }

    private val adapter = RemoteTemplateAdapter(templateCallback)

/*
    ----------------------------------------------
        Overridden Functions
    ----------------------------------------------
*/

    override fun getBackgroundColor() = ContextCompat.getColor(context!!, R.color.colorCardDark)
//
//    @Suppress("UNNECESSARY_SAFE_CALL")
//    override fun isSheetAlwaysExpanded()= binding?.root?.measuredHeight ?: 0 > getPeekHeight()
//
    override fun animateCornerRadius() = true

    override fun animateStatusBar() = true
//
//    override fun isSheetCancelable() = false
//
    override fun getPeekHeight(): Int {
        val displayMetrics = DisplayMetrics()
        displayMetrics.heightPixels = 0
        context?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        var height = displayMetrics.heightPixels
        height = if (height <= 0){
            500
        }
        else {
            height - (height * .05).toInt()
        }

        return min(height, binding?.hubList?.getCustomMaxHeight() ?: height)
    }


//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(requireContext(), theme).apply {
//        setOnShowListener {
//            BottomSheetBehavior.from<View>(findViewById(com.google.android.material.R.id.design_bottom_sheet)).state = BottomSheetBehavior.STATE_EXPANDED
//        }
//    }

//    override fun getTheme() = R.style.BottomSheetDialogTheme

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_REMOTE_UID, awaitingRemoteUID)
    }


    @SuppressLint("LogNotTimber")
    override fun onResume() {
        super.onResume()

        adapter.listen()
        // Check to see if we were waiting for a remoteProfile to load
        if (awaitingRemoteUID != "") {
            // If the remote was loaded during activity reload, we're done!
            if (LocalData.remoteProfiles.containsKey(awaitingRemoteUID)) {
                hideLoadingView()
                try { TempData.tempRemoteProfile = LocalData.remoteProfiles[awaitingRemoteUID]!! }
                catch (e : Exception) { Log.e("RemoteTemplateSheet", "$e") }
                awaitingRemoteUID = ""
                dismiss()
            // Otherwise, add the listener back and wait for a change
            } else {
                LocalData.remoteProfiles.addOnMapChangedCallback(remoteProfileListener)
                showLoadingView()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        adapter.stopListening()
        if (awaitingRemoteUID != "") {
            LocalData.remoteProfiles.removeOnMapChangedCallback(remoteProfileListener)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.v_remote_templates_sheet, container, false)

        // Restore state
        awaitingRemoteUID = savedInstanceState?.getString(KEY_REMOTE_UID, "") ?: ""

        // Start from scratch button
        binding!!.btnPos.setOnClickListener {
            TempData.tempRemoteProfile = RemoteProfile()
            awaitingRemoteUID = ""
            templateSheetCallback?.onTemplateSelected("")
            dismiss()
        }

        // Cancel button
        binding!!.btnNeg.setOnClickListener {
            dismiss()
        }

        // Template List
        binding!!.hubList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding!!.hubList.adapter = adapter

        //BottomSheetBehavior.from<View>(dialog?.findViewById(com.google.android.material.R.id.design_bottom_sheet)).state = BottomSheetBehavior.STATE_EXPANDED

        return binding!!.root
    }

/*
    ----------------------------------------------
        Layout Functions
    ----------------------------------------------
*/

    private fun showLoadingView() {
        binding!!.btnNeg.visibility = View.GONE
        binding!!.btnPos.startAnimation()
        binding!!.hubList.isEnabled = false
        binding!!.hubList.animate().alpha(0.1f).setDuration(500).setInterpolator(AccelerateDecelerateInterpolator()).start()
    }

    private fun hideLoadingView() {
        binding!!.btnNeg.visibility = View.VISIBLE
        binding!!.btnPos.revertAnimation()
        binding!!.hubList.isEnabled = true
        binding!!.hubList.animate().alpha(1f).setDuration(500).setInterpolator(AccelerateDecelerateInterpolator()).start()
    }



    fun onBackPressed(): Boolean {
        return if (awaitingRemoteUID != "") {
            LocalData.remoteProfiles.removeOnMapChangedCallback(remoteProfileListener)
            awaitingRemoteUID = ""
            hideLoadingView()
            true
        } else {
            false
        }
    }

/*
    ----------------------------------------------
        Static Stuff
    ----------------------------------------------
*/

    interface RemoteTemplateSheetCallback {
        fun onTemplateSelected(uid : String)
    }

    companion object {
        const val KEY_REMOTE_UID = "AWAITING_REMOTE_UID"
    }
}

