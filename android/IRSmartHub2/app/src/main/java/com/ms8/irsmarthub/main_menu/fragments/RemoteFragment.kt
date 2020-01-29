package com.ms8.irsmarthub.main_menu.fragments

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableList
import com.ms8.irsmarthub.R
import com.ms8.irsmarthub.database.AppState
import com.ms8.irsmarthub.databinding.FRemoteCurrentBinding
import com.ms8.irsmarthub.main_menu.MainActivity
import com.ms8.irsmarthub.remote_control.button.models.Button
import com.ms8.irsmarthub.remote_control.command.models.Command
import com.ms8.irsmarthub.remote_control.remote.views.RemoteLayoutAdapter
import com.ms8.irsmarthub.remote_control.remote.views.asymmetric_gridview.Utils
import com.ms8.irsmarthub.utils.getActionBarSize

class RemoteFragment: MainFragment() {
    private var binding: FRemoteCurrentBinding? = null

/*
----------------------------------------------
    Layout Adapter
----------------------------------------------
*/
    private val remoteAdapter = RemoteLayoutAdapter()
    fun onButtonPressed(buttonPosition: Int, command: Command?) {
        val inEditMode = AppState.tempData.tempRemote.inEditMode.get() ?: false
        if (inEditMode)
        {
            //todo - show creation dialog with pressed button loaded OR new button
            Log.d("TEST", "Editing button $buttonPosition")
        } else
        {
            // todo - send button's command to hub
            //RealtimeDatabaseFunctions.sendCommandToHub(command)
            Log.d("TEST", "Sending command from button $buttonPosition")
        }
    }

/*
----------------------------------------------
    Overridden Functions
----------------------------------------------
*/
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FRemoteCurrentBinding.inflate(inflater, null, false)
        binding?.remoteLayout?.apply {
            activity?.getActionBarSize()?.let { topPadding = it }
            setupAdapter(remoteAdapter)
        }

        updateRemoteLayout()
        return binding!!.remoteLayoutRoot
    }

    override fun onResume() {
        super.onResume()

        // Listen for remote button presses
        remoteAdapter.setButtonPressedListener { buttonPosition, command -> onButtonPressed(buttonPosition, command) }

        // Dynamically show/hide the toolbar and action bar on scroll
        binding?.remoteLayout?.addOnScrollListener((activity as MainActivity).showHideUIElementsScrollListener)

        // Listen for changes to button layout
        AppState.tempData.tempRemote.buttons.addOnListChangedCallback(buttonLayoutChangeListener)

        // Sets the tempRemote to the user's favorite, or sets up a default favorite if they don't have one.
        // If a remote is already loaded, or there are no remotes fetched, this does nothing.
        AppState.setupTempRemote()
    }

    override fun onPause() {
        super.onPause()

        remoteAdapter.setButtonPressedListener { _, _ ->  }
        binding?.remoteLayout?.removeOnScrollListener((activity as MainActivity).showHideUIElementsScrollListener)

        // Stop listen for changes to button layout
        AppState.tempData.tempRemote.buttons.removeOnListChangedCallback(buttonLayoutChangeListener)
    }

    override fun newInstance(): MainFragment { return RemoteFragment()}

    fun updateRemoteLayout() {
        val newList = ArrayList<Button>()
        val inEditMode = AppState.tempData.tempRemote.inEditMode.get() ?: false
        AppState.tempData.tempRemote.buttons.toCollection(newList)
        remoteAdapter.buttons = newList
        remoteAdapter.inEditMode = inEditMode

        context?.let { c ->
            // Setup prompt
            val promptVisibility = if (newList.size == 0 && !inEditMode) View.VISIBLE else View.GONE
            val promptText1 = if (AppState.tempData.tempRemote.uid.get()?.isNotEmpty() == true)
                R.string.hint_add_buttons_to_remote
            else
                R.string.hint_create_new_remote
            val promptText2 = R.string.hint_add_buttons_to_remote_ending
            val promptDrawable = ContextCompat.getDrawable(
                c,
                if (AppState.tempData.tempRemote.uid.get()?.isNotEmpty() == true)
                    R.drawable.ic_mode_edit_white_24dp
                else
                R.drawable.ic_new_remote)
            binding?.txtCreateFirstRemoteP1?.apply {
                visibility = promptVisibility
                setText(promptText1)
                setCompoundDrawablesWithIntrinsicBounds(null, null, promptDrawable, null)
            }
            binding?.txtCreateFirstRemoteP2?.apply {
                visibility = promptVisibility
                setText(promptText2)
            }

            // Setup remote background
            binding?.remoteLayout?.backgroundTintList =
                if (inEditMode)
                    ContextCompat.getColorStateList(c, R.color.colorBgRemoteEditMode)
                else
                    ContextCompat.getColorStateList(c, R.color.colorBgRemote)
        }
    }

/*
----------------------------------------------
    Listeners
----------------------------------------------
*/
    private val buttonLayoutChangeListener = object
    : ObservableList.OnListChangedCallback<ObservableList<Button>>()
{
    override fun onChanged(sender: ObservableList<Button>?) { updateRemoteLayout() }

    override fun onItemRangeRemoved(s: ObservableList<Button>?, p: Int, i: Int) { updateRemoteLayout() }

    override fun onItemRangeMoved(s: ObservableList<Button>?, p: Int, tp: Int, i: Int) { updateRemoteLayout() }

    override fun onItemRangeInserted(s: ObservableList<Button>?, p: Int, i: Int) { updateRemoteLayout() }

    override fun onItemRangeChanged(s: ObservableList<Button>?, p: Int, i: Int) { updateRemoteLayout() }
}

/*
----------------------------------------------
    Companion Objects
----------------------------------------------
*/
    companion object {
        const val STATE = "com.ms8.irsmarthub.RemoteFragment.CUSTOM_STATE"
        const val recyclerViewTag = "myRemoteRV"

        internal class State(): Parcelable {
            var inEditMode: Boolean = false
                private set

            constructor(
                inEditMode: Boolean
            ): this() {
                this.inEditMode = inEditMode
            }

            // ---- Parcelable Implementation ---- //
            constructor(parcel: Parcel) : this() {
                inEditMode = parcel.readInt() == 1
            }

            override fun writeToParcel(parcel: Parcel, flags: Int) {
                parcel.writeInt(if (inEditMode) 1 else 0)
            }

            override fun describeContents() = 0

            companion object CREATOR : Parcelable.Creator<State> {
                override fun createFromParcel(parcel: Parcel): State {
                    return State(parcel)
                }

                override fun newArray(size: Int): Array<State?> {
                    return arrayOfNulls(size)
                }
            }

        }
    }
}