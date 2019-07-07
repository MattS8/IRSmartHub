package com.ms8.smartirhub.android.models.firestore

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
class RemoteProfileTemplate {
    var name            : String = ""
    var previewURL      : String = ""
    var remoteProfile   : String = ""

    @get:Exclude
    var uid             : String = ""
}