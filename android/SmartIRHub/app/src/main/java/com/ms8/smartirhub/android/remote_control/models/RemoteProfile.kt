package com.ms8.smartirhub.android.remote_control.models

import androidx.databinding.Observable
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.ms8.smartirhub.android.models.firestore.Hub.Companion.DEFAULT_HUB
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.ADD_TO_END
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.ID_BUTTONS
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.ID_NAME

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
        var properties      : ArrayList<Properties> = ArrayList<Properties>().apply { add(Properties()) }
        var command         : ArrayList<Command>    = ArrayList<Command>().apply { add(Command()) }
        var name            : String                = ""
        var style           : Int                   = STYLE_BTN_SINGLE_ACTION
        var columnSpan      : Int       = 1
        var rowSpan         : Int       = 1


        class Properties {
            var bgStyle         : BgStyle   = BgStyle.BG_CIRCLE
            var bgUrl           : String    = ""
            var image           : String    = ""
            var marginBottom    : Int       = 16
            var marginTop       : Int       = 16
            var marginStart     : Int       = 16
            var marginEnd       : Int       = 16

            enum class BgStyle {
                BG_INVISIBLE,
                BG_CIRCLE,
                BG_ROUND_RECT,
                BG_ROUND_RECT_TOP,
                BG_ROUND_RECT_BOTTOM,
                BG_CUSTOM_IMAGE,
                BG_NONE
            }
        }

        companion object {
            fun newCommandList(): java.util.ArrayList<Command> = ArrayList<Command>().apply { add(Command()) }

            const val STYLE_CREATE_BUTTON = 0
            const val STYLE_SPACE = 1
            const val STYLE_BTN_SINGLE_ACTION = 2
            const val STYLE_BTN_NO_MARGIN = 3
            const val STYLE_BTN_INCREMENTER_VERTICAL = 4
            const val STYLE_BTN_RADIAL_W_CENTER = 5
            const val STYLE_BTN_RADIAL = 6

            const val ID_BUTTONS = 80839
            const val ID_NAME = 80840

            const val ADD_TO_END = -1

            const val IMG_ADD           = "_IMG_ADD_"
            const val IMG_SUBTRACT      = "_IMG_SUBTRACT_"
            const val IMG_RADIAL_LEFT   = "_IMG_RADIAL_LEFT"
            const val IMG_RADIAL_UP     = "_IMG_RADIAL_UP"
            const val IMG_RADIAL_DOWN   = "_IMG_RADIAL_DOWN"
            const val IMG_RADIAL_RIGHT  = "_IMG_RADIAL_RIGHT"
        }
    }

    class Command {
        var actions : ObservableArrayList<Action> = ObservableArrayList()

        class Action {
            var hubUID      : String    = DEFAULT_HUB
            var irSignal    : String    = ""
            var delay       : Int       = 0
        }
    }
}