package com.ms8.smartirhub.android.models.firestore

import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableArrayMap
import androidx.databinding.ObservableMap
import com.google.firebase.firestore.Exclude

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
class Group {
    var personalGroup       : Boolean                                   = false
    var owner               : String                                    = ""

    @get:Exclude
    var uid                 : String                                    = ""

    //Firestore Collections
    @get:Exclude
    val remoteProfiles      : ObservableArrayList<String>               = ObservableArrayList()
    @get:Exclude
    val connectedDevices    : ObservableArrayList<String>               = ObservableArrayList()
    @get:Exclude
    val users               : ObservableMap<String, UserPermissions>    = ObservableArrayMap()


    class UserPermissions {
        var addDevices = false
        var addRemoteProfiles = false
        var addUsers = false
        var removeDevices = false
        var removeRemoteProfiles = false
        var removeUsers = false
    }
}


