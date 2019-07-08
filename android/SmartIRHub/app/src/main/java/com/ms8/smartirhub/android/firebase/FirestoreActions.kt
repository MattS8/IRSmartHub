package com.ms8.smartirhub.android.firebase

import android.annotation.SuppressLint
import android.util.ArrayMap
import android.util.Log
import androidx.databinding.ObservableArrayList
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.DocumentChange.Type.REMOVED
import com.google.firebase.firestore.DocumentChange.Type.MODIFIED
import com.google.firebase.firestore.DocumentChange.Type.ADDED
import com.ms8.smartirhub.android.database.LocalData
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.models.firestore.IrSignal
import com.ms8.smartirhub.android.models.firestore.*
import org.jetbrains.anko.doAsync
import java.lang.Exception

object FirestoreActions {
    private var userListener            : ListenerRegistration? = null
    private var groupListeners          : ArrayMap<String, ListenerRegistration> = ArrayMap()
    private var remoteProfileListeners  : ArrayMap<String, ListenerRegistration> = ArrayMap()
    private var hubListeners            : ArrayMap<String, ListenerRegistration> = ArrayMap()

/*
    ----------------------------------------------
        Retrieval Functions
    ----------------------------------------------
*/

    fun getUser(username: String): Task<DocumentSnapshot> {
        return FirebaseFirestore.getInstance().collection("users").document(username).get()
    }

    fun getUserFromUID(): Task<QuerySnapshot> {
        return FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("uid", FirebaseAuth.getInstance().currentUser!!.uid).get()
    }

    @SuppressLint("LogNotTimber")
    fun getRemoteTemplates() {
        doAsync {
            FirebaseFirestore.getInstance().collection("remoteProfileTemplates").addSnapshotListener { querySnapshot, e ->
                when {
                    e != null -> {
                        Log.e("GetRemoteTemplates", "$e")
                    }
                    else -> {
                        querySnapshot!!.documentChanges.forEach { docChange ->
                            when (docChange.type) {
                                ADDED, MODIFIED -> {
                                    if (docChange.document.id != TEST_REMOTE_PROFILE_TEMPLATE) {
                                        try {
                                            val template =
                                                RemoteProfileTemplate()
                                            template.name = docChange.document["name"] as String
                                            Log.d("TEST", "adding template ${template.name}")
                                            template.remoteProfile = docChange.document["remoteProfile"] as String
                                            template.previewURL = docChange.document["previewURL"] as String
                                            template.uid = docChange.document.id
                                            LocalData.remoteProfileTemplates[docChange.document.id] = template
                                        } catch (ex : Exception) { Log.e("GetRemoteTemplates", "$ex") }
                                    }
                                }
                                REMOVED -> {
                                    LocalData.remoteProfileTemplates.remove(docChange.document.id)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun reportError(errMsg: String) {
        //TODO Report Error
        Log.e("T#", "Todo: Report error $errMsg")
    }

    @SuppressLint("LogNotTimber")
    fun getRemoteProfile(remoteProfileUID: String) {
        doAsync {
            val remoteProfile = LocalData.remoteProfiles[remoteProfileUID]

            // Remote Profile was at least partially downloaded. Ensure all signals are on local disk
            if (remoteProfile != null) {
                val missingIrSignals = ArrayList<String>()
                // Iterate through all actions and collect any signals not found locally
                for (i in 0 until remoteProfile.buttons.size) {
                    for (x in 0 until remoteProfile.buttons[i].command.actions.size) {
                        val sig = remoteProfile.buttons[i].command.actions[x].irSignal
                        LocalData.signals[sig]
                            ?: missingIrSignals.add(sig)
                    }
                }

                // Check to see if we need to download some signals
                if (missingIrSignals.size > 0) {
                    getIrSignals(missingIrSignals, remoteProfile)
                } else {
                    // Otherwise just remove and add the remote profile to update any listeners
                    LocalData.remoteProfiles.remove(remoteProfile.uid)
                    LocalData.remoteProfiles[remoteProfile.uid] = remoteProfile
                }
            } else {
                // Download entire remoteProfile
                FirebaseFirestore.getInstance().collection("remoteProfile").document(remoteProfileUID).get()
                    .addOnFailureListener { Log.e("GetRemoteProfile", "Failed to get profile with uid: $remoteProfileUID") }
                    .addOnSuccessListener { snapshot ->
                        Log.d("GetRemoteProfile", "Got Result")
                        val missingIrSignals = ArrayList<String>()
                        if (snapshot.exists()) {
                            val newProfile = parseRemoteProfile(snapshot, false, missingIrSignals)

                            if (newProfile != null) {
                                if (missingIrSignals.isNotEmpty()) {
                                    Log.d("GetRemoteProfile", "Waiting on ${missingIrSignals.size} signals before adding to local database")
                                    getIrSignals(missingIrSignals, newProfile)
                                } else {
                                    Log.d("GetRemoteProfile", "Adding remote ${newProfile.uid}")
                                    LocalData.remoteProfiles.remove(newProfile.uid)
                                    LocalData.remoteProfiles[newProfile.uid] = newProfile
                                }
                            }
                        } else {
                            Log.w("GetRemoteProfile", "remoteProfile with UID ${snapshot.id} does not exist")
                        }
                    }
            }
        }
    }

    @SuppressLint("LogNotTimber")
    private fun parseRemoteProfile(snapshot: DocumentSnapshot, fetchMissingSignals: Boolean = false, missingIrSignals: ArrayList<String> = ArrayList()): RemoteProfile? {
        var newProfile: RemoteProfile? = null
        Log.d("ParseRemoteProfile", "Parsing snapshot...")
        try {
            newProfile = RemoteProfile().apply {
                (snapshot["buttons"] as List<Map<String, Any?>>).forEach { buttonMap ->
                    buttons.add(RemoteProfile.Button().apply {
                        name = buttonMap["name"] as String
                        style = (buttonMap["style"] as Number).toInt()
                        (buttonMap["command"] as List<Map<String, Any?>>).forEach { actionMap ->
                            command.actions.add(RemoteProfile.Command.Action().apply {
                                delay = (actionMap["delay"] as Number).toInt()
                                hubUID = actionMap["hubUID"] as String
                                irSignal = actionMap["irSignal"] as String
                                if (!LocalData.signals.containsKey(irSignal))
                                    missingIrSignals.add(irSignal)
                            })
                        }
                    })
                }
                name = snapshot["name"] as String
                uid = snapshot.id
            }
        } catch (e : Exception) { Log.e("GetRemoteProfile", "$e")}

        if (fetchMissingSignals)
            getIrSignals(missingIrSignals)

        return newProfile
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun getIrSignals(missingIrSignals: ArrayList<String>, remoteProfile : RemoteProfile? = null) {
        Log.d("getSignals", "Getting ${missingIrSignals.size} signals... (remoteProfile = $remoteProfile)")
        val tasks: ArrayList<Task<DocumentSnapshot>> = ArrayList()
        missingIrSignals.forEach { signal ->
            tasks.add(FirebaseFirestore.getInstance().collection("signals").document(signal).get())
        }
        Tasks.whenAllSuccess<Task<DocumentSnapshot>>(tasks)
            .addOnSuccessListener {listOfTasks ->
                Log.d("getSignals(remote)", "Got all signals!")
                try {
//                    listOfTasks.forEach {
//                        Log.d("getSignals(remote)", "parsing...")
//                        if (it.isSuccessful) {
//                            parseIrSignal(it.result, it.result?.id)?.let {sig ->
//                                Log.d("TEST", "signal: ${sig.name} | ${sig.rawLength} | ${sig.uid} | ${sig.rawData[0]}")
//                                LocalData.signals[sig.uid] = sig
//                            }
//                        } else {
//                            Log.w("TEST", "Not successful")
//                        }
//                    }
//                    remoteProfile?.let { prof ->
//                        LocalData.remoteProfiles.remove(prof.uid)
//                        LocalData.remoteProfiles[prof.uid] = prof
//                    }
                } catch (e : Exception) {
                    Log.e("getSignals", "$e")
                }

                try {
                    for (i in 0 until listOfTasks.size) {
                        Log.d("getSignals(remote)", "parsing... (remoteProfile = ${remoteProfile?.name ?: "ITS NULL"})")
                        val docsnap: DocumentSnapshot = listOfTasks[i] as DocumentSnapshot
                        //val signal = parseIrSignal(docsnap, docsnap.id)
                        val signal = docsnap.toObject(IrSignal::class.java)
                        signal?.let {irSig ->
                            Log.d("TEST", "signal: ${irSig.name} | ${irSig.rawLength} | ${irSig.uid} | ${irSig.rawData[0]}")
                            LocalData.signals.remove(irSig.uid)
                            LocalData.signals[irSig.uid] = irSig
                        }
                        remoteProfile?.let { prof ->
                            Log.d("getSignals(remote)", "adding remote profile")
                            LocalData.remoteProfiles.remove(prof.uid)
                            LocalData.remoteProfiles[prof.uid] = prof
                        }
                    }
                } catch (e : Exception) {Log.e("getSignals", "$e")}
            }
    }

    fun getIrSignal(irSignal: String): Task<DocumentSnapshot> {
        return FirebaseFirestore.getInstance().collection("signals").document(irSignal).get()
    }

//    @SuppressLint("LogNotTimber")
//    @Suppress("UNCHECKED_CAST")
//    private fun parseIrSignal(result: DocumentSnapshot?, id: String?): IrSignal? {
//        var signal: IrSignal? = null
//        try {
//            val irMap = result!!
//            signal = IrSignal().apply {
//                this.repeat = irMap["repeat"] as Boolean
//                this.code = irMap["code"] as String
//                this.encodingType = (irMap["encodingType"] as Number).toInt()
//                this.rawLength = (irMap["rawLength"] as Number).toInt()
//                this.name = irMap["name"] as String
//                this.uid = id!!
//                this.rawData = HashMap(irMap["rawData"] as Map<Int, String>)
//            }
//        } catch (e : Exception) { Log.e("parseIrSignal", "$e") }
//
//        Log.d("parseIrSignal", "Parse successful! (${signal?.uid})")
//        return signal
//    }


/*
    ----------------------------------------------
        Listening Functions
    ----------------------------------------------
*/
    /*
        1. Listen to changes in user data (listenToUserData)
            - Spawn listener for each group (currently only listens to static info: "owner" and "personalGroup")
            - Spawn listener for group invitations

        2. For Each Group:
            - Listen to changes in group/connectedDevices
            - Listen to changes in group/remoteProfiles
            - Listen to changes in group/users

     */

    @SuppressLint("LogNotTimber")
    @Suppress("UNCHECKED_CAST")
    fun listenToUserData(username: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        userListener = FirebaseFirestore.getInstance().collection("users")
            .document(username)
            .addSnapshotListener { snapshot, exception ->
                when {
                    exception != null -> { Log.e("listenToUserData", "snapshot error ($exception)") }
                    !snapshot!!.exists() -> {Log.d("listenToUserData", "User data is null")}
                    snapshot.id != TEST_USER -> {
                        try {
                            val userData = snapshot.toObject(User::class.java) ?: return@addSnapshotListener
                            userData.username = snapshot.id
                            userData.groups = ObservableArrayList<String>().apply { addAll(snapshot.data!!["groups"] as ArrayList<String>) }
    //                        val userData = User().apply {
    //                            val data = snapshot.data ?: return@addSnapshotListener
    //                            this.groups = ObservableArrayList<String>().apply { addAll(data["groups"] as ArrayList<String>) }
    //                            this.uid = uid
    //                            this.username = snapshot.id
    //                        }
                            if (userData.uid == uid) {
                                LocalData.user = userData
                                listenToGroupData()
                                // TODO: Listen to group invitations
                            } else {
                                Log.e("listenToUserData", "Listening to user who isn't currently logged in. (userData.uid = ${userData.uid}) (currentUser.uid = $uid)")
                            }
                        } catch (e : Exception) { Log.e("listenToUserData", "$e") }
                    }
                }
            }
    }

    @SuppressLint("LogNotTimber")
    @Suppress("UNCHECKED_CAST")
    fun listenToGroupData() {
        val groups = LocalData.user?.groups ?: return
        groups.forEach { groupUID ->
            if (groupUID != TEST_GROUP && !groupListeners.containsKey(groupUID)) {
                // Listen to group
                groupListeners[groupUID] = FirebaseFirestore.getInstance().collection("groups").document(groupUID)
                    .addSnapshotListener {snapshot, e ->
                        when {
                            // Error
                            e != null -> {
                                Log.e("listenToGroupData", "$e")
                                // Check all hubs in this group and remove any the user no longer has access to
                                snapshot?.let { removeGroup(snapshot) }
                            }
                            // Null group?
                            snapshot == null -> { Log.w("listenToUserGroup", "snapshot for $groupUID was null!") }
                            // Group no longer exists
                            !snapshot.exists() -> {
                                Log.d("listenToGroupData", "nonexistent group: ${snapshot.id}")
                                // Check all hubs in this group and remove any the user no longer has access to
                                removeGroup(snapshot)
                            }
                            // Received group
                            else -> {
                                Log.d("listenToGroupData", "Received Group: ${snapshot.data}")
                                try {
                                    val group = Group().apply {
                                        uid = snapshot.id
                                        owner = snapshot["owner"] as String
                                        personalGroup = snapshot["personalGroup"] as Boolean
                                    }
                                    LocalData.groups.remove(snapshot.id)
                                    LocalData.groups[snapshot.id] = group
                                } catch (e : Exception) { Log.e("listenToGroupData", "$e") }
                            }
                        }
                    }

                // Get list of remoteProfiles associated with this group and spawn listeners
                FirebaseFirestore.getInstance().collection("groups").document(groupUID)
                    .collection("remoteProfiles").addSnapshotListener { snap, ex ->
                        when {
                            // Error
                            ex != null -> { Log.e("listenToUserGroup", "Fetching remoteProfiles... $ex") }
                            // Empty
                            snap == null || snap.isEmpty -> { Log.d("listenToUserGroup", "No remote profiles associated with group $groupUID") }
                            // Received Remote Profiles
                            else -> {
                                val listOfProfiles = ArrayList<String>()
                                snap.documentChanges.forEach { docChange ->
                                    when (docChange.type) {
                                    // Remote removed from group
                                        REMOVED -> {
                                            // Check to see if user still has access to remote after it being removed from group
                                            checkRemoteAccess(docChange.document.id, groupUID)
                                        }
                                    // Remote changed/added to group
                                        MODIFIED, ADDED -> {
                                            listOfProfiles.add(docChange.document.id)
                                        }
                                    }
                                }
                                listenToRemoteProfiles(listOfProfiles)
                            }
                        }
                    }

                // Get list of connectedDevices associated with this group and spawn listeners
                FirebaseFirestore.getInstance().collection("groups").document(groupUID)
                    .collection("connectedDevices").addSnapshotListener { snapshot2, e2 ->
                        when {
                            // Error
                            e2 != null -> { Log.e("listenToUserGroup", "Fetching connectedDevices... $e2") }
                            // Null
                            snapshot2 == null -> { Log.w("listenToUserGroup", "connectedDevice snapshot for $groupUID was null") }
                            // Empty
                            snapshot2.isEmpty -> { Log.d("listenToUserGroup", "No devices associated with group $groupUID") }
                            // Received devices
                            else -> {
                                val listOfHubs = ArrayList<String>()
                                snapshot2.documentChanges.forEach { docChange ->
                                    when (docChange.type) {
                                    // SmartHub was removed from group
                                        REMOVED -> {
                                            // Check to see if user still has access to hub after it being removed from group
                                            checkHubAccess(docChange.document.id, groupUID)
                                        }
                                    // SmartHub was added/changed to group
                                        MODIFIED, ADDED -> {
                                            listOfHubs.add(docChange.document.id)
                                        }
                                    }
                                }
                                listenToHubs(listOfHubs)
                            }
                        }
                    }

                // Get list of users associated with this group and spawn listeners
                FirebaseFirestore.getInstance().collection("groups").document(groupUID)
                    .collection("users").addSnapshotListener {usersSnapshot, usersError ->
                        when {
                        // Error
                            usersError != null -> { Log.e("listenToUserGroup", "Fetching users... $usersError") }
                        // Null
                            usersSnapshot == null -> { Log.w("listenToUserGroup", "users snapshot for $groupUID was null!")}
                        // Empty
                            usersSnapshot.isEmpty -> { Log.w("listenToUserGroup", "No users associated with group $groupUID (huh?)")}
                        // Received users
                            else -> {
                                usersSnapshot.documentChanges.forEach { documentChange ->
                                    val docId = documentChange.document.id
                                    // Remove group is user has been removed from group
                                    if (docId == FirebaseAuth.getInstance().currentUser?.uid && documentChange.type == REMOVED) {
                                        removeGroup(documentChange.document)
                                    }
                                }
                            }
                        }
                    }

            } else {
                Log.d("listenToGroupData", "tried to listen on group <$groupUID> while already subscribed... skipping...")
            }
        }
    }

    private fun listenToRemoteProfiles(remotes: ArrayList<String>) {
        remotes.forEach { uid ->
            if (uid != TEST_REMOTE_PROFILE && !remoteProfileListeners.contains(uid)) {
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
                                parseRemoteProfile(snapshot, true)?.let { remoteProf -> LocalData.remoteProfiles[snapshot.id] = remoteProf }
                            }
                        }
                    }
            }
        }
    }

    @SuppressLint("LogNotTimber")
    private fun listenToHubs(hubs: ArrayList<String>) {
        hubs.forEach { uid ->
            if (!hubListeners.contains(uid)) {
                hubListeners[uid] = FirebaseFirestore.getInstance().collection("hubs").document(uid)
                    .addSnapshotListener {snapshot, e ->
                        when {
                        // Error
                            e != null -> {
                                Log.e("listenToHubs", " ($uid) $e")
                                snapshot?.let { removeHub(it.id) }
                            }
                        // Hub no longer exists
                            snapshot == null || !snapshot.exists() -> {
                                snapshot?.let { removeHub(it.id) }
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

/* ----------------------------------------------- Storing Functions ----------------------------------------------- */

    fun addIrSignal(): Task<DocumentReference> {
        val irSignal = TempData.tempSignal!!
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        return FirebaseFirestore.getInstance().collection("signals")
            .add(irSignal.toFirebaseObject(uid))
    }

    fun addUser(username: String) : Task<Void> {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val user = User().apply {
            this.uid = uid
            this.username = username
            groups.add(uid)
        }
        val group = Group().apply {
            this.uid = uid
            this.personalGroup = true
        }

        val root = FirebaseFirestore.getInstance()
        val writeBatch = FirebaseFirestore.getInstance().batch()
        writeBatch.set(root.collection("users").document(username), user)
        writeBatch.set(root.collection("groups").document(uid), group)

        return writeBatch.commit()
    }

/*
    -----------------------------------------------
        Removal Functions
    -----------------------------------------------
*/

    private fun checkRemoteAccess(remoteUID: String, groupID: String): Boolean {
        LocalData.groups.forEach { entry ->
            if (entry.key != groupID && entry.value.remoteProfiles.contains(remoteUID))
                return true
        }

        return false
    }

    private fun checkHubAccess(hubUID: String, groupID: String) {
        if (!stillHasAccess(hubUID, groupID)) {
            hubListeners[hubUID]?.remove()
            hubListeners.remove(hubUID)
            LocalData.hubs.remove(hubUID)
        }
    }

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
        LocalData.groups[snapshot.id]?.connectedDevices?.forEach { hubUID ->
            checkHubAccess(hubUID, snapshot.id)
        }
        LocalData.groups.remove(snapshot.id)
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
        LocalData.groups.keys.forEach { key ->
            // If a group other than the removed group contains the connected hub, the user still has access
            if (key != groupUID && LocalData.groups[key]?.connectedDevices?.contains(hubUID) == true) {
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


    const val TEST_REMOTE_PROFILE_TEMPLATE = "_TEST_TEMPLATE"
    const val TEST_USER = "TEST_USER"
    const val TEST_GROUP = "_TEST_GROUP"
    const val TEST_REMOTE_PROFILE = "_TEST_REMOTE_PROFILE"
}