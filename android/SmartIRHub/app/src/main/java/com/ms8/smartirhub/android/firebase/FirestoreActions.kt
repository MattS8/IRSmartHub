package com.ms8.smartirhub.android.firebase

import android.util.ArrayMap
import android.util.Log
import androidx.databinding.ObservableArrayList
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.ms8.smartirhub.android.data.Group
import com.ms8.smartirhub.android.data.RemoteProfile
import com.ms8.smartirhub.android.data.User
import com.ms8.smartirhub.android.database.LocalData

object FirestoreActions {
    var userListener : ListenerRegistration? = null
    var groupListeners = ArrayMap<String, ListenerRegistration>()
    var remoteProfileListener : ListenerRegistration? = null


    fun getUser(username: String): Task<DocumentSnapshot> {
        return FirebaseFirestore.getInstance().collection("users").document(username).get()
    }

    fun getUserFromUID(): Task<QuerySnapshot> {
        return FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("uid", FirebaseAuth.getInstance().currentUser!!.uid).get()
    }

    /**
     *  Creates an entry in the database for newly created user.
     */
    fun createNewUser(username: String) : Task<Void> {
        return FirebaseFirestore.getInstance().collection("users").document(username)
            .set(User().apply { FirebaseAuth.getInstance().currentUser!!.uid }, SetOptions.merge())
    }

    fun reportError(errMsg: String) {
        //TODO Report Error
        Log.e("T#", "Todo: Report error $errMsg")
    }

    fun listenToUserData() {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        userListener = FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, exception ->
                when {
                    exception != null -> { Log.e("listenToUserData", "snapshot error ($exception)") }
                    snapshot == null || !snapshot.isEmpty -> {Log.d("listenToUserData", "User data is null")}
                    else -> {
                        if (snapshot.size() > 1)
                            Log.w("listenToUserData", "Received more than one user object from uid: $uid")

                        val userData = snapshot.documents[0].toObject(User::class.java)
                        if (LocalData.user == null) {
                            Log.d("listToUserData", "Adding all user data")
                            LocalData.setupUser(userData!!, uid)
                            return@addSnapshotListener
                        }
                        // Only update list of remote profiles if changed
                        if (!listsAreEqual(LocalData.user!!.remoteProfiles, userData!!.remoteProfiles)) {
                            LocalData.user!!.remoteProfiles.clear()
                            LocalData.user!!.remoteProfiles.addAll(userData.remoteProfiles)
                        }
                        // Only update group list if changed
                        if (!listsAreEqual(LocalData.user!!.groups, userData.groups)) {
                            LocalData.user!!.groups.clear()
                            LocalData.user!!.groups.addAll(userData.groups)
                        }
                        // Only update connected devices if changed
                        if (!listsAreEqual(LocalData.user!!.connectedDevices, userData.connectedDevices)) {
                            LocalData.user!!.connectedDevices.clear()
                            LocalData.user!!.connectedDevices.addAll(userData.connectedDevices)
                        }
                    }
                }
            }
    }

    private fun listsAreEqual(local: ObservableArrayList<String>, remoteProfiles: ObservableArrayList<String>): Boolean {
        local.forEach {
            if (!remoteProfiles.contains(it))
                return false
        }

        return true
    }

    fun listenToRemoteProfiles() {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid

        if (remoteProfileListener != null){
            Log.w("listenToRemoteProfiles", "Tried listening while a listener was already subscribed!")
            remoteProfileListener?.remove().also { remoteProfileListener = null }
        }

        remoteProfileListener = FirebaseFirestore.getInstance().collection("remoteProfiles")
            .whereArrayContains("users", uid)
            .addSnapshotListener{snapshot, e ->
                when {
                    e != null -> { Log.e("listenToRemoteProfiles", "$e") }
                    else -> {
                        Log.d("listenToRemoteProfiles", "Received <${snapshot?.size()}> remoteProfiles from db")
                        for (doc in snapshot!!) {
                            val remoteProfile = doc.toObject(RemoteProfile::class.java)
                            LocalData.remoteProfiles.remove(doc.id)
                            LocalData.remoteProfiles[doc.id] = remoteProfile
                        }
                    }
                }
            }
    }

    fun listenToUserGroups() {
        val groups = LocalData.user?.groups ?: arrayListOf<String>()
        groups.forEach {
            if (!groupListeners.containsKey(it)) {
                groupListeners[it] = FirebaseFirestore.getInstance().collection("groups").document(it)
                    .addSnapshotListener {snapshot, e ->
                        when {
                            // Error
                            e != null -> Log.e("listenToUserGroups", "$e")
                            // Group no longer exists
                            snapshot == null || !snapshot.exists() -> {
                                Log.d("listenToUserGroups", "User removed from nonexistent group: ${snapshot?.id}")
                                LocalData.userGroups.remove(snapshot?.id)
                            }
                            // Received group
                            else -> {
                                Log.d("listenToUserGroups", "Received Group: ${snapshot.data}")
                                        LocalData.userGroups.remove(snapshot.id)
                                LocalData.userGroups[snapshot.id] = Group(snapshot.id, snapshot.data?.get("owner") as String)
                            }
                        }

                    }
            } else {
                Log.w("listenToUserGroups", "tried to listen on group <$it> while already subscribed... skipping...")
            }
        }
    }

    fun removeAllListeners() {
        userListener?.remove()
    }



    //UNUSED
//    fun listenToGroups() {
//        val uid = FirebaseAuth.getInstance().currentUser!!.uid
//
//        FirebaseFirestore.getInstance().collection("groups")
//            .whereArrayContains("users", uid)
//            .addSnapshotListener{snapshot, e ->
//                if (e != null) {
//                    Log.e("listenToGroups", "Error getting group ($e)")
//                } else {
//                    for (doc in snapshot!!) {
//                        val group = doc.toObject(Group::class.java)
//                        LocalData.userGroups.remove(doc.id)
//                        LocalData.userGroups[doc.id] = group
//                    }
//                }
//            }
//    }
}