package com.ms8.smartirhub.android.firebase

import android.util.ArrayMap
import android.util.Log
import androidx.databinding.ObservableArrayList
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.gson.Gson
import com.ms8.smartirhub.android.data.*
import com.ms8.smartirhub.android.database.LocalData
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.firebase.FirebaseConstants.IR_RES_OVERFLOW_ERR
import com.ms8.smartirhub.android.firebase.FirebaseConstants.IR_RES_RECEIVED_SIG
import com.ms8.smartirhub.android.firebase.FirebaseConstants.IR_RES_TIMEOUT_ERR
import com.ms8.smartirhub.android.firebase.FirebaseConstants.IR_RES_UNKNOWN_ERR
import java.lang.Exception

object FirestoreActions {
    private var userListener : ListenerRegistration? = null
    private var groupListeners = ArrayMap<String, ListenerRegistration>()
    private var remoteProfileListeners = ArrayMap<String, ListenerRegistration>()
    private var hubListeners = ArrayMap<String, ListenerRegistration>()
    private var irSignalsListener : ListenerRegistration? = null

/* ---------------------------------------------- Retrieval Functions ---------------------------------------------- */

    fun getUser(username: String): Task<DocumentSnapshot> {
        return FirebaseFirestore.getInstance().collection("users").document(username).get()
    }

    fun getUserFromUID(): Task<QuerySnapshot> {
        return FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("uid", FirebaseAuth.getInstance().currentUser!!.uid).get()
    }


    fun reportError(errMsg: String) {
        //TODO Report Error
        Log.e("T#", "Todo: Report error $errMsg")
    }


/* ---------------------------------------------- Listening Functions ---------------------------------------------- */

    @Suppress("UNCHECKED_CAST")
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

                        val doc = snapshot.documents[0]

                        val userData = User(uid, doc.id).apply {
                            groups = ObservableArrayList<String>().apply { addAll(doc["groups"] as ArrayList<String>) }
                        }
                        if (LocalData.user == null) {
                            Log.d("listToUserData", "Adding all user data")
                            LocalData.user = userData
                            return@addSnapshotListener
                        }
                    }
                }
            }
    }

//    fun listenToRemoteProfiles() {
//        val uid = FirebaseAuth.getInstance().currentUser!!.uid
//
//        if (remoteProfileListener != null) {
//            Log.w("listenToRemoteProfiles", "Tried listening while a listener was already subscribed!")
//            return
//        }
//
//        remoteProfileListener = FirebaseFirestore.getInstance().collection("remoteProfiles")
//            .whereArrayContains("users", uid)
//            .addSnapshotListener{snapshot, e ->
//                when {
//                    e != null -> { Log.e("listenToRemoteProfiles", "$e") }
//                    else -> {
//                        Log.d("listenToRemoteProfiles", "Received <${snapshot?.size()}> remoteProfiles from db")
//                        for (doc in snapshot!!) {
//                            val remoteProfile = doc.toObject(RemoteProfile::class.java)
//                            LocalData.remoteProfiles.remove(doc.id)
//                            LocalData.remoteProfiles[doc.id] = remoteProfile
//                        }
//                    }
//                }
//            }
//    }

    private fun listenToRemoteProfiles(remotes: ArrayList<String>) {
        remotes.forEach { uid ->
            if (!remoteProfileListeners.contains(uid)) {
                remoteProfileListeners[uid] = FirebaseFirestore.getInstance().collection("hubs").document(uid)
                    .addSnapshotListener {snapshot, e ->
                        when {
                        // Error
                            e != null -> {
                                Log.e("listenToHubs", "$snapshot | $e")
                                snapshot?.let {
                                    remoteProfileListeners[uid]?.remove()
                                    remoteProfileListeners.remove(uid)
                                    LocalData.remoteProfiles.remove(uid)
                                }
                            }
                        // Hub no longer exists
                            snapshot == null || !snapshot.exists() -> {
                                snapshot?.let { removeRemoteProfile(snapshot.id) }
                            }
                        // Received hub
                            else -> {
                                LocalData.remoteProfiles.remove(snapshot.id)
                                LocalData.remoteProfiles[snapshot.id] = snapshot.toObject(RemoteProfile::class.java)
                            }
                        }
                    }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun listenToUserGroups() {
        val groups = LocalData.user?.groups ?: arrayListOf<String>()
        groups.forEach {
            if (!groupListeners.containsKey(it)) {
                groupListeners[it] = FirebaseFirestore.getInstance().collection("groups").document(it)
                    .addSnapshotListener {snapshot, e ->
                        when {
                        // Error
                            e != null -> {
                                Log.e("listenToUserGroups", "$e")
                                snapshot?.let { removeGroup(snapshot) }
                            }
                        // Group no longer exists
                            snapshot == null || !snapshot.exists() -> {
                                Log.d("listenToUserGroups", "nonexistent group: ${snapshot?.id}")
                                // Check all hubs in this group and remove any the user no longer has access to
                                snapshot?.let { removeGroup(snapshot) }
                            }
                        // Received group
                            else -> {
                                Log.d("listenToUserGroups", "Received Group: ${snapshot.data}")
                                val groupId = snapshot.id
                                LocalData.userGroups.remove(groupId)
                                LocalData.userGroups[groupId] = Group(snapshot["owner"] as String, snapshot["personalGroup"] as Boolean)
                                    .apply { uid = groupId }
                                fetchConnectedDevicesFromGroup(groupId)
                                fetchRemoteProfiles(groupId)
                            }
                        }
                    }
            } else {
                Log.w("listenToUserGroups", "tried to listen on group <$it> while already subscribed... skipping...")
            }
        }
    }

    private fun fetchRemoteProfiles(groupId: String) {
        FirebaseFirestore.getInstance().collection("groups").document(groupId)
            .collection("remoteProfiles").addSnapshotListener { snap, ex ->
                when {
                    // Error
                    ex != null -> {
                        Log.e("listenToUserGroup", "Fetching remoteProfiles... $ex")
                    }
                    // Empty
                    snap!!.isEmpty -> {
                        Log.d("listenToUserGroup", "No remote profiles associated with group $groupId")
                    }
                    // Received Remote Profiles
                    else -> {
                        val listOfProfiles = ArrayList<String>()
                        snap.documentChanges.forEach { docChange ->
                            listOfProfiles.add(docChange.document.id)
                        }
                        listenToRemoteProfiles(listOfProfiles)
                    }
                }
            }
    }

    private fun fetchConnectedDevicesFromGroup(groupId: String) {
        FirebaseFirestore.getInstance().collection("groups").document(groupId)
            .collection("connectedDevices").addSnapshotListener { snapshot2, e2 ->
                when {
                    // Error
                    e2 != null -> {
                        Log.e("listenToUserGroup", "Fetching connectedDevices... $e2")
                    }
                    // Empty
                    snapshot2!!.isEmpty -> {
                        Log.d("listenToUserGroup", "No devices associated with group $groupId")
                    }
                    // Received devices
                    else -> {
                        val listOfHubs = ArrayList<String>()
                        snapshot2.documentChanges.forEach { docChange ->
                            listOfHubs.add(docChange.document.id)
                        }
                        listenToHubs(listOfHubs)
                    }
                }
            }
    }

    private fun listenToHubs(hubs: ArrayList<String>) {
        hubs.forEach { uid ->
            if (!hubListeners.contains(uid)) {
                hubListeners[uid] = FirebaseFirestore.getInstance().collection("hubs").document(uid)
                    .addSnapshotListener {snapshot, e ->
                        when {
                        // Error
                            e != null -> {
                                Log.e("listenToHubs", " ($uid) $e")
                                snapshot?.let {
                                    hubListeners[uid]?.remove()
                                    hubListeners.remove(uid)
                                    LocalData.hubs.remove(uid)
                                }
                            }
                        // Hub no longer exists
                            snapshot == null || !snapshot.exists() -> {
                                snapshot?.let { removeHub(snapshot.id) }
                            }
                        // Received hub
                            else -> {
                                LocalData.hubs.remove(snapshot.id)
                                val hub = snapshot.toObject(Hub::class.java)
                                LocalData.hubs[snapshot.id] = hub.apply { this?.uid = snapshot.id }
                            }
                        }
                    }
            }
        }
    }

    fun listenToIrSignals() {
        if (irSignalsListener != null) {
            Log.w("listenToIrSignals", "Tried listening while a listener was already subscribed!")
            return
        }

        val username = LocalData.user!!.username
        irSignalsListener = FirebaseFirestore.getInstance().collection("users").document(username).collection("irSignals")
            .addSnapshotListener {snapshot, e ->
                when {
                // Error
                    e != null -> {
                        Log.e("listenToIrSignals", "$e")
                        snapshot?.let { irSignalsListener?.remove() }
                    }
                    snapshot != null -> {
                        snapshot.documentChanges.forEach {docChange ->
                            when {
                            // IR signal was removed
                                !docChange.document.exists() -> {
                                    LocalData.irSignals.remove(docChange.document.id)
                                }
                            // New IR signal found
                                else -> {
                                    val irSignal = docChange.document.toObject(IrSignal::class.java)
                                    LocalData.irSignals.remove(docChange.document.id)
                                    LocalData.irSignals[docChange.document.id] = irSignal
                                }
                            }
                        }
                    }
                }

            }
    }

/* ----------------------------------------------- Storing Functions ----------------------------------------------- */

    fun addIrSignal(): Task<DocumentReference> {
        val username = LocalData.user!!.username
        val irSignal = TempData.tempSignal!!
        return FirebaseFirestore.getInstance().collection("users").document(username).collection("irSignals")
            .add(irSignal)
    }

    fun addUser(username: String) : Task<Void> {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val user = User(uid, username).apply{ groups.add(uid)}
        val group = Group(uid, true)

        val root = FirebaseFirestore.getInstance()
        val writeBatch = FirebaseFirestore.getInstance().batch()
        writeBatch.set(root.collection("users").document(username), user)
        writeBatch.set(root.collection("groups").document(uid), group)

        return writeBatch.commit()
    }

    fun addPersonalGroup(): Task<Void> {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val group = Group(uid, true)
        return FirebaseFirestore.getInstance().collection("groups").document(uid)
            .set(group)
    }

/* ----------------------------------------------- Removal Functions ----------------------------------------------- */

    private fun removeHub(uid: String) {
        hubListeners[uid]?.remove()
        hubListeners.remove(uid)
        LocalData.hubs.remove(uid)
    }

    private fun removeRemoteProfile(uid : String) {
        remoteProfileListeners[uid]?.remove()
        remoteProfileListeners.remove(uid)
        LocalData.remoteProfiles.remove(uid)
    }

    private fun removeGroup(snapshot: DocumentSnapshot) {
        LocalData.userGroups[snapshot.id]?.connectedDevices?.forEach { hubUID ->
            if (!stillHasAccess(hubUID, snapshot.id)) {
                hubListeners[hubUID]?.remove()
                hubListeners.remove(hubUID)
                LocalData.hubs.remove(hubUID)
            }
        }
        LocalData.userGroups.remove(snapshot.id)
    }

    fun removeAllListeners() {
        userListener?.remove().also { userListener = null }
        //remoteProfileListener?.remove().also { userListener = null }
        hubListeners.forEach {
            it.value.remove()
            hubListeners.remove(it.key)
        }
        groupListeners.forEach {
            it.value.remove()
            groupListeners.remove(it.key)
        }
        remoteProfileListeners.forEach {
            it.value.remove()
            remoteProfileListeners.remove(it.key)
        }
    }

    // Helper Functions

    private fun stillHasAccess(hubUID: String, groupUID: String): Boolean {
        LocalData.userGroups.keys.forEach { key ->
            // If a group other than the removed group contains the connected hub, the user still has access
            if (key != groupUID && LocalData.userGroups[key]?.connectedDevices?.contains(hubUID) == true) {
                return true
            }
        }

        return false
    }

    private fun listsAreEqual(local: ObservableArrayList<String>, remoteProfiles: ObservableArrayList<String>): Boolean {
        local.forEach {
            if (!remoteProfiles.contains(it))
                return false
        }

        return true
    }


}