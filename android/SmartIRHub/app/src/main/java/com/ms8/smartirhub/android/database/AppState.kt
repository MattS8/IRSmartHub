package com.ms8.smartirhub.android.database

import androidx.databinding.ObservableArrayMap
import androidx.databinding.ObservableField
import com.ms8.smartirhub.android.main_view.MainViewActivity
import com.ms8.smartirhub.android.models.firestore.IrSignal
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import com.ms8.smartirhub.android.models.firestore.Group
import com.ms8.smartirhub.android.models.firestore.Hub
import com.ms8.smartirhub.android.models.firestore.RemoteProfileTemplate
import com.ms8.smartirhub.android.models.firestore.User
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
        }
    }

    data class TempData (
        var tempRemoteProfile   : RemoteProfile = RemoteProfile(),
        var tempButton          : RemoteProfile.Button? = null,
        var tempSignal          : IrSignal?             = null
    )

    data class ErrorData (
        var userSignInError : ObservableField<Exception?> = ObservableField()
    )

}