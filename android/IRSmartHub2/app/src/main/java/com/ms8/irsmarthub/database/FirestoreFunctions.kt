package com.ms8.irsmarthub.database

import android.util.ArrayMap
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.ms8.irsmarthub.hub.models.Hub
import com.ms8.irsmarthub.hub.models.HubPermissions
import com.ms8.irsmarthub.remote_control.command.models.IrSignal
import com.ms8.irsmarthub.remote_control.remote.models.Remote
import com.ms8.irsmarthub.remote_control.remote.models.RemotePermissions
import com.ms8.irsmarthub.user.models.User
import org.jetbrains.anko.doAsync

object FirestoreFunctions {
    private var userDataListener: ListenerRegistration? = null
    private val remoteListeners: ArrayMap<String, ListenerRegistration> = ArrayMap()
    private val hubListeners: ArrayMap<String, ListenerRegistration> = ArrayMap()

    private var isFetchingUser = false

    fun clearAllListeners() {
        userDataListener?.remove()?.also { userDataListener = null }
        remoteListeners.forEach { it.value.remove() }
        remoteListeners.clear()
        hubListeners.forEach { it.value.remove() }
        hubListeners.clear()
    }

    object User {
        /**
         * Adds new userData to the backend for the currently logged in user.
         * This function will attempt to reserve the given username, and if
         * an endpoint is found, it returns an error stating so.
         * On success, user data is "fetched" to set up endpoint listeners.
         */
        fun addUser(username: String) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid

            if (uid == null){
                Log.e(TAG, "(addUser) - Attempted to add a user but wasn't logged in.")
                AppState.errorData.signInError.set(Exception(("Attempted to add a user but wasn't logged in.")))
                return
            }

            val newUser = User(uid = uid)
            FirebaseFirestore.getInstance().collection("users").document(username)
                .set(newUser.toFirebaseObject())
                .addOnSuccessListener { fetchAllData() }
                .addOnFailureListener { e -> AppState.errorData.signInError.set(e) }
        }

        fun setUser(newUser: com.ms8.irsmarthub.user.models.User = AppState.tempData.tempUser.toUser()) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid

            if (uid == null){
                Log.e(TAG, "(setUser) - Attempted to set user but wasn't logged in.")
                AppState.errorData.signInError.set(Exception(("Attempted to set user but wasn't logged in.")))
                return
            }

            FirebaseFirestore.getInstance().collection("users").document(newUser.username)
                .set(newUser.toFirebaseObject())
                .addOnFailureListener { e -> AppState.errorData.signInError.set(e) }
        }
    }

/*
------------------------------------------------
    Initial Fetch of All User Data
-----------------------------------------------
*/

    /**
     * Runs through the process of getting all user data.
     * This starts by fetching the initial user data.
     * Then is fetches all remote, hub, and IrSignal data.
     *
     * If any fetch returns an error, errorData.fetchUserDataError is set.
     */
    fun fetchAllData() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            Log.d(TAG, "(fetchUserData) - Attempted to fetch data, but wasn't logged in.")
            return
        }

        if (isFetchingUser) {
            Log.d(TAG, "(fetchUserData) - Attempted to fetch data, but already fetching initial data.")
            return
        }

        // The following are used to identify which state the sign in process is currently in:
        // tempUser.uid == "" -> Haven't fetched any user data
        // tempUser.uid != "" && tempUser.username == "" -> Fetched initial data, but no initial data found (signing up)
        // tempUser.uid != "" && tempUser.username != "" -> Fetched initial data and received initial data (fetching all data/done)

        if (AppState.tempData.tempUser.uid.get()?.isNotEmpty() == true) {
            Log.d(TAG, "(fetchUserData) - Attempted to fetch data, but user must complete sign up first.")
            return
        }

        isFetchingUser = true
        FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("uid", currentUser.uid)
            .get()
            .addOnCompleteListener { isFetchingUser = false }
            .addOnSuccessListener { snapshots -> onUserQuery(snapshots) }
            .addOnFailureListener { e -> AppState.errorData.fetchUserDataError.set(e) }
    }

    /**
     * Handles an initial login query where the user was found via UID.
     */
    private fun onUserQuery(snapshots: QuerySnapshot) {
        val userUID = FirebaseAuth.getInstance().currentUser?.uid
            ?: return AppState.errorData.fetchUserDataError.set(Exception("(fetchUserData - onUserQuery) - " +
                    "Error: Not signed in!"))
        when {
            snapshots.size() > 1 ->
                AppState.errorData.fetchUserDataError.set(Exception("Received more than on user object from uid: $userUID"))
            snapshots.size() == 0 ->
            {
                // Set to an empty state with only the UID. This will let the splash screen know that it needs to prompt
                // the user for a username in order to create new userData endpoint. After which, fetchData() will properly
                // find the userData and continue  the sign-in process.
                Log.d(TAG, "(fetchUserData - onUserQuery) - No user found for uid: $userUID")
                AppState.tempData.tempUser.copyFrom(User().apply { uid = userUID })
            }
            else ->
            {
                val snapshotError = AppState.tempData.tempUser.copyFrom(snapshots.documents[0])
                snapshotError?.let { error ->
                    AppState.errorData.fetchUserDataError.set(error)
                    return
                }
                if (userDataListener != null)
                    return

                val username = AppState.tempData.tempUser.username.get()
                if (username?.isNotEmpty() == true) {
                    userDataListener =  FirebaseFirestore.getInstance().collection("users")
                        .document(username)
                        .addSnapshotListener {snapshot, exception -> onUserDataSnapshot(snapshot, exception)}
                } else
                    AppState.errorData.fetchUserDataError.set(Exception("(fetchUserData - onUserQuery) Error: Username missing for " +
                            "established userData associated with uid $userUID"))
            }
        }
    }

    private fun onUserDataSnapshot(snapshot: DocumentSnapshot?, exception: Exception?) {
        when {
            exception != null ->
                AppState.errorData.fetchUserDataError.set(Exception("Snapshot listener returned error: $exception"))
            !snapshot!!.exists() ->
            {
                // This should not be possible, as onUserDataSnapshot should only be triggered from a
                // previously successful query of the userData.
                AppState.errorData.fetchUserDataError.set(Exception("(fetchUserData - onUserDataSnapshot) Error: userData has disappeared for " +
                        "${FirebaseAuth.getInstance().currentUser?.uid}"))
                userDataListener?.remove()
                userDataListener = null
            }
            else ->
            {
                val snapshotError = AppState.tempData.tempUser.copyFrom(snapshot)
                snapshotError?.let { error -> AppState.errorData.fetchUserDataError.set(error); return }
                Log.d(TAG, "user has ${AppState.tempData.tempUser.remotes.size} remotes and ${AppState.tempData.tempUser.hubs.size} hubs")
                doAsync {
                    AppState.tempData.tempUser.remotes.forEach { remoteUID ->
                        if (!remoteListeners.containsKey(remoteUID)) {
                            val registration = FirebaseFirestore.getInstance().collection("remotes").document(remoteUID)
                                .addSnapshotListener {snapshot, exception -> onRemoteDataSnapshot(snapshot, exception)}
                            remoteListeners[remoteUID] = registration
                        }
                    }
                    AppState.tempData.tempUser.hubs.forEach { hubUID ->
                        if (!hubListeners.containsKey(hubUID)) {
                            val registration = FirebaseFirestore.getInstance().collection("hubs").document(hubUID)
                                .addSnapshotListener {snapshot, exception -> onHubDataSnapshot(snapshot, exception)}
                            hubListeners[hubUID] = registration
                        }
                    }
                }
            }
        }
    }

    private fun onRemoteDataSnapshot(snapshot: DocumentSnapshot?, exception: Exception?) {
        when {
            exception != null ->
                AppState.errorData.fetchUserDataError.set(Exception("(fetchUserData - onRemoteDataSnapshot)" +
                        " Snapshot listener returned error: $exception"))
            !snapshot!!.exists() ->
            {
                AppState.errorData.fetchUserDataError.set(Exception("(fetchUserData - onRemoteDataSnapshot)" +
                        " No remote found for uid: ${snapshot.id}"))
                AppState.userData.remotes.remove(snapshot.id)
            }
            else -> {
                val remoteResult = Remote.copyFrom(snapshot)
                    ?: return AppState.errorData.fetchUserDataError.set(Exception("(fetchUserData - onRemoteDataSnapshot)" +
                            " Unable to convert snapshot to remote (${snapshot.data.toString()})"))

                // Add remote
                AppState.userData.remotes[snapshot.id] = remoteResult

                doAsync {
                    // Fetch missing IR signals for remote
                    remoteResult.buttons.forEach { button ->
                        button.commands.forEach { command ->
                            command.actions.forEach { action ->
                                if (!AppState.userData.signals.containsKey(action.irSignal))
                                    FirebaseFirestore.getInstance().collection("signals").document(action.irSignal).get()
                                        .addOnCompleteListener { task -> onIrSignalDataSnapshot(task)}
                            }
                        }
                    }
                }

                // Fetch user's permissions
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                    ?: return AppState.errorData.fetchUserDataError.set(Exception("(fetchUserData - onRemoteDataSnapshot) - Not signed in!"))
                FirebaseFirestore.getInstance().collection("remotes").document(remoteResult.uid).collection("users")
                    .document(uid).get().addOnCompleteListener { task -> onRemotePermissionsSnapshot(task, remoteResult.uid) }
            }
        }
    }

    private fun onRemotePermissionsSnapshot(task: Task<DocumentSnapshot>, remoteUID: String) {
        when {
            task.isSuccessful -> {
                val remotePermissions = RemotePermissions.copyFrom(task.result)
                    ?: return AppState.errorData.fetchUserDataError.set(Exception("(fetchUserData - onHubPermissionsSnapshot)" +
                            " Unable to convert snapshot to HubPermissions (${task.result?.data.toString()})"))

                AppState.userData.remotes[remoteUID]?.userPermissions?.put(remotePermissions.uid, remotePermissions)
            }
            else ->
                AppState.errorData.fetchUserDataError.set(Exception("(fetchUserData - onIrSignalDataSnapshot)" +
                        " Problem fetching IR signal (${task.result?.id} failed with exception: ${task.exception})"))
        }
    }

    private fun onIrSignalDataSnapshot(task: Task<DocumentSnapshot>) {
        when {
            task.isSuccessful -> {
                val irSignalResult = IrSignal.copyFrom(task.result)
                if (irSignalResult == null) {
                    AppState.errorData.fetchUserDataError.set(Exception("(fetchUserData - onIrSignalDataSnapshot) " +
                            "Unable to convert snapshot to IrSignal (${task.result?.data.toString()})"))
                    return
                }
                AppState.userData.signals[irSignalResult.uid] = irSignalResult
            }
            else ->
                AppState.errorData.fetchUserDataError.set(Exception("Problem fetching IR signal (${task.result?.id} failed with exception: " +
                        "${task.exception})"))
        }
    }

    private fun onHubDataSnapshot(snapshot: DocumentSnapshot?, exception: Exception?) {
        when {
            exception != null ->
                AppState.errorData.fetchUserDataError.set(Exception("(fetchUserData - onHubDataSnapshot)" +
                        " Snapshot listener returned error: $exception"))
            !snapshot!!.exists() ->
            {
                AppState.errorData.fetchUserDataError.set(Exception("fetchUserData - onHubDataSnapshot)" +
                        " No hub found for uid: ${snapshot.id}"))
                AppState.userData.hubs.remove(snapshot.id)
            }
            else ->
            {
                val hubResult = Hub.fromSnapshot(snapshot)
                if (hubResult == null) {
                    AppState.errorData.fetchUserDataError.set(Exception("(fetchUserData - onHubDataSnapshot)" +
                            " Unable to convert snapshot to Hub (${snapshot.data.toString()})"))
                    return
                }
                // Add hub
                AppState.userData.hubs[hubResult.uid] = hubResult
                // Fetch hub permissions
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid == null) {
                    AppState.errorData.fetchUserDataError.set(Exception("(fetchUserData - onHubDataSnapshot) - Not signed in!"))
                    return
                }
                FirebaseFirestore.getInstance().collection("hubs").document(hubResult.uid).collection("users")
                    .document(uid).get().addOnCompleteListener { task -> onHubPermissionsSnapshot(task, hubResult.uid) }
            }
        }
    }

    private fun onHubPermissionsSnapshot(
        task: Task<DocumentSnapshot>,
        hubUID: String
    ) {
        when {
            task.isSuccessful -> {
                val hubPermissions = HubPermissions.copyFrom(task.result)
                if (hubPermissions == null) {
                    AppState.errorData.fetchUserDataError.set(Exception("(fetchUserData - onHubPermissionsSnapshot)" +
                            " Unable to convert snapshot to HubPermissions (${task.result?.data.toString()})"))
                    return
                }
                AppState.userData.hubs[hubUID]?.userPermissions?.put(hubPermissions.uid, hubPermissions)
            }
            else ->
                AppState.errorData.fetchUserDataError.set(Exception("(fetchUserData - onIrSignalDataSnapshot)" +
                        " Problem fetching IR signal (${task.result?.id} failed with exception: ${task.exception})"))
        }
    }

    private const val TAG = "FirebaseFunctions"
}