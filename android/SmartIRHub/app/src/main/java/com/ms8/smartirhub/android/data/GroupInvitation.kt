package com.ms8.smartirhub.android.data

import com.google.firebase.firestore.Exclude

class GroupInvitation {
    var owner: String = ""
    @get:Exclude lateinit var groupID : String
}