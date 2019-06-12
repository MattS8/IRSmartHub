package com.ms8.smartirhub.android.data

import androidx.databinding.ObservableArrayList
import com.google.firebase.firestore.Exclude

class User {
    var uid : String = ""
    var connectedDevices : ObservableArrayList<String> = ObservableArrayList()
    var remoteProfiles : ObservableArrayList<String> = ObservableArrayList()
    var groups : ObservableArrayList<String> = ObservableArrayList()

    @get:Exclude
    lateinit var username : String
}