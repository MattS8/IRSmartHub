package com.ms8.irsmarthub.hub.models

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import java.lang.Exception

@IgnoreExtraProperties
data class HubPermissions(
    var permission: PermissionLevel = PermissionLevel.READ,
    var username: String = "",
    @get:Exclude
    var uid: String = ""
    ) {

    companion object {
        fun copyFrom(snapshot: DocumentSnapshot?): HubPermissions? {
            if (snapshot == null)
                return null
            try {
                return HubPermissions(
                    PermissionLevel.valueOf(snapshot["permission"] as String),
                    snapshot["username"] as String,
                    snapshot.id
                )
            } catch (e: Exception) {
                Log.e("HubPermissions", "(copyFrom) - Error: $e")
            }

            return null
        }

        enum class PermissionLevel {READ, READ_WRITE, FULL_ACCESS}
    }
}