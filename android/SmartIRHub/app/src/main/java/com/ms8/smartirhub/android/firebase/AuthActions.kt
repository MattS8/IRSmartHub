package com.ms8.smartirhub.android.firebase

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.andrognito.flashbar.Flashbar
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.utils.MySharedPreferences
import java.lang.Exception

object AuthActions {

    //TODO: Removal
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
            .requestEmail()
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
    fun handleGoogleSignInResult2(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account!!.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnSuccessListener { FirestoreActions.getUserFromUID() }
                .addOnFailureListener { e -> AppState.errorData.userSignInError.set(e) }

        } catch (e : Exception) {
            AppState.errorData.userSignInError.set(e)
        }
    }

    fun createAccount(emailString: String, passwordString: String) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(emailString, passwordString)
            .addOnSuccessListener { AppState.userData.user.uid.set(FirebaseAuth.getInstance().currentUser!!.uid) }
            .addOnFailureListener { e -> AppState.errorData.userSignInError.set(e) }
    }

    fun signInWithEmail(email: String, password: String, fetchUsername : Boolean = true) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                if (fetchUsername) {
                    FirestoreActions.getUserFromUID()
                } else {
                    AppState.userData.user.uid.set(FirebaseAuth.getInstance().currentUser!!.uid) }
                }
            .addOnFailureListener { e -> AppState.errorData.userSignInError.set(e) }
    }

    fun signOut(context: Context) {
        MySharedPreferences.removeUser(context)
        AppState.userData.removeData()
        FirebaseAuth.getInstance().signOut()
        FirestoreActions.removeAllListeners()
    }

    /* ---------------- Constants ---------------- */

    const val TAG = "FirebaseAuthActions"
    const val RC_SIGN_IN = 4
}