package com.ms8.smartirhub.android.data

import androidx.databinding.Observable
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.ms8.smartirhub.android.data.RemoteProfile.Button.Companion.ID_BUTTONS
import com.ms8.smartirhub.android.data.RemoteProfile.Button.Companion.ID_NAME

@IgnoreExtraProperties
class RemoteProfile: Observable {
    private val callbacks = ArrayList<Observable.OnPropertyChangedCallback>()
    val buttons = ObservableArrayList<Button>()
    var uid = ""
    var name = ""
    set(value) {
        field = value
        callbacks.forEach { cb -> cb.onPropertyChanged(this, ID_NAME) }
    }

    @get:Exclude
    var inEditMode = ObservableBoolean().apply { set(false) }

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


    fun numberOfButtons() = buttons.size

    fun addButton(button: Button, position: Int) {
        if (position == -1) {
            buttons.add(button)
        } else {
            buttons[position] = button
        }
        callbacks.forEach { cb -> cb.onPropertyChanged(this, ID_BUTTONS) }
    }

    fun removeButton(position: Int) {
        buttons.removeAt(position)
        callbacks.forEach { cb -> cb.onPropertyChanged(this, ID_BUTTONS) }
    }

    class Button {
        var name = ""
        var style = 1
        var command = Command()

        companion object {
            const val STYLE_CREATE_BUTTON = 0
            const val STYLE_SQUARE_BUTTON = 1
            const val STYLE_ROUND_BUTTON = 2
            const val STYLE_VERTICAL_RECT_TOP_ROUNDED = 3
            const val STYLE_VERTICAL_RECT_BOT_ROUNDED = 4

            const val ID_BUTTONS = 80839
            const val ID_NAME = 80840
        }
    }
}