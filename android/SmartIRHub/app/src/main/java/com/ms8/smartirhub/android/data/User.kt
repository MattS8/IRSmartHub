package com.ms8.smartirhub.android.data

import android.databinding.ObservableArrayList

class User(val uid : String) {
    val connectedDevices : ObservableArrayList<String> = ObservableArrayList()
    val remoteProfiles : ObservableArrayList<String> = ObservableArrayList()


}