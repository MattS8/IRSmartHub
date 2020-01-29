package com.ms8.irsmarthub.remote_control.remote.models

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.ms8.irsmarthub.remote_control.button.models.Button

@IgnoreExtraProperties
data class Remote(
    var uid: String = "",
    var name: String = "",
    var owner: String = "",
    var ownerUsername: String = "",
    @get:Exclude
    var buttons: ArrayList<Button> = ArrayList()
) {

    fun toFirebaseObject() : Map<String, Any?> {
        return HashMap<String, Any?>()
            .apply {
                put("buttons", ArrayList<Map<String, Any?>>()
                    .apply {
                        buttons.forEach { b ->
                            add(b.toFirebaseObject())
                        }
                    })
                put("name", name)
                put("owner", owner)
                put("ownerUsername", ownerUsername)
            }
    }

    companion object {
        fun copyFrom(snapshot: DocumentSnapshot) : Remote? {
            val newRemote = snapshot.toObject(Remote::class.java)
                ?: return null


            // set uid
            newRemote.uid = snapshot.id

            // set buttons
            if (snapshot.contains("buttons")) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    (snapshot["buttons"] as List<Map<String, Any?>>).forEach { b ->
                        newRemote.buttons.add(Button.fromFirebaseObject(b))
                    }
                } catch (exception : Exception) { return null }
            }

            return newRemote
        }
    }
}