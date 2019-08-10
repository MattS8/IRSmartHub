package com.ms8.smartirhub.android.models.firestore

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.QueryDocumentSnapshot

@IgnoreExtraProperties
class RemoteProfileTemplate {
    var name            : String = ""
    var previewURL      : String = ""
    var remoteProfile   : String = ""

    @get:Exclude
    var uid             : String = ""

    companion object {
        fun fromSnapshot(snapshot: QueryDocumentSnapshot) : RemoteProfileTemplate {
            return snapshot.toObject(RemoteProfileTemplate::class.java)
                .apply {
                    uid = snapshot.id
                }
        }
    }
}