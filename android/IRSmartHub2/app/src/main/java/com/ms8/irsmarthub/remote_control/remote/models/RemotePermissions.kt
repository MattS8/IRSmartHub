package com.ms8.irsmarthub.remote_control.remote.models

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude

data class RemotePermissions (
    var permission: PermissionLevel = PermissionLevel.READ,
    var username: String = "",
    @get:Exclude
    var uid: String = ""
) {
    companion object {
        fun copyFrom(snapshot: DocumentSnapshot?): RemotePermissions? {
            if (snapshot == null)
                return null
            try {
                return RemotePermissions(
                    PermissionLevel.valueOf(snapshot["permission"] as String),
                    snapshot["username"] as String,
                    snapshot.id
                )
            } catch (e: Exception) {
                Log.e(TAG, "(copyFrom) - Error: $e")
            }

            return null
        }

        enum class PermissionLevel {READ, READ_WRITE, FULL_ACCESS}
        private const val TAG = "RemotePermissions"
    }
}