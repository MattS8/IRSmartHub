package com.ms8.smartirhub.android.database

import androidx.databinding.Observable
import androidx.databinding.ObservableArrayMap
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.firebase.RealtimeDatabaseFunctions
import com.ms8.smartirhub.android.models.firestore.IrSignal
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import com.ms8.smartirhub.android.models.firestore.Group
import com.ms8.smartirhub.android.models.firestore.Hub
import com.ms8.smartirhub.android.models.firestore.RemoteProfileTemplate
import com.ms8.smartirhub.android.models.firestore.User
import com.ms8.smartirhub.android.remote_control.button.models.Button
import java.lang.Exception

object AppState {
    val userData : UserData = UserData()
    val tempData : TempData = TempData()
    val errorData : ErrorData = ErrorData()

    data class UserData(
        val groups           : ObservableArrayMap<String, Group>                 = ObservableArrayMap(),
        val hubs             : ObservableArrayMap<String, Hub>                   = ObservableArrayMap(),
        val remotes          : ObservableArrayMap<String, RemoteProfile>         = ObservableArrayMap(),
        val remoteTemplates  : ObservableArrayMap<String, RemoteProfileTemplate> = ObservableArrayMap(),
        val irSignals        : ObservableArrayMap<String, IrSignal>              = ObservableArrayMap(),
        val user             : User                                              = User()
    ) {

        fun removeData() {
            user.clear()
            groups.clear()
            remotes.clear()
            hubs.clear()
            irSignals.clear()

            FirestoreActions.removeAllListeners()
        }

        fun hasFetchedUserData(): Boolean {
            return hubs.size == user.hubs.size
                    && remotes.size == user.remotes.size
                    && irSignals.size == user.irSignals.size
        }
    }

    data class TempData (
        val tempRemoteProfile   : RemoteProfile              = RemoteProfile(),
        var tempButton          : ObservableField<Button>    = ObservableField(),
        var tempCommand         : RemoteProfile.Command?     = null,
        var tempSignal          : ObservableField<IrSignal?> = ObservableField(),
        var isCreatingNewButton : ObservableBoolean          = ObservableBoolean(false)
    )

    data class ErrorData (
        var userSignInError : ObservableField<Exception?> = ObservableField(),
        var remoteSaveError : ObservableField<Exception?> = ObservableField(),
        var pairSignalError : ObservableField<RealtimeDatabaseFunctions.HubException?> = ObservableField(),
        var saveSignalError : ObservableField<Exception?> = ObservableField()
    )

    fun resetTempRemote() {
        tempData.tempRemoteProfile.uid = ""
        tempData.tempRemoteProfile.name = ""
        tempData.tempRemoteProfile.inEditMode.set(false)
        tempData.tempRemoteProfile.buttons.clear()
    }
}