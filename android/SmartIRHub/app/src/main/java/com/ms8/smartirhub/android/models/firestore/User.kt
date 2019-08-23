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

@Suppress("UNCHECKED_CAST")
@IgnoreExtraProperties
data class User(
    var defaultHub  : String                        = "",
    var favRemote   : String                        = "",
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
                put("favRemote", favRemote)
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

    fun clear() {
        defaultHub = ""
        favRemote = ""
        uid.set("")
        username.set("")
        groups.clear()
        hubs.clear()
        irSignals.clear()
        remotes.clear()
    }

    @SuppressLint("LogNotTimber")
    fun copyFromSnapshot(snapshot: DocumentSnapshot) {
        // set default hub
        defaultHub = snapshot["defaultHub"] as String

        favRemote = snapshot["favRemote"] as String

        // set username
        username.set(snapshot.id)

        // set uid
        uid.set(FirebaseAuth.getInstance().currentUser?.uid ?: "")

        // add hubs
        if (snapshot.contains("hubs")) {
            try {
                hubs.clear()
                hubs.addAll(snapshot["hubs"] as Collection<String>)
            } catch (e : Exception) {
                Log.e("User", "$e")
            }
        }

        // add irSignals
        if (snapshot.contains("irSignals")) {
            try {
                irSignals.clear()
                irSignals.addAll(snapshot["hubs"] as Collection<String>)
            } catch (e : Exception) {
                Log.e("User", "$e")
            }
        }

        // add remotes
        if (snapshot.contains("remotes")) {
            try {
                remotes.clear()
                remotes.addAll(snapshot["remotes"] as Collection<String>)
                Log.d("TEST", "found some remotes (${remotes.size})")
            } catch (e : Exception) {
                Log.e("user", "$e")
            }
        }
    }
}
