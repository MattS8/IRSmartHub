package com.ms8.smartirhub.android.models.firestore

import android.annotation.SuppressLint
import android.util.Log
import androidx.databinding.ObservableArrayMap
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
    val users           : ObservableArrayMap<String, String> = ObservableArrayMap()
) {
    override fun equals(other: Any?): Boolean {
        return other != null && other is Hub && uid == other.uid
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + uid.hashCode()
        return result
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        const val DEFAULT_HUB = "_default_hub_"

        @SuppressLint("LogNotTimber")
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