package com.ms8.smartirhub.android.firebase

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.ms8.smartirhub.android.data.User

object FirestoreActions {

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
}