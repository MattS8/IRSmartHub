package com.ms8.smartirhub.android.data

import android.util.Log
import androidx.databinding.ObservableArrayList
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

class User(val uid: String, @Exclude val username: String) {
    var groups : ObservableArrayList<String> = ObservableArrayList()
    var irSignals : ObservableArrayList<String> = ObservableArrayList()
    var defaultHub = ""
}