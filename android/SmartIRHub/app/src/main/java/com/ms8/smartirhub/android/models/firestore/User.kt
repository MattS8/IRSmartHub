package com.ms8.smartirhub.android.models.firestore

import android.annotation.SuppressLint
import android.util.Log
import androidx.databinding.Observable
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import java.lang.Exception

@IgnoreExtraProperties
data class User(
    var defaultHub  : String                        = "",
    @get: Exclude
    var uid         : ObservableField<String>       = ObservableField(""),
    @get: Exclude
    var groups      : ObservableArrayList<String>   = ObservableArrayList(),
    @get: Exclude
    val username    : ObservableField<String>       = ObservableField(""),
    @get: Exclude
    var hubs        : ObservableArrayList<String>   = ObservableArrayList(),
    @get: Exclude
    var irSignals   : ObservableArrayList<String>   = ObservableArrayList(),
    @get: Exclude
    var remotes     : ObservableArrayList<String>   = ObservableArrayList()
) {

    override fun toString(): String {
        return "USER: uid = ${uid.get()}, defaultHub = $defaultHub, groups (size) = ${groups.size}, username = ${username.get()}, " +
                "hubs (size) = ${hubs.size}, irSignals (size) = ${irSignals.size}, remotes (size) = ${remotes.size}"
    }

    fun toFirebaseObject() : Map<String, Any?> {
        return HashMap<String, Any?>()
            .apply {
                put("uid", uid.get())
                put("defaultHub", defaultHub)
                put("hubs", ArrayList<String>()
                    .apply {
                        addAll(hubs)
                    })
                put("irSignals", ArrayList<String>()
                    .apply {
                        addAll(irSignals)
                    })
                put("remotes", ArrayList<String>()
                    .apply {
                        addAll(remotes)
                    })
            }
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        @SuppressLint("LogNotTimber")
        fun fromSnapshot(snapshot: DocumentSnapshot) : User {
            val newUser = snapshot.toObject(User::class.java)
                ?: User()

            // set username
            newUser.username.set(snapshot.id)

            // set uid
            newUser.uid.set(FirebaseAuth.getInstance().currentUser?.uid ?: "")

            // add hubs
            if (snapshot.contains("hubs")) {
                try {
                    newUser.hubs = ObservableArrayList<String>()
                        .apply {
                            addAll(snapshot["hubs"] as Collection<String>)
                        }
                } catch (e : Exception) {
                   Log.e("User", "$e")
                }
            }

            // add irSignals
            if (snapshot.contains("irSignals")) {
                try {
                    newUser.irSignals = ObservableArrayList<String>()
                        .apply {
                            addAll(snapshot["hubs"] as Collection<String>)
                        }
                } catch (e : Exception) {
                    Log.e("User", "$e")
                }
            }

            // add remotes
            if (snapshot.contains("remotes")) {
                try {
                    newUser.remotes = ObservableArrayList<String>()
                        .apply {
                            addAll(snapshot["remotes"] as Collection<String>)
                        }
                } catch (e : Exception) {
                    Log.e("user", "$e")
                }
            }

            return  newUser
        }
    }
}
