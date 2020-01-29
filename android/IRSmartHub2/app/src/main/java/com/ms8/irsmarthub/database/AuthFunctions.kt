package com.ms8.irsmarthub.database

import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import java.lang.Exception

object AuthFunctions {
    fun signInWithEmail(email: String, password: String) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { FirestoreFunctions.fetchAllData() }
            .addOnFailureListener { e -> AppState.errorData.signInError.set(e) }
    }

    fun signUpWithEmail(email: String, password: String) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { FirestoreFunctions.fetchAllData() }
            .addOnFailureListener { e -> AppState.errorData.signInError.set(e) }
    }

    fun signInWithGoogle(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account!!.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnSuccessListener { FirestoreFunctions.fetchAllData() }
                .addOnFailureListener { e -> AppState.errorData.signInError.set(e) }

        } catch (e : Exception) {
            AppState.errorData.signInError.set(e)
        }
    }

}