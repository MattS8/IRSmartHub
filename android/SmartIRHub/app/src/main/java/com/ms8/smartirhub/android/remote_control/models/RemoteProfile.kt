package com.ms8.smartirhub.android.remote_control.models

import android.annotation.SuppressLint
import android.util.ArrayMap
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.Observable
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import com.andrognito.flashbar.Flashbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.models.firestore.Hub.Companion.DEFAULT_HUB
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.ADD_TO_END
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.ID_BUTTONS
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.ID_NAME
import com.ms8.smartirhub.android.utils.MyValidators
import com.ms8.smartirhub.android.utils.MyValidators.isValidRemoteName
import com.ms8.smartirhub.android.utils.extensions.getGenericErrorFlashbar

@IgnoreExtraProperties
class RemoteProfile: Observable {
    @get: Exclude
    val buttons             : ObservableArrayList<Button>                       = ObservableArrayList()
    @get: Exclude
    var uid                 : String                                            = ""
    var name                : String                                            = ""
    set(value) { field = value; notifyCallbacks(ID_NAME) }
    var owner               : String                                            = ""

    @get:Exclude
    var inEditMode          : ObservableBoolean                                 = ObservableBoolean().apply { set(false) }
    @get:Exclude
    var isCreatingNewButton : ObservableBoolean                                 = ObservableBoolean().apply { set(false) }

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

    fun toFirebaseObject() : Map<String, Any?> {
        return HashMap<String, Any?>()
            .apply {
                put("buttons", ArrayList<Map<String, Any?>>()
                    .apply {
                        buttons.forEach { b ->
                            add(b.toFirebaseObject())
                        }
                    })
                put("name", name)
                put("owner", owner)
            }
    }

    fun copyFrom(remoteProfile: RemoteProfile?) {
        remoteProfile?.let {
            uid = it.uid
            name = it.name
            owner = it.owner
            buttons.clear()
            buttons.addAll(it.buttons)
        }
    }

    /**
     * Saves remote changes to firebase. If activity is not null, error messages will be
     * displayed via a Flashbar.
     */
    fun saveRemote(activity : AppCompatActivity? = null) : Boolean {
        return when {
            // show error if name is missing
            name.isEmpty() -> {
                activity?.showRemoteNameEmptyFlashbar()
                false
            }

            // show error if name is invalid
            !name.isValidRemoteName() -> {
                activity?.showInvalidRemoteNameFlashbar()
                false
            }

            // begin "save remote" task
            else -> {
                if (AppState.tempData.tempRemoteProfile.uid.isEmpty()) {
                    // create new remote
                    FirestoreActions.addRemote()
                } else {
                    // update existing remote
                    FirestoreActions.updateRemote()
                }
                true
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        /**
         * Parses remote from firestore snapshot.
         */
        @SuppressLint("LogNotTimber")
        fun fromSnapshot(snapshot: DocumentSnapshot) : RemoteProfile {
            val newRemote = snapshot.toObject(RemoteProfile::class.java)
                ?: RemoteProfile()

            // set uid
            newRemote.uid = FirebaseAuth.getInstance().currentUser!!.uid

            // set buttons
            if (snapshot.contains("buttons")) {
                try {
                    (snapshot["buttons"] as List<Map<String, Any?>>).forEach { b ->
                        newRemote.buttons.add(Button.fromMap(b))
                    }
                } catch (exception : Exception) { Log.e("Remote", "$exception") }
            }

            return newRemote
        }
    }

/*
----------------------------------------------
   Remote Classes
----------------------------------------------
*/

    @Suppress("UNCHECKED_CAST")
    class Button {
        var properties      : ArrayList<Properties> = ArrayList<Properties>().apply { add(Properties()) }
        var commands        : ArrayList<Command>    = ArrayList<Command>().apply { add(Command()) }
        var name            : String                = ""
        var style           : Int                   = STYLE_BTN_SINGLE_ACTION
        var columnSpan      : Int                   = 1
        var rowSpan         : Int                   = 1


        fun toFirebaseObject() : Map<String, Any?> {
            return HashMap<String, Any?>()
                .apply {
                    put("properties", ArrayList<Map<String, Any?>>()
                        .apply {
                            properties.forEach { p ->
                                add(p.toFirebaseObject())
                            }
                        })
                    put("commands", ArrayList<Map<String, Any?>>()
                        .apply {
                            commands.forEach { c ->
                                add(c.toFirebaseObject())
                            }
                        })
                    put("name", name)
                    put("style", style)
                    put("columnSpan", columnSpan)
                    put("rowSpan", rowSpan)
                }
        }

        companion object {
            fun newCommandList(): java.util.ArrayList<Command> = ArrayList<Command>().apply { add(Command()) }

            @SuppressLint("LogNotTimber")
            fun fromMap(buttonMap: Map<String, Any?>): Button {
                val newButton = Button()
                    .apply {
                        name = buttonMap["name"] as String
                        style = (buttonMap["style"] as Number).toInt()
                        rowSpan = (buttonMap["rowSpan"] as Number).toInt()
                        columnSpan = (buttonMap["columnSpan"] as Number).toInt()
                    }

                // set properties
                if (buttonMap.containsKey("properties")) {
                    try {
                        newButton.properties.clear()
                        (buttonMap["properties"] as List<Map<String, Any?>>).forEach { p ->
                            newButton.properties.add(Properties.fromMap(p))
                        }
                    } catch (e : Exception) { Log.e("Button", "$e") }
                }

                // set commands
                if (buttonMap.containsKey("commands")) {
                    try {
                        (buttonMap["commands"] as List<Map<String, Any?>>).forEach { c ->
                            newButton.commands.add(Command.fromMap(c))
                        }
                    } catch (e : Exception) { Log.e("Button", "$e") }
                }

                return newButton
            }

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

            fun toFirebaseObject() : Map<String, Any?> {
                return ArrayMap<String, Any?>()
                    .apply {
                        put("bgStyle", bgStyle.ordinal)
                        put("bgUrl", bgUrl)
                        put("image", image)
                        put("marginBottom", marginBottom)
                        put("marginTop", marginTop)
                        put("marginStart", marginStart)
                        put("marginEnd", marginEnd)
                    }
            }

            companion object {
                fun fromMap(propertiesMap: Map<String, Any?>) : Properties {
                    return Properties()
                        .apply {
                            if (propertiesMap.containsKey("bgUrl")) {
                                bgUrl = propertiesMap["bgUrl"] as String
                            }

                            if (propertiesMap.containsKey("image")) {
                                image = propertiesMap["image"] as String
                            }

                            marginEnd = (propertiesMap["marginEnd"] as Number).toInt()
                            marginBottom = (propertiesMap["marginBottom"] as Number).toInt()
                            marginTop = (propertiesMap["marginTop"] as Number).toInt()
                            marginStart = (propertiesMap["marginStart"] as Number).toInt()

                            bgStyle = toBgStyle((propertiesMap["bgStyle"] as Number).toInt())
                        }
                }

                @SuppressLint("LogNotTimber")
                fun toBgStyle(intVal : Int) : BgStyle {
                   return when(intVal) {
                        BgStyle.BG_INVISIBLE.ordinal -> BgStyle.BG_INVISIBLE
                        BgStyle.BG_CIRCLE.ordinal -> BgStyle.BG_CIRCLE
                        BgStyle.BG_ROUND_RECT.ordinal -> BgStyle.BG_ROUND_RECT
                        BgStyle.BG_ROUND_RECT_TOP.ordinal -> BgStyle.BG_ROUND_RECT_TOP
                        BgStyle.BG_ROUND_RECT_BOTTOM.ordinal -> BgStyle.BG_ROUND_RECT_BOTTOM
                        BgStyle.BG_CUSTOM_IMAGE.ordinal -> BgStyle.BG_CUSTOM_IMAGE
                        BgStyle.BG_NONE.ordinal -> BgStyle.BG_NONE

                        else -> {
                            Log.e("BgStyle", "Received unknown int value ($intVal)")
                            BgStyle.BG_NONE
                        }
                    }
                }
            }


        }
    }

    @Suppress("UNCHECKED_CAST")
    class Command {
        var actions : ObservableArrayList<Action> = ObservableArrayList()

        fun toFirebaseObject() : Map<String, Any?> {
            return HashMap<String, Any?>()
                .apply {
                    put("actions", ArrayList<Map<String, Any?>>()
                        .apply {
                            actions.forEach { a ->
                                add(a.toFirebaseObject())
                            }
                        })
                }
        }

        companion object {
            fun fromMap(commandMap : Map<String, Any?>) : Command {
                return Command()
                    .apply {
                        if (commandMap.containsKey("actions")) {
                            (commandMap["actions"] as List<Map<String, Any?>>).forEach { a ->
                                actions.add(Action.fromMap(a))
                            }
                        }
                    }
            }
        }

        class Action {
            var hubUID      : String    = DEFAULT_HUB
            var irSignal    : String    = ""
            var delay       : Int       = 0

            fun toFirebaseObject() : Map<String, Any?> {
                return HashMap<String, Any?>()
                    .apply {
                        put("hubUID", hubUID)
                        put("irSignal", irSignal)
                        put("delay", delay)
                    }
            }

            companion object {
                fun fromMap(actionMap : Map<String, Any?>) : Action {
                    return Action()
                        .apply {
                            hubUID = actionMap["hubUID"] as String
                            irSignal = actionMap["irSignal"] as String
                            delay = (actionMap["delay"] as Number).toInt()
                        }
                }
            }
        }
    }
}

/*
----------------------------------------------
   Extensions
----------------------------------------------
*/

fun AppCompatActivity.getRemoteNameErrorString() : String {
    return "${getString(R.string.remote_names_must_be)} ${MyValidators.MIN_REMOTE_NAME_LENGTH} - ${MyValidators.MAX_REMOTE_NAME_LENGTH} ${getString(R.string.and_no_characters)}"
}

 fun AppCompatActivity?.showRemoteNameEmptyFlashbar() {
     this?.let {
         getGenericErrorFlashbar(true)
             .message(getString(R.string.err_empty_remote_name))
             .build()
             .show()
     }
}

fun AppCompatActivity?.showInvalidRemoteNameFlashbar() {
    this?.let {
        getGenericErrorFlashbar(true)
            .message(getRemoteNameErrorString())
            .build()
            .show()
    }
}

fun AppCompatActivity?.showUnknownRemoteSaveError() {
    this?.let {
        getGenericErrorFlashbar(true)
            .message(getString(R.string.err_unknown_save_remote))
            .build()
            .show()
    }
}

