package com.ms8.smartirhub.android.main_view.fragments


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.FRemoteCurrentBinding
import com.ms8.smartirhub.android.main_view.MainViewActivity
import com.ms8.smartirhub.android.main_view.fragments.OLD_RemoteFragment.Companion.LayoutState.SHOW_CREATE_FIRST_REMOTE
import com.ms8.smartirhub.android.main_view.fragments.OLD_RemoteFragment.Companion.LayoutState.SHOW_FAV_REMOTE
import com.ms8.smartirhub.android.remote_control.button.models.Button
import com.ms8.smartirhub.android.remote_control.command.creation.GetFromRemoteActivity
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import com.ms8.smartirhub.android.remote_control.views.RemoteLayout
import com.ms8.smartirhub.android.utils.RequestCodes
import com.ms8.smartirhub.android.utils.extensions.getGenericNotificationFlashbar


class OLD_RemoteFragment : MainFragment() {
    val remoteLayout  = RemoteLayout()
    var binding : FRemoteCurrentBinding? = null
    var state : State = State()
    private var screenHeight = 800

/*
-----------------------------------------------
    Listener Hell Below
-----------------------------------------------
*/

    // Is called when a remote has been added to AppState
    private val remoteCreationCompleteListener : (Button) -> Unit = { button : Button ->
        activity?.let {
            if (it !is AppCompatActivity)
                return@let
            when (button.type) {
                Button.Companion.ButtonStyle.STYLE_SPACE -> it.getGenericNotificationFlashbar(getString(R.string.added_space_button))
                    .build().show()
                else -> it.getGenericNotificationFlashbar(getString(R.string.added_button))
                    .build().show()
            }
        }
    }

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
                    remoteLayout.binding?.txtCreateFirstRemoteP1?.animate()
                        ?.alpha(1f)
                        ?.setInterpolator(DecelerateInterpolator())
                        ?.setDuration(300)
                        ?.start()
                    remoteLayout.binding?.txtCreateFirstRemoteP2?.animate()
                        ?.alpha(1f)
                        ?.setInterpolator(DecelerateInterpolator())
                        ?.setDuration(300)
                        ?.start()
                    remoteLayout.binding?.remoteLayout?.animate()
                        ?.translationY(screenHeight.toFloat())
                        ?.setInterpolator(AccelerateInterpolator())
                        ?.setDuration(300)
                        ?.start()
                }

                SHOW_FAV_REMOTE -> {
                    remoteLayout.binding?.txtCreateFirstRemoteP1?.animate()
                        ?.alpha(0f)
                        ?.setInterpolator(DecelerateInterpolator())
                        ?.setDuration(300)
                        ?.start()
                    remoteLayout.binding?.txtCreateFirstRemoteP2?.animate()
                        ?.alpha(0f)
                        ?.setInterpolator(DecelerateInterpolator())
                        ?.setDuration(300)
                        ?.start()
                    remoteLayout.binding?.remoteLayout?.animate()
                        ?.translationY(0f)
                        ?.setInterpolator(AccelerateDecelerateInterpolator())
                        ?.setDuration(300)
                        ?.start()
                }
            }
        }
    }

/*
    ----------------------------------------------
        Overridden Functions
    ----------------------------------------------
*/
    override fun newInstance(): MainFragment { return OLD_RemoteFragment() }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(REMOTE_FRAG_STATE, state)
    }

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
            onCreationComplete = remoteCreationCompleteListener
            onRequestCommandFromRemote = requestCommandFromRemoteListener
            onRequestActionsFromRemote = requestActionFromRemoteListener
        }

        // Restore state
        state = savedInstanceState?.getParcelable(
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
            remoteLayout.topPadding = TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)

        // Set scroll listener to notify when to hide UI Elements
        remoteLayout.binding?.remoteLayout?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
        remoteLayout.binding = binding
        remoteLayout.buttonCreator.apply {
            onCreationComplete = remoteCreationCompleteListener
            onRequestCommandFromRemote = requestCommandFromRemoteListener
            onRequestActionsFromRemote = requestActionFromRemoteListener
        }
    }

    override fun onPause() {
        super.onPause()
        remoteLayout.stopListening()
        remoteLayout.binding = null
        remoteLayout.buttonCreator.apply {
            onCreationComplete = {}
            onRequestCommandFromRemote = {}
            onRequestActionsFromRemote = {}
        }
    }

    override fun toString() = "Remote Fragment"

    companion object {
        enum class LayoutState {SHOW_CREATE_FIRST_REMOTE, SHOW_FAV_REMOTE}
        const val REMOTE_FRAG_STATE = "REMOTE_FRAG_STATE"

        @SuppressLint("LogNotTimber")
        fun toLayoutState(intVal : Int) : LayoutState {
            return when (intVal) {
                SHOW_CREATE_FIRST_REMOTE.ordinal -> SHOW_CREATE_FIRST_REMOTE
                SHOW_FAV_REMOTE.ordinal -> SHOW_FAV_REMOTE
                else -> {
                    Log.e("RemoteFragment", "Unknown int val conversion to layout state: $intVal")
                    SHOW_FAV_REMOTE
                }
            }
        }

        class State() : Parcelable {
            var isShowingTemplateSheet = false
            var layoutState = SHOW_CREATE_FIRST_REMOTE

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

