package com.ms8.smartirhub.android.database

import androidx.databinding.ObservableArrayMap
import com.ms8.smartirhub.android.data.*

object LocalData {
    var user : User? = null
    val userGroups = ObservableArrayMap<String, Group>()
    val remoteProfiles = ObservableArrayMap<String, RemoteProfile>()
    val hubs = ObservableArrayMap<String, Hub>()
    val irSignals = ObservableArrayMap<String, IrSignal>()

    val remoteProfileTemplates = ObservableArrayMap<String, RemoteProfileTemplate>()

    fun removeUserData() {
        user = null
        userGroups.clear()
        remoteProfiles.clear()
        hubs.clear()
        irSignals.clear()
    }

//    fun userDataReady(): Boolean {
//        var userDataReady = user != null && user!!.groups.size == LocalData.userGroups.size
//        when (userDataReady) {
//            false -> return false
//            true -> {
//                remoteProfiles.forEach { entry ->
//                    entry.value.
//                }
//            }
//        }
//        return
//    }
}