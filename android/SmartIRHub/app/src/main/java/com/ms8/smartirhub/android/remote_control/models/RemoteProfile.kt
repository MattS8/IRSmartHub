package com.ms8.smartirhub.android.remote_control.models

import android.annotation.SuppressLint
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.Observable
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.models.firestore.Hub
import com.ms8.smartirhub.android.models.firestore.Hub.Companion.DEFAULT_HUB
import com.ms8.smartirhub.android.models.firestore.IrSignal
import com.ms8.smartirhub.android.remote_control.button.creation.ButtonCreator.Companion.NEW_BUTTON
import com.ms8.smartirhub.android.remote_control.button.models.Button
import com.ms8.smartirhub.android.remote_control.button.models.Button.Companion.ADD_TO_END
import com.ms8.smartirhub.android.remote_control.button.models.Button.Companion.ID_BUTTONS
import com.ms8.smartirhub.android.remote_control.button.models.Button.Companion.ID_NAME
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
    var ownerUsername       : String                                            = ""

    @get:Exclude
    var inEditMode          : ObservableBoolean                                 = ObservableBoolean().apply { set(false) }
    @get:Exclude
    var isCreatingNewButton : ObservableBoolean                                 = ObservableBoolean().apply { set(false) }

    @get:Exclude
    var newButtonPosition   : Int                                               = NEW_BUTTON
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

    private fun notifyCallbacks(id : Int) { callbacks.forEach { cb -> cb.onPropertyChanged(this, id) } }

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

    fun copyFrom(remoteProfile: RemoteProfile?) {
        remoteProfile?.let {
            uid = it.uid
            name = it.name
            owner = it.owner
            ownerUsername = it.ownerUsername
            buttons.clear()
            buttons.addAll(it.buttons)
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
            newRemote.uid = snapshot.id

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

        @SuppressLint("LogNotTimber")
        fun permissionFromString(string: String) : RemoteProfile.PermissionType {
            return when (string) {
                "READ" -> {
                    PermissionType.READ
                }
                "READ_WRITE" -> {
                    PermissionType.READ_WRITE
                }
                "FULL_ACCESS" -> {
                    PermissionType.FULL_ACCESS
                }
                else -> {
                    Log.w("Remote", "Unknown permission string: $string")
                    PermissionType.READ
                }
            }
        }

        fun permissionToString(permission: PermissionType) : String {
            return when (permission) {
                PermissionType.READ -> "READ"
                PermissionType.READ_WRITE -> "READ_WRITE"
                PermissionType.FULL_ACCESS -> "FULL_ACCESS"
            }
        }
    }

/*
----------------------------------------------
   Remote Classes
----------------------------------------------
*/

    class Permission(val permission: PermissionType, val username: String) {
        override fun toString(): String {
            return when (permission) {
                PermissionType.READ -> "READ"
                PermissionType.READ_WRITE -> "READ_WRITE"
                PermissionType.FULL_ACCESS -> "FULL_ACCESS"
            }
        }
    }

    enum class PermissionType {READ, READ_WRITE, FULL_ACCESS}

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

        class Action(
            var hubUID : String = DEFAULT_HUB,
            var irSignal : String = "",
            var delay : Int = 0) {

            fun toFirebaseObject() : Map<String, Any?> {
                return HashMap<String, Any?>()
                    .apply {
                        put("hubUID", hubUID)
                        put("irSignal", irSignal)
                        put("delay", delay)
                    }
            }

            fun getCachedIrSignal(): IrSignal {
                return AppState.userData.irSignals[irSignal] ?: IrSignal()
            }

            fun getCachedHub(): Hub {
                return AppState.userData.hubs[hubUID] ?: AppState.userData.hubs[AppState.userData.user.defaultHub] ?: Hub()
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

