package com.ms8.smartirhub.android.data

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
class RemoteProfileTemplate {
    var name = ""
    var previewURL = ""
    var remoteProfile = ""

    @get:Exclude
    var uid = ""
}