package com.ms8.smartirhub.android.firebase

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.andrognito.flashbar.Flashbar
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.ms8.smartirhub.android.R
import java.lang.Exception

object FirebaseAuthActions {

    /**
     * Runs through initial Auth steps. Begins by signing in anonymously if the user is not
     * already signed into an account. If the user is signed in, but they haven't verified their
     * email, a FlashBar notification is shown to remind them. This function is meant to be run after
     * initial splash screen.
     */
    fun init(activity: AppCompatActivity) {
        val instance = FirebaseAuth.getInstance()

        when {
            // Sign In anonymously if not signed in
            instance.currentUser == null -> instance.signInAnonymously()
            // User is signed in but email has not been verified
            !instance.currentUser!!.isEmailVerified -> {
                // Combine reminder message with user's email
                val message = activity.getString(R.string.remind_verify_email) + instance.currentUser!!.email

                // Show email reminder SnackBar
                Flashbar.Builder(activity)
                    .title(R.string.verify_email)
                    .message(message)
                    .positiveActionText(R.string.resend_verification_email)
                    .negativeActionText(android.R.string.ok)
                    .positiveActionTextColorRes(R.color.colorAccent)
                    .positiveActionTapListener(object : Flashbar.OnActionTapListener {
                        override fun onActionTapped(bar: Flashbar) {
                            bar.dismiss()
                            sendEmailVerification()
                        }
                    })
                    .negativeActionTapListener(object : Flashbar.OnActionTapListener {
                        override fun onActionTapped(bar: Flashbar) = bar.dismiss()
                    })
                    .build()
                    .show()
            }
        }
    }

    /**
     * Starts the process of signing a user in/up with Google.
     */
    fun signInWithGoogle(activity: AppCompatActivity) {
        Log.d("TEST###", "Signing in with Google...")
        if (GoogleSignIn.getLastSignedInAccount(activity) == null)
            Log.d("TEST#", "No previous google sign in")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.client_id))
            .requestProfile()
            .build()
        activity.startActivityForResult(GoogleSignIn.getClient(activity, gso).signInIntent, RC_SIGN_IN)
    }

    /**
     * Sends a verification email to the email account associated with t he signed in user.
     */
    private fun sendEmailVerification() {
        val instance = FirebaseAuth.getInstance()

        when (instance.currentUser) {
            null -> Log.e(TAG, "Tried resending verification email while not logged in!")
            else -> instance.currentUser!!.sendEmailVerification()
        }
    }

    /**
     *  Attempts to sign user in with their google account
     */
    fun handleGoogleSignInResult(data: Intent?) : Exception? {
        Log.d("TEST###", "Handling Google Sign In Result...")
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account!!.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener { authTask ->
                if (authTask.isSuccessful)
                    Log.d("TEST###", "user display name: ${FirebaseAuth.getInstance().currentUser?.displayName}")
                else
                    Log.w("TEST###", "failed to log in with google... ${authTask.exception}")
            }
        } catch (e : Exception) {
            return e.also { Log.e(TAG, "Google sign in failed", e) }
        }

        return null
    }

    /**
     *  Creates an entry in the database for newly created user.
     */
    fun createUserDB() {
        val user = FirebaseAuth.getInstance().currentUser


    }


    /**
     * Shows a flashBar view notifying the user that signing in with Google has failed.
     */
    fun showSignInWithGoogleError(activity: AppCompatActivity) {
        Flashbar.Builder(activity)
            .title(R.string.err_sign_in_title)
            .message(R.string.err_sign_in_google_desc)
            .primaryActionText(android.R.string.ok)
            .primaryActionTapListener(object : Flashbar.OnActionTapListener {
                override fun onActionTapped(bar: Flashbar) { bar.dismiss() }
            })
            .build()
            .show()
    }


    fun createAccount(emailString: String, passwordString: String): Task<AuthResult> {
        return FirebaseAuth.getInstance().createUserWithEmailAndPassword(emailString, passwordString)
    }

    fun signInWithEmail(email: String, password: String): Task<AuthResult> {
        return FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
    }

    /**
     * Fetches the username corresponding to the current user
     */
    fun getUsername(): DocumentReference? {
        FirebaseAuth.getInstance().currentUser?.uid?.let {
            return FirebaseFirestore.getInstance().collection("users_uid").document(it)
        }

        return null
    }

    /* ---------------- Constants ---------------- */

    const val TAG = "FirebaseAuthActions"
    const val RC_SIGN_IN = 4
}