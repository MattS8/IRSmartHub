package com.ms8.smartirhub.android.models.firestore

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
class GroupInvitation {
    var owner   : String = ""

    @get:Exclude
    var groupID : String = ""
}