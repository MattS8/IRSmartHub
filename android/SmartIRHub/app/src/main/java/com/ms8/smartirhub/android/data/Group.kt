package com.ms8.smartirhub.android.data

import android.databinding.ObservableArrayList
import android.databinding.ObservableArrayMap
import android.databinding.ObservableMap

class Group(val uid : String, val owner : String) {
    val remoteProfiles : ObservableArrayList<String> = ObservableArrayList()
    val connectedHubs : ObservableArrayList<String> = ObservableArrayList()
    val users : ObservableMap<String, Permissions> = ObservableArrayMap<String, Permissions>()

    class Permissions {
        val addDevices = false
        val addRemoteProfiles = false
        val addUsers = false
        val removeDevices = false
        val removeRemoteProfiles = false
        val removeUsers = false
    }
}


