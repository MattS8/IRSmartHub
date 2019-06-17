package com.ms8.smartirhub.android.data

import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableArrayMap
import androidx.databinding.ObservableMap
import com.google.firebase.firestore.Exclude

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
class Group(val owner : String, val personalGroup: Boolean) {
    @get:Exclude
    val remoteProfiles : ObservableArrayList<String> = ObservableArrayList()
    @get:Exclude
    val connectedDevices : ObservableArrayList<String> = ObservableArrayList()
    @get:Exclude
    val users : ObservableMap<String, Permissions> = ObservableArrayMap<String, Permissions>()

    @get:Exclude
    var uid  = ""

    class Permissions {
        val addDevices = false
        val addRemoteProfiles = false
        val addUsers = false
        val removeDevices = false
        val removeRemoteProfiles = false
        val removeUsers = false
    }
}


