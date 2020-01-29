package com.ms8.irsmarthub.database.models

import android.util.Log
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.ms8.irsmarthub.database.AppState
import com.ms8.irsmarthub.user.models.User
import java.lang.Exception

class TempUser(
    var defaultHub: ObservableField<String> = ObservableField(""),
    var favRemote: ObservableField<String> = ObservableField(""),
    var uid: ObservableField<String> = ObservableField(""),
    var username: ObservableField<String> = ObservableField(""),
    var hubs: ObservableArrayList<String> = ObservableArrayList(),
    var remotes: ObservableArrayList<String> = ObservableArrayList()
) {

    fun copyFrom(user: User?) {
        val copiedUser = user ?: User()

        defaultHub.set(copiedUser.defaultHub)
        favRemote.set(copiedUser.favRemote)
        uid.set(copiedUser.uid)
        username.set(copiedUser.username)
        hubs.clear()
        hubs.addAll(copiedUser.hubs)
        remotes.clear()
        remotes.addAll(copiedUser.remotes)
    }

    @Suppress("UNCHECKED_CAST")
    fun copyFrom(snapshot: DocumentSnapshot) : Exception? {

        try {
            defaultHub.set(snapshot["defaultHub"] as String)
            favRemote.set(snapshot["favRemote"] as String)
            Log.d("TEST", "Username: ${username.get()} -> ${snapshot.id}")
            username.set(snapshot.id)
            uid.set(FirebaseAuth.getInstance().currentUser!!.uid)
            // add hubs
            if (snapshot.contains("hubs")) {
                hubs.clear()
                hubs.addAll(snapshot["hubs"] as Collection<String>)
            }
            // add remotes
            if (snapshot.contains("remotes")) {
                remotes.clear()
                remotes.addAll(snapshot["remotes"] as Collection<String>)
            }
        } catch (e : Exception) {
            return e
        }

        return null
    }

    fun hasFetchedInitialUserData(): Boolean {
        Log.d("TEST", "checking if all user data has been fetched..." +
                "\n\tuid: ${uid.get()} (${AppState.tempData.tempUser})" +
                "\n\thubs.size: ${hubs.size} (${AppState.userData.hubs.size})" +
                "\n\tremotes.size: ${remotes.size} (${AppState.userData.remotes.size})")
        return uid.get()?.isNotEmpty() == true
                && username.get()?.isNotEmpty() == true
                && hubs.size == AppState.userData.hubs.size
                && remotes.size == AppState.userData.remotes.size
    }

    fun toUser(): User {
        return User(
            defaultHub.get().toString(),
            favRemote.get().toString(),
            uid.get().toString(),
            username.get().toString(),
            hubs,
            remotes
        )
    }
}