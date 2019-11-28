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
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.models.firestore.IrSignal
import com.ms8.smartirhub.android.models.firestore.*
import com.ms8.smartirhub.android.remote_control.button.models.Button
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import org.jetbrains.anko.doAsync
import java.lang.Exception
import java.lang.StringBuilder

object FirestoreActions {
    private var userListener            : ListenerRegistration? = null
    private var groupListeners          : ArrayMap<String, ListenerRegistration> = ArrayMap()
    private var remoteProfileListeners  : ArrayMap<String, ListenerRegistration> = ArrayMap()
    private var hubListeners            : ArrayMap<String, ListenerRegistration> = ArrayMap()

    private var remoteListeners = ArrayMap<String, ListenerRegistration>()
    private var remotePermissionListeners = ArrayMap<String, ListenerRegistration>()

    private var bFetchingUser           : Boolean = false

/*
    ----------------------------------------------
        Retrieval Functions
    ----------------------------------------------
*/

    fun getUser(username: String): Task<DocumentSnapshot> {
        return FirebaseFirestore.getInstance().collection("users").document(username).get()
    }

    @SuppressLint("LogNotTimber")
    fun getUserFromUID() {
        if (!bFetchingUser) {
            bFetchingUser = true
            FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo("uid", FirebaseAuth.getInstance().currentUser!!.uid).get()
                .addOnCompleteListener { bFetchingUser = false }
                .addOnFailureListener { e ->
                    Log.e("getUserFromUID", "$e")
                    AppState.errorData.userSignInError.set(e)
                }
                .addOnSuccessListener { snapshots ->
                    if (snapshots.size() > 1)
                        Log.e("getUserFromUID", "Received more than one user object from uid:" +
                                " ${FirebaseAuth.getInstance().currentUser?.uid}")

                    if (snapshots.size() == 0) {
                        AppState.userData.user.uid.set(FirebaseAuth.getInstance().currentUser!!.uid)
                    } else {
                        AppState.userData.user.copyFromSnapshot(snapshots.documents[0])
                        listenToUserData2()
                    }

                    Log.d("TESTUSERUID", "user uid = ${AppState.userData.user.uid} | username = ${AppState.userData.user.username}")
                }
        }
    }

    @SuppressLint("LogNotTimber")
    fun getRemoteTemplates() {
        doAsync {
            FirebaseFirestore.getInstance().collection("remoteTemplates").addSnapshotListener { querySnapshot, e ->
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
                                            val template = RemoteProfileTemplate.fromSnapshot(docChange.document)
                                            AppState.userData.remoteTemplates[template.uid] = template
                                            Log.d("TEST", "adding template ${template.name}")
                                        } catch (ex : Exception) { Log.e("GetRemoteTemplates", "$ex") }
                                    }
                                }
                                REMOVED -> {
                                    AppState.userData.remoteTemplates.remove(docChange.document.id)
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
            val remoteProfile = AppState.userData.remotes[remoteProfileUID]

            // Remote Profile was at least partially downloaded. Ensure all irSignals are on local disk
            if (remoteProfile != null) {
                val missingIrSignals = ArrayList<String>()
                // Iterate through all actions and collect any irSignals not found locally
                for (i in 0 until remoteProfile.buttons.size) {
                    for (j in 0 until remoteProfile.buttons[i].commands.size)
                        for (k in 0 until remoteProfile.buttons[i].commands[j].actions.size) {
                            val sig = remoteProfile.buttons[i].commands[j].actions[k].irSignal
                            AppState.userData.irSignals[sig]
                                ?: missingIrSignals.add(sig)
                        }
                }

                // Check to see if we need to download some irSignals
                if (missingIrSignals.size > 0) {
                    getIrSignals(missingIrSignals, remoteProfile)
                } else {
                    // Otherwise just remove and add the remote profile to update any listeners
                    AppState.userData.remotes.remove(remoteProfile.uid)
                    AppState.userData.remotes[remoteProfile.uid] = remoteProfile
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
                                    Log.d("GetRemoteProfile", "Waiting on ${missingIrSignals.size} irSignals before adding to local database")
                                    getIrSignals(missingIrSignals, newProfile)
                                } else {
                                    Log.d("GetRemoteProfile", "Adding remote ${newProfile.uid}")
                                    AppState.userData.remotes.remove(newProfile.uid)
                                    AppState.userData.remotes[newProfile.uid] = newProfile
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
                    buttons.add(
                        Button(Button.buttonStyleFromInt((buttonMap["type"] as Number).toInt()) ?: run {
                            Log.e("FirestoreActions", "parseRemoteProfile - Unknown type found (${(buttonMap["type"] as Number).toInt()})")
                            Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND
                        })
                            .apply {
                                name = buttonMap["name"] as String
                                rowSpan = (buttonMap["rowSpan"] as Number).toInt()
                                columnSpan = (buttonMap["columnSpan"] as Number).toInt()
                                commands = ArrayList()
                                (buttonMap["commands"] as List<List<Map<String, Any?>>>).forEach { cmd ->
                                    commands.add(RemoteProfile.Command().apply {
                                        cmd.forEach { actionMap ->
                                            actions.add(RemoteProfile.Command.Action().apply {
                                                delay = (actionMap["delay"] as Number).toInt()
                                                hubUID = actionMap["hubUID"] as String
                                                irSignal = actionMap["irSignal"] as String
                                                if (!AppState.userData.irSignals.containsKey(irSignal))
                                                    missingIrSignals.add(irSignal)
                                            })
                                        }
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
        Log.d("getIrSignals", "Getting ${missingIrSignals.size} irSignals... (remoteProfile = $remoteProfile)")
        val tasks: ArrayList<Task<DocumentSnapshot>> = ArrayList()
        missingIrSignals.forEach { signal ->
            tasks.add(FirebaseFirestore.getInstance().collection("irSignals").document(signal).get())
        }
        Tasks.whenAllSuccess<Task<DocumentSnapshot>>(tasks)
            .addOnSuccessListener {listOfTasks ->
                Log.d("getIrSignals(remote)", "Got all irSignals!")
                try {
//                    listOfTasks.forEach {
//                        Log.d("getIrSignals(remote)", "parsing...")
//                        if (it.isSuccessful) {
//                            parseIrSignal(it.result, it.result?.id)?.let {sig ->
//                                Log.d("TEST", "signal: ${sig.name} | ${sig.rawLength} | ${sig.uid} | ${sig.rawData[0]}")
//                                LocalData.irSignals[sig.uid] = sig
//                            }
//                        } else {
//                            Log.w("TEST", "Not successful")
//                        }
//                    }
//                    remoteProfile?.let { prof ->
//                        LocalData.remotes.remove(prof.uid)
//                        LocalData.remotes[prof.uid] = prof
//                    }
                } catch (e : Exception) {
                    Log.e("getIrSignals", "$e")
                }

                try {
                    for (i in 0 until listOfTasks.size) {
                        Log.d("getIrSignals(remote)", "parsing... (remoteProfile = ${remoteProfile?.name ?: "ITS NULL"})")
                        val docsnap: DocumentSnapshot = listOfTasks[i] as DocumentSnapshot
                        //val signal = parseIrSignal(docsnap, docsnap.id)
                        val signal = docsnap.toObject(IrSignal::class.java)
                        signal?.let {irSig ->
                            Log.d("TEST", "signal: ${irSig.name} | ${irSig.rawLength} | ${irSig.uid} | ${irSig.rawData[0]}")
                            AppState.userData.irSignals.remove(irSig.uid)
                            AppState.userData.irSignals[irSig.uid] = irSig
                        }
                        remoteProfile?.let { prof ->
                            Log.d("getIrSignals(remote)", "adding remote profile")
                            AppState.userData.remotes.remove(prof.uid)
                            AppState.userData.remotes[prof.uid] = prof
                        }
                    }
                } catch (e : Exception) {Log.e("getIrSignals", "$e")}
            }
    }

    fun getIrSignal(irSignal: String): Task<DocumentSnapshot> {
        return FirebaseFirestore.getInstance().collection("irSignals").document(irSignal).get()
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
            - Listen to changes in group/remotes
            - Listen to changes in group/users

     */

//    @SuppressLint("LogNotTimber")
//    @Suppress("UNCHECKED_CAST")
//    fun listenToUserData(username: String) {
//        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
//
//        userListener = FirebaseFirestore.getInstance().collection("users")
//            .document(username)
//            .addSnapshotListener { snapshot, exception ->
//                when {
//                    exception != null -> { Log.e("listenToUserData", "snapshot error ($exception)") }
//                    !snapshot!!.exists() -> {Log.d("listenToUserData", "User data is null")}
//                    snapshot.id != TEST_USER -> {
//                        try {
//                            val userData = snapshot.toObject(User::class.java) ?: return@addSnapshotListener
//                            userData.username = snapshot.id
//                            userData.groups = ObservableArrayList<String>().apply { addAll(snapshot.data!!["groups"] as ArrayList<String>) }
//    //                        val userData = User().apply {
//    //                            val data = snapshot.data ?: return@addSnapshotListener
//    //                            this.groups = ObservableArrayList<String>().apply { addAll(data["groups"] as ArrayList<String>) }
//    //                            this.uid = uid
//    //                            this.username = snapshot.id
//    //                        }
//                            if (userData.uid == uid) {
//                                AppState.userData.user = userData
//                                listenToGroupData()
//                                // TODO: Listen to group invitations
//                            } else {
//                                Log.e("listenToUserData", "Listening to user who isn't currently logged in. (userData.uid = ${userData.uid}) (currentUser.uid = $uid)")
//                            }
//                        } catch (e : Exception) { Log.e("listenToUserData", "$e") }
//                    }
//                }
//            }
//    }

    @SuppressLint("LogNotTimber")
    fun listenToUserData2(username: String = AppState.userData.user.username.get() ?: "") {
        Log.d("T#listenToUserData2", "listening to userData from $username")
        doAsync {
            if (userListener != null) {
                Log.w("T#listenToUserData2", "Tried listening more than once!")
                return@doAsync
            }
            userListener = FirebaseFirestore.getInstance().collection("users")
                .document(username)
                .addSnapshotListener {snapshot, exception -> when {
                    exception != null -> { Log.e("listenToUserData", "snapshot error ($exception)") }
                    !snapshot!!.exists() -> {Log.d("T#listenToUserData2", "User data is null")}
                    else -> {
                        Log.d("T#listenToUserData2", "User before: \n${AppState.userData.user}")
                        AppState.userData.user.copyFromSnapshot(snapshot)

                        // listen to hubs
                        hubListeners.clear()
                        AppState.userData.user.hubs.forEach { hubUID ->
                            hubListeners[hubUID] =
                                FirebaseFirestore.getInstance().collection("hubs").document(hubUID)
                                .addSnapshotListener {snapshot, exception ->
                                    when {
                                    // error
                                        exception != null -> {
                                            Log.e("listenToUserData", "$exception")
                                            AppState.userData.hubs.remove(hubUID)
                                            AppState.errorData.userSignInError.set(exception)
                                        }
                                    // hub not found
                                        !snapshot!!.exists() -> {
                                            AppState.userData.hubs.remove(hubUID)
                                            AppState.errorData.userSignInError.set(Exception("Hub with uid '$hubUID' was not found!"))
                                        }
                                    // success
                                        else -> {
                                            AppState.userData.hubs[hubUID] = Hub.fromSnapshot(snapshot)
                                        }
                                    }
                                }
                        }

                        // listen to irSignals
                        AppState.userData.user.irSignals.forEach { irSignalUID ->
                            FirebaseFirestore.getInstance().collection("signals").document(irSignalUID)
                                .addSnapshotListener {snapshot, exception ->
                                    when {
                                    // error
                                        exception != null -> {
                                            Log.e("listenToUserData", "$exception")
                                            AppState.userData.irSignals.remove(irSignalUID)
                                            AppState.errorData.userSignInError.set(exception)
                                        }
                                    // irSignal not found
                                        !snapshot!!.exists() -> {
                                            AppState.userData.irSignals.remove(irSignalUID)
                                            AppState.errorData.userSignInError.set(Exception("IrSignal with uid '$irSignalUID' was not found!"))
                                        }
                                    // success
                                        else -> {
                                            AppState.userData.irSignals[irSignalUID] = (snapshot.toObject(IrSignal::class.java) ?: IrSignal())
                                                .apply {
                                                    uid = snapshot.id
                                                }
                                        }
                                    }}
                        }

                        // listen to remotes
                        Log.d("TEST", "Listening to ${AppState.userData.user.remotes.size} remotes")
                        AppState.userData.user.remotes.forEach { remoteUID ->
                            listenToRemote(remoteUID)
                        }

                        Log.d("T#listenToUserData2", "User after: \n${AppState.userData.user}")
                    }
                }}
        }
    }


/*
 ----------------------------------------------
    Fetching - Remotes
 ----------------------------------------------
 */

    class RemoteListener(private val remoteUID : String) : EventListener<DocumentSnapshot> {
        override fun onEvent(snapshot: DocumentSnapshot?, exception: FirebaseFirestoreException?) {
            when {
                exception != null -> {
                    Log.e("rmtListener", "$exception")
                    AppState.userData.remotes.remove(remoteUID)
                    AppState.userData.remotePermissions.remove(remoteUID)
                    AppState.errorData.userSignInError.set(exception)
                }
                !snapshot!!.exists() -> {
                    AppState.userData.remotes.remove(remoteUID)
                    AppState.errorData.userSignInError.set(Exception("Remote with uid '$remoteUID' was not found!"))
                }
                else -> {
                    // add remote
                    Log.d("TEST", "remoteListener - Received remote info for $remoteUID")
                    val newRemote = RemoteProfile.fromSnapshot(snapshot)
                    AppState.userData.remotes[remoteUID] = newRemote

                    // set to tempRemote if no remote is currently selected and is favorite remote
                    if (AppState.tempData.tempRemoteProfile.uid == "" && newRemote.uid == AppState.userData.user.favRemote)
                        AppState.tempData.tempRemoteProfile.copyFrom(newRemote)

                    // sync any new irSignals
                    val missingIrSignals = ArrayList<String>()
                    newRemote.buttons.forEach { button ->
                        button.commands.forEach { command ->
                            command.actions.forEach { action ->
                                if (!AppState.userData.irSignals.containsKey(action.irSignal))
                                    missingIrSignals.add(action.irSignal)
                            }
                        }
                    }
                    if (missingIrSignals.size > 0)
                        getIrSignals(ArrayList(missingIrSignals.distinct()))
                }
            }
        }
    }
    class RemotePermissionListener(private val remoteUID: String) : EventListener<DocumentSnapshot> {
        @SuppressLint("LogNotTimber")
        override fun onEvent(snapshot: DocumentSnapshot?, exception: FirebaseFirestoreException?) {
            when {
                exception != null -> {
                    Log.e("rmtPermissionListener", "$exception")
                    AppState.userData.remotes.remove(remoteUID)
                    AppState.userData.remotePermissions.remove(remoteUID)
                }
                !snapshot!!.exists() -> {
                    Log.e("rmtPermissionListener", "Remote with uid '$remoteUID' was not found or could not be accessed!")
                    AppState.userData.remotes.remove(remoteUID)
                    AppState.userData.remotePermissions.remove(remoteUID)
                }
                else -> {
                    snapshot.data?.let {
                        Log.d("TEST", "remotePermissionListener - Received remote info for $remoteUID")
                        var permissionType = RemoteProfile.PermissionType.READ
                        var username = ""

                        if (it.containsKey("permission")) {
                            permissionType = RemoteProfile.permissionFromString(it["permission"] as String)

                        } else Log.w("rmtPermissionListener", "Received snapshot for $remoteUID but is missing permission item")

                        if (it.containsKey("username")) {
                            username = it["username"] as String
                        } else Log.w("rmtPermissionListener", "Received snapshot for $remoteUID but is missing username item")

                        AppState.userData.remotePermissions[remoteUID] = RemoteProfile.Permission(permissionType, username)
                    }
                }
            }
        }
    }
    @SuppressLint("LogNotTimber")
    private fun listenToRemote(remoteUID: String) {
        Log.d("TEST", "listening to remote $remoteUID")
        if (remoteListeners[remoteUID] == null) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            remoteListeners[remoteUID] = FirebaseFirestore.getInstance().collection(REMOTES_ENDPOINT).document(remoteUID)
                .addSnapshotListener(RemoteListener(remoteUID))
            remotePermissionListeners[remoteUID] = FirebaseFirestore.getInstance().collection(REMOTES_ENDPOINT).document(remoteUID)
                .collection("users").document(uid)
                .addSnapshotListener(RemotePermissionListener(remoteUID))
        } else {
            Log.w("FirestoreActions", "listenToRemote - already listening to remote $remoteUID")
        }
    }

    fun updateRemoteName(uid: String) {
        val nameCopy = StringBuilder().append(AppState.tempData.tempRemoteName).toString()
        AppState.tempData.tempRemoteName = ""

        FirebaseFirestore.getInstance().collection(REMOTES_ENDPOINT).document(uid)
            .update("name", nameCopy)
    }

    fun addIrSignal(): Task<DocumentReference> {
        val irSignal = AppState.tempData.tempSignal.get()!!
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val username = AppState.userData.user.username.get() ?: throw Exception("No username")

        return FirebaseFirestore.getInstance().collection("signals")
            .add(irSignal.toFirebaseObject(uid))
            .addOnSuccessListener {
                val savedIrSignal = IrSignal.copyFrom(AppState.tempData.tempSignal.get())
                savedIrSignal.uid = it.id
                AppState.userData.irSignals[savedIrSignal.uid] = savedIrSignal
                FirebaseFirestore.getInstance().collection("users")
                    .document(username)
                    .update("irSignals", FieldValue.arrayUnion(savedIrSignal.uid))
                    .addOnSuccessListener {
                        AppState.tempData.tempSignal.set(savedIrSignal)
                    }.addOnFailureListener {e ->
                        AppState.errorData.saveSignalError.set(e)
                    }
            }
            .addOnFailureListener { e ->
                AppState.errorData.saveSignalError.set(e)
            }
    }

    fun addUser(username: String) {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseFirestore.getInstance().collection("users").document(username)
            .set(User().apply {
                this.uid.set(uid)
                this.username.set(username)
            }.toFirebaseObject())
            .addOnSuccessListener {
                listenToUserData2(username)
            }
            .addOnFailureListener { e -> AppState.errorData.userSignInError.set(e) }
    }

    @SuppressLint("LogNotTimber")
    fun addRemote() {
        // check for username and uid
        when {
            AppState.userData.user.username.get() == null -> {
                AppState.errorData.remoteSaveError.set(Exception("No username found in userData."))
                return
            }
            FirebaseAuth.getInstance().currentUser?.uid == null -> {
                AppState.errorData.remoteSaveError.set(Exception("No user currently logged in!"))
                return
            }
        }


        val remote = AppState.tempData.tempRemoteProfile
        remote.owner = FirebaseAuth.getInstance().currentUser!!.uid
        remote.owner = AppState.userData.user.username.get()!!

        // Add to 'remotes' collection, then add new uid to userData
        FirebaseFirestore.getInstance().collection(REMOTES_ENDPOINT).add(remote.toFirebaseObject())
            .addOnSuccessListener { ref ->
                // Add new uid to existing remote object
                remote.uid = ref.id

                updateUserDataAndAddRemotePermissions(remote)
            }
            .addOnFailureListener { e ->
                Log.e("addRemote", "$e")
                AppState.errorData.remoteSaveError.set(e)
            }
    }

    @SuppressLint("LogNotTimber")
    private fun updateUserDataAndAddRemotePermissions(remote: RemoteProfile) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Get updated list of remote UIDs
        val updatedUserData: MutableMap<String, Any?> = hashMapOf()
        updatedUserData["remotes"] = ArrayList<String>()
            .apply {
                addAll(AppState.userData.user.remotes)
                add(remote.uid)
            }
        // Add as favRemote if user doesn't have one currently
        if (AppState.userData.user.favRemote.isEmpty())
            updatedUserData["favRemote"] = remote.uid

        // Update userdata and add permissions collection to new remote
        FirebaseFirestore.getInstance().let { db ->
            val userDataRef = db.collection("users")
                .document(remote.owner)
            val remotePermissionsRef = db.collection(REMOTES_ENDPOINT)
                .document(remote.uid).collection("users").document(uid)
            val permission = RemoteProfile.Permission(RemoteProfile.PermissionType.FULL_ACCESS, remote.ownerUsername)

            db.batch()
                .update(userDataRef, updatedUserData)
                .set(remotePermissionsRef, permission)
                .commit()
                .addOnSuccessListener {
                    // Add new remote to listener group
                    listenToRemote(remote.uid)

                    // Denote success by setting editMode to false
                    AppState.tempData.tempRemoteProfile.inEditMode.set(false)

                    // Clear tempData no longer relevant to the saved remote
                    AppState.tempData.tempButton.set(null)
                    AppState.tempData.tempRemoteProfile.isCreatingNewButton.set(false)
                }.addOnFailureListener { e ->
                    Log.e("addRemote", "$e")
                    AppState.errorData.remoteSaveError.set(e)
                }
        }

//        // Update userData
//        FirebaseFirestore.getInstance().collection("users").document(remote.owner)
//            .update(updatedUserData)
//            .addOnSuccessListener {
//                // Add new remote to listener group
//                listenToRemote(remote.uid)
//
//                // Denote success by setting editMode to false
//                AppState.tempData.tempRemoteProfile.inEditMode.set(false)
//
//                // Clear tempData no longer relevant to the saved remote
//                AppState.tempData.tempButton.set(null)
//                AppState.tempData.tempRemoteProfile.isCreatingNewButton.set(false)
//            }
//            .addOnFailureListener { e ->
//                Log.e("addRemote", "$e")
//                AppState.errorData.remoteSaveError.set(e)
//            }
    }

/*
    -----------------------------------------------
        Removal Functions
    -----------------------------------------------
*/

    private fun checkRemoteAccess(remoteUID: String, groupID: String): Boolean {
        AppState.userData.groups.forEach { entry ->
            if (entry.key != groupID && entry.value.remoteProfiles.contains(remoteUID))
                return true
        }

        return false
    }

    private fun checkHubAccess(hubUID: String, groupID: String) {
        if (!stillHasAccess(hubUID, groupID)) {
            hubListeners[hubUID]?.remove()
            hubListeners.remove(hubUID)
            AppState.userData.hubs.remove(hubUID)
        }
    }

    private fun removeHub(uid: String) {
        hubListeners[uid]?.remove()
        hubListeners.remove(uid)
        AppState.userData.hubs.remove(uid)
    }

    private fun removeRemoteProfile(uid : String) {
        remoteProfileListeners[uid]?.remove()
        remoteProfileListeners.remove(uid)
        AppState.userData.remotes.remove(uid)
    }

    private fun removeGroup(snapshot: DocumentSnapshot) {
        AppState.userData.groups[snapshot.id]?.connectedDevices?.forEach { hubUID ->
            checkHubAccess(hubUID, snapshot.id)
        }
        AppState.userData.groups.remove(snapshot.id)
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
        remoteListeners.forEach {
            it.value.remove()
            remoteListeners.remove(it.key)
        }
        remotePermissionListeners.forEach {
            it.value.remove()
            remotePermissionListeners.remove(it.key)
        }
    }

    // Helper Functions

    private fun stillHasAccess(hubUID: String, groupUID: String): Boolean {
        AppState.userData.groups.keys.forEach { key ->
            // If a group other than the removed group contains the connected hub, the user still has access
            if (key != groupUID && AppState.userData.groups[key]?.connectedDevices?.contains(hubUID) == true) {
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

    fun updateRemote() {
        AppState.tempData.tempRemoteProfile.let { remote ->
            FirebaseFirestore.getInstance().collection(REMOTES_ENDPOINT).document(remote.uid)
                .set(remote.toFirebaseObject())
                .addOnSuccessListener {
                    AppState.tempData.tempRemoteProfile.inEditMode.set(false)
                }
                .addOnFailureListener { e ->
                    Log.d("updateRemote", "$e")
                    AppState.errorData.remoteSaveError.set(e)
                }
        }
    }

/*
    -----------------------------------------------
        Extensions
    -----------------------------------------------
*/

    /* RemoteProfile Extensions */

    private fun RemoteProfile.toFirebaseObject() : Map<String, Any?> {
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


    private const val TEST_REMOTE_PROFILE_TEMPLATE = "_TEST_TEMPLATE"
    private const val TEST_USER = "_TEST_USER"
    private const val TEST_GROUP = "_TEST_GROUP"
    private const val TEST_REMOTE_PROFILE = "_TEST_REMOTE_PROFILE"
    private const val TEST_REMOTE = "_TEST_REMOTE"

    private const val REMOTES_ENDPOINT = "remotes"
}