package com.ms8.smartirhub.android.data

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
class Hub {
    var name = ""
    var owner = ""

    @get:Exclude
    var uid = ""
}