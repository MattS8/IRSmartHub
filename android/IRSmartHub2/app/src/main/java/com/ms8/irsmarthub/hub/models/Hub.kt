package com.ms8.irsmarthub.hub.models

import android.util.ArrayMap
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import java.lang.Exception

@IgnoreExtraProperties
data class Hub(
    var name            : String = "",
    var owner           : String = "",
    var ownerUsername   : String = "",
    @Exclude
    var uid             : String = "",
    @get: Exclude
    val users           : ArrayMap<String, String> = ArrayMap()
) {

    fun toFirebaseObject() : Map<String, Any?> {
        return HashMap<String, Any?>()
            .apply {
                put("name", name)
                put("owner", owner)
                put("ownerUsername", ownerUsername)
                put("users", users)
            }
    }

    companion object {
        const val DEFAULT_HUB = "_default_hub_"

        @Suppress("UNCHECKED_CAST")
        fun fromSnapshot(snapshot: DocumentSnapshot) : Hub {
            val newHub = snapshot.toObject(Hub::class.java)
                ?: Hub()

            // set uid
            newHub.uid = snapshot.id

            // set users
            if (snapshot.contains("users")) {
                try {
                    newHub.users.putAll(snapshot["users"] as Map<out String, String>)
                } catch (exception : Exception) {
                    Log.e("Hub", "$exception")
                }
            }

            return newHub
        }
    }
}