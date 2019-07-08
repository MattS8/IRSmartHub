package com.ms8.smartirhub.android.models.firestore

import androidx.databinding.ObservableArrayList
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
class User {
    var uid         : String                        = ""
    var defaultHub  : String                        = ""

    @get: Exclude
    var groups      : ObservableArrayList<String>   = ObservableArrayList()
    @get: Exclude
    var username    : String                        = ""
}