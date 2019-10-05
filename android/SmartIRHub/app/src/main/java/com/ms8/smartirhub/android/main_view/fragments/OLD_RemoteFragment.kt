package com.ms8.smartirhub.android.main_view.fragments


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.databinding.*
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.FRemoteCurrentBinding
import com.ms8.smartirhub.android.main_view.MainViewActivity
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import android.util.DisplayMetrics
import android.util.Log
import com.ms8.smartirhub.android.main_view.fragments.OLD_RemoteFragment.Companion.LayoutState.*
import com.ms8.smartirhub.android.utils.RequestCodes


class OLD_RemoteFragment : MainFragment() {
    lateinit var binding: FRemoteCurrentBinding
    var state : State =
        State()
    var screenHeight = 800

    val remotesListener = object : ObservableMap.OnMapChangedCallback<ObservableArrayMap<String, RemoteProfile>, String, RemoteProfile>() {
        override fun onMapChanged(sender: ObservableArrayMap<String, RemoteProfile>?, key: String?) { determineState() }
    }
    val editModeListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) { determineState() }
    }
    val createNewButtonListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            if (AppState.tempData.tempRemoteProfile.isCreatingNewButton.get()) {
                //startActivityForResult(Intent(activity, CBWalkThroughActivity::class.java), REQ_NEW_BUTTON)
            }
        }
    }

    private fun determineState(forceUpdate : Boolean = false) {
        // Get new state
        val newLayoutState = when {
            AppState.userData.remotes.size == 0 && !AppState.tempData.tempRemoteProfile.inEditMode.get() -> SHOW_CREATE_FIRST_REMOTE
            else -> SHOW_FAV_REMOTE
        }

        // Only animate change if state has actually changed or we're forcing an update
        if (newLayoutState != state.layoutState || forceUpdate) {
            // update state
            state.layoutState = newLayoutState

            when (state.layoutState) {
                SHOW_CREATE_FIRST_REMOTE -> {
                    binding.txtCreateFirstRemoteP1.animate()
                        .alpha(1f)
                        .setInterpolator(DecelerateInterpolator())
                        .setDuration(300)
                        .start()
                    binding.txtCreateFirstRemoteP2.animate()
                        .alpha(1f)
                        .setInterpolator(DecelerateInterpolator())
                        .setDuration(300)
                        .start()
                    binding.remoteLayout.animate()
                        .translationY(screenHeight.toFloat())
                        .setInterpolator(AccelerateInterpolator())
                        .setDuration(300)
                        .start()
                }

                SHOW_FAV_REMOTE -> {
                    binding.txtCreateFirstRemoteP1.animate()
                        .alpha(0f)
                        .setInterpolator(DecelerateInterpolator())
                        .setDuration(300)
                        .start()
                    binding.txtCreateFirstRemoteP2.animate()
                        .alpha(0f)
                        .setInterpolator(DecelerateInterpolator())
                        .setDuration(300)
                        .start()
                    binding.remoteLayout.animate()
                        .translationY(0f)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .setDuration(300)
                        .start()
                }
            }
        }
    }

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
    override fun newInstance(): MainFragment { return OLD_RemoteFragment()
}

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(REMOTE_FRAG_STATE, state)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCodes.BUTTON_SETUP) {
            // Only need to worry about enabling "create new button". The tempButton
            //  should already be added at the end of a successful process
            AppState.tempData.tempRemoteProfile.isCreatingNewButton.set(false)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.f_remote_current, container, false)

        // Restore state
        state = savedInstanceState?.getParcelable<State>(
            REMOTE_FRAG_STATE
        ) ?: state
            .apply {
                // check for create remote prompt
                layoutState = if (AppState.userData.remotes.size == 0 && !AppState.tempData.tempRemoteProfile.inEditMode.get())
                    SHOW_CREATE_FIRST_REMOTE
                else
                    SHOW_FAV_REMOTE
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


        // Get screen height
        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)?.let {
            screenHeight = displayMetrics.heightPixels
        }

        // show/hide remote background and 'create remote' text
        when (state.layoutState) {
            SHOW_CREATE_FIRST_REMOTE -> {
                binding.txtCreateFirstRemoteP1.alpha = 1f
                binding.txtCreateFirstRemoteP2.alpha = 1f
                binding.remoteLayout.translationY = screenHeight.toFloat()
            }
            SHOW_FAV_REMOTE -> {
                binding.txtCreateFirstRemoteP1.alpha = 0f
                binding.txtCreateFirstRemoteP2.alpha = 0f
                binding.remoteLayout.translationY = 0f
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.remoteLayout.startListening()
        AppState.userData.remotes.addOnMapChangedCallback(remotesListener)
        AppState.tempData.tempRemoteProfile.inEditMode.addOnPropertyChangedCallback(editModeListener)
        AppState.tempData.tempRemoteProfile.isCreatingNewButton.addOnPropertyChangedCallback(createNewButtonListener)
    }

    override fun onPause() {
        super.onPause()
        binding.remoteLayout.stopListening()
        AppState.userData.remotes.removeOnMapChangedCallback(remotesListener)
        AppState.tempData.tempRemoteProfile.inEditMode.removeOnPropertyChangedCallback(editModeListener)
        AppState.tempData.tempRemoteProfile.isCreatingNewButton.removeOnPropertyChangedCallback(createNewButtonListener)
    }

    override fun toString() = "Remote Fragment"

    companion object {
        enum class LayoutState {SHOW_CREATE_FIRST_REMOTE, SHOW_FAV_REMOTE}
        const val REMOTE_FRAG_STATE = "REMOTE_FRAG_STATE"

        @SuppressLint("LogNotTimber")
        fun toLayoutState(intVal : Int) : LayoutState {
            return when (intVal) {
                LayoutState.SHOW_CREATE_FIRST_REMOTE.ordinal -> LayoutState.SHOW_CREATE_FIRST_REMOTE
                LayoutState.SHOW_FAV_REMOTE.ordinal -> LayoutState.SHOW_FAV_REMOTE
                else -> {
                    Log.e("RemoteFragment", "Unknown int val conversion to layout state: $intVal")
                    LayoutState.SHOW_FAV_REMOTE
                }
            }
        }

        class State() : Parcelable {
            var isShowingTemplateSheet = false
            var layoutState = LayoutState.SHOW_CREATE_FIRST_REMOTE

            constructor(parcel: Parcel) : this() {
                isShowingTemplateSheet = parcel.readByte() != 0.toByte()
                layoutState =
                    toLayoutState(
                        parcel.readInt()
                    )
            }

            override fun writeToParcel(parcel: Parcel, flags: Int) {
                parcel.writeByte(if (isShowingTemplateSheet) 1 else 0)
                parcel.writeInt(layoutState.ordinal)
            }

            override fun describeContents(): Int {
                return 0
            }

            companion object CREATOR : Parcelable.Creator<State> {
                override fun createFromParcel(parcel: Parcel): State {
                    return State(
                        parcel
                    )
                }

                override fun newArray(size: Int): Array<State?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}

