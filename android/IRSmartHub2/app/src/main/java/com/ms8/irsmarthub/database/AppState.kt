package com.ms8.irsmarthub.database

import android.util.Log
import androidx.databinding.ObservableArrayMap
import androidx.databinding.ObservableField
import com.google.firebase.firestore.FirebaseFirestore
import com.ms8.irsmarthub.database.models.TempRemote
import com.ms8.irsmarthub.database.models.TempUser
import com.ms8.irsmarthub.hub.models.Hub
import com.ms8.irsmarthub.remote_control.command.models.IrSignal
import com.ms8.irsmarthub.remote_control.remote.models.Remote
import com.ms8.irsmarthub.user.models.User

object AppState {
    val tempData = TempData()
    val userData = UserData()
    val errorData = ErrorData()

    data class TempData(
        val tempRemote: TempRemote = TempRemote(),
        val tempUser: TempUser = TempUser()
    ) {
        fun resetAllData() {
            tempRemote.copyFrom(null)
            tempUser.copyFrom(null)
        }
    }

    data class UserData(
        val remotes: ObservableArrayMap<String, Remote> = ObservableArrayMap(),
        val hubs: ObservableArrayMap<String, Hub> = ObservableArrayMap(),
        val signals: ObservableArrayMap<String, IrSignal> = ObservableArrayMap()
    ) {
        fun resetAllData() {
            remotes.clear()
            hubs.clear()
            signals.clear()
        }
    }

    data class ErrorData(
        val fetchUserDataError: ObservableField<Exception?> = ObservableField(),
        val signInError: ObservableField<Exception?> = ObservableField()
    )


/*
------------------------------------------------
    General Functions
-----------------------------------------------
*/

    fun setupTempRemote() {
        if (tempData.tempRemote.uid.get()?.isNotEmpty() == true)
            return

        // User has not defined a "favorite remote" -> try and define one for them
        val favRemoteUID = tempData.tempUser.favRemote.get()
        if (favRemoteUID?.isNotEmpty() == true) {
            val favRemote = userData.remotes[favRemoteUID]
            if (favRemote == null) {
                Log.e(TAG, "(setupTempRemote) - User's favorite remote was not fetched! " +
                        "(uid = $favRemoteUID)")
                setupDefaultFavoriteRemote()
                return
            }
            tempData.tempRemote.copyFrom(favRemote)
        } else {
            setupDefaultFavoriteRemote()
        }
    }

    private fun setupDefaultFavoriteRemote() {
        if (userData.remotes.size == 0)
            return

        val favRemote = userData.remotes[userData.remotes.keys.elementAt(0)]
        favRemote?.let { remote ->
            tempData.tempUser.favRemote.set(remote.uid)
            tempData.tempRemote.copyFrom(favRemote)

            FirestoreFunctions.User.setUser()
        }
    }

    private const val TAG = "AppState"
}