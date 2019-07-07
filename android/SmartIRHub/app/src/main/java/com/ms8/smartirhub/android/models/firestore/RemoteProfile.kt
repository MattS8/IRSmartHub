package com.ms8.smartirhub.android.models.firestore

import androidx.databinding.Observable
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.ms8.smartirhub.android.models.firestore.RemoteProfile.Button.Companion.ADD_TO_END
import com.ms8.smartirhub.android.models.firestore.RemoteProfile.Button.Companion.ID_BUTTONS
import com.ms8.smartirhub.android.models.firestore.RemoteProfile.Button.Companion.ID_NAME

@IgnoreExtraProperties
class RemoteProfile: Observable {
    val buttons             : ObservableArrayList<Button>                       = ObservableArrayList()
    var uid                 : String                                            = ""
    var name                : String                                            = ""
    set(value) { field = value; notifyCallbacks(ID_NAME) }

    @get:Exclude
    var inEditMode          : ObservableBoolean                                 = ObservableBoolean().apply { set(false) }

    private val callbacks   : ArrayList<Observable.OnPropertyChangedCallback>   = ArrayList()

/*
 ----------------------------------------------
    Observable Functions
 ----------------------------------------------
 */

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {
        callbacks.remove(callback)
    }

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {
        callback?.let { callbacks.add(it) }
    }

    fun addButton(button: Button, position: Int = ADD_TO_END) {
        if (position == ADD_TO_END) {
            buttons.add(button)
        } else {
            buttons[position] = button
        }
        notifyCallbacks(ID_BUTTONS)
    }

    fun removeButton(position: Int) {
        buttons.removeAt(position)
        notifyCallbacks(ID_BUTTONS)
    }

    private fun notifyCallbacks(id : Int) {
        callbacks.forEach { cb -> cb.onPropertyChanged(this, id) }
    }

/*
----------------------------------------------
   Remote Classes
----------------------------------------------
*/

    class Button {
        var name    : String    = ""
        var style   : Int       = 1
        var command : Command =
            Command()

        companion object {
            const val STYLE_CREATE_BUTTON = 0
            const val STYLE_SQUARE_BUTTON = 1
            const val STYLE_ROUND_BUTTON = 2
            const val STYLE_VERTICAL_RECT_TOP_ROUNDED = 3
            const val STYLE_VERTICAL_RECT_BOT_ROUNDED = 4

            const val ID_BUTTONS = 80839
            const val ID_NAME = 80840

            const val ADD_TO_END = -1
        }
    }

    class Command {
        var actions : ObservableArrayList<Action> = ObservableArrayList()

        class Action {
            var hubUID      : String    =
                DEFAULT_HUB
            var irSignal    : String    = ""
            var delay       : Int       = 0
        }

        companion object {
            const val DEFAULT_HUB = "_default_hub_"
        }
    }
}