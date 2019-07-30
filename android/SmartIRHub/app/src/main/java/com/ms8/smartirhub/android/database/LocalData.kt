package com.ms8.smartirhub.android.database

import androidx.databinding.ObservableArrayMap
import com.ms8.smartirhub.android.models.firestore.IrSignal
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import com.ms8.smartirhub.android.models.firestore.Group
import com.ms8.smartirhub.android.models.firestore.Hub
import com.ms8.smartirhub.android.models.firestore.RemoteProfileTemplate
import com.ms8.smartirhub.android.models.firestore.User

object LocalData {
    val groups                  : ObservableArrayMap<String, Group>                 = ObservableArrayMap()
    val hubs                    : ObservableArrayMap<String, Hub>                   = ObservableArrayMap()
    val remoteProfiles          : ObservableArrayMap<String, RemoteProfile>         = ObservableArrayMap()
    val remoteProfileTemplates  : ObservableArrayMap<String, RemoteProfileTemplate> = ObservableArrayMap()
    val signals                 : ObservableArrayMap<String, IrSignal>              = ObservableArrayMap()
    var user                    : User?                                             = null


    fun removeUserData() {
        user = null
        groups.clear()
        remoteProfiles.clear()
        hubs.clear()
        signals.clear()
    }
}