package com.ms8.smartirhub.android

import android.app.Activity
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.os.Handler
import android.support.constraint.ConstraintSet
import android.support.v7.app.AppCompatActivity
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import com.andrognito.flashbar.Flashbar
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.FirebaseError.ERROR_CREDENTIAL_ALREADY_IN_USE
import com.google.firebase.FirebaseError.ERROR_EMAIL_ALREADY_IN_USE
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.ms8.smartirhub.android.databinding.ActivitySplashBinding
import com.ms8.smartirhub.android.firebase.FirebaseAuthActions
import com.ms8.smartirhub.android.utils.MySharedPreferences
import com.wajahatkarim3.easyvalidation.core.view_ktx.validator


class SplashActivity : AppCompatActivity() {
    /**
     * Contains links to all Views
     */
    private lateinit var binding : ActivitySplashBinding

    /**
     * Keeps track of activity metadata
     */
    private var splashState = SplashState()

    /**
     * Transition animation properties
     */
    private var layoutTransition = AutoTransition().apply {
        interpolator = AccelerateDecelerateInterpolator()
        duration = 700
    }

    /**
     * Used to wait until FirebaseAuth has initialized.
     * If the user has never signed in before -> show sign in layout
     * If the user has signed in, but never declared a username -> show username layout
     * If the user has completely signed in -> go to MainActivity
     */
    val authStateListener : FirebaseAuth.AuthStateListener = FirebaseAuth.AuthStateListener {
        when {
            // First time
            it.currentUser == null && splashState.layoutState == LayoutState.SHOW_SPLASH ->
                Handler().postDelayed({showSignIn(true)}, 50)
            // Already signed in
            it.currentUser != null && !splashState.isSigningIn() -> {
                when (MySharedPreferences.hasUsername(this) || it.currentUser!!.isAnonymous) {
                    true -> {
                        // Completely signed in
                        gotoMainPage()
                    }
                    false -> {
                        // Need to get username
                        showCreateUsername(true)
                    }
                }
            }
        }
    }

    /* -------------------- OnClick Functions -------------------- */

    /**
     * Attempts to sign user in with inputted email and password
     * If input is invalid, show error
     * If input is valid, attempt sign in
     *  If sign in successful, check for linked username
     *      If username is in offline storage, goto MainActivity
     *      If username is not in offline storage, find linked username
     *          If username is found, save in offline storage and goto MainActivity
     *          If username if not found, show create username layout
     *  If sign in unsuccessful, show error as to why it failed
     */
    private fun btnSignInClicked() {
        if (isValidEmailAndPassword()) {
            FirebaseAuthActions.signInWithEmail(binding.email.editText?.text.toString(),
                                                binding.password.editText?.text.toString())
                .addOnCompleteListener { task ->
                    when {
                        task.isSuccessful -> {
                            if (MySharedPreferences.hasUsername(this))
                                gotoMainPage()
                            else
                                FirebaseAuthActions.getUsername()?.get()
                                    ?.addOnSuccessListener {doc ->
                                        if (doc != null) {
                                            Log.d("TEST##", "found username: $doc")
                                            gotoMainPage()
                                            MySharedPreferences.setUsername(this, doc["username"] as String?)
                                        } else {
                                            showCreateUsername(true)
                                        }
                                    }
                                    ?.addOnFailureListener {exception ->
                                        Log.e(TAG, "get failed with $exception")
                                    }
                        }
                        task.exception is FirebaseAuthInvalidCredentialsException -> {
                            Flashbar.Builder(this)
                                .dismissOnTapOutside()
                                .enableSwipeToDismiss()
                                .duration(Flashbar.DURATION_SHORT)
                                .title(R.string.err_inv_email_pass)
                                .message(R.string.err_inv_email_pass_desc)
                                .positiveActionText(R.string.dismiss)
                                .positiveActionTapListener(object : Flashbar.OnActionTapListener {
                                    override fun onActionTapped(bar: Flashbar) = bar.dismiss()
                                }).build().show()
                        }
                        task.exception is FirebaseAuthInvalidUserException -> {
                            Flashbar.Builder(this)
                                .dismissOnTapOutside()
                                .enableSwipeToDismiss()
                                .duration(Flashbar.DURATION_SHORT)
                                .title(R.string.err_email_nonexistant)
                                .message(R.string.err_email_nonexistant_desc)
                                .positiveActionText(R.string.dismiss)
                                .positiveActionTapListener(object : Flashbar.OnActionTapListener {
                                    override fun onActionTapped(bar: Flashbar) = bar.dismiss()
                                }).build().show()
                        }
                        else -> {
                            Log.e(TAG, "Unknown sign in error (${task.exception})")
                            Flashbar.Builder(this)
                                .dismissOnTapOutside()
                                .enableSwipeToDismiss()
                                .title(R.string.err_sign_in_title)
                                .message(R.string.err_sign_in_desc)
                                .positiveActionText(R.string.dismiss)
                                .positiveActionTapListener(object : Flashbar.OnActionTapListener {
                                    override fun onActionTapped(bar: Flashbar) = bar.dismiss()
                                })
                                .negativeActionText(R.string.report_issue)
                                .negativeActionTapListener(object : Flashbar.OnActionTapListener {
                                    override fun onActionTapped(bar: Flashbar) {
                                        bar.dismiss()
                                        TODO("Not yet implemented")
                                    }
                                }).build().show()
                        }
                    }
                }
        }
    }

    /**
     * Attempt to sign in with Google
     */
    private fun btnGoogleSignInClicked() {
        FirebaseAuthActions.signInWithGoogle(this)
    }

    /**
     * If sign in layout is being shown, continue to username layout
     * If username layout is shown, check username for validity and set current user's username
     */
    private fun btnSignUpClicked() {
        when (splashState.layoutState) {
            LayoutState.SHOW_SIGN_IN -> {
                if (isValidEmailAndPassword())
                    FirebaseAuthActions.createAccount(binding.email.editText?.text.toString(), binding.password.editText?.text.toString())
                        .addOnCompleteListener { task ->
                            when {
                                task.isSuccessful -> {
                                    showCreateUsername(true)
                                }
                                task.exception is FirebaseAuthUserCollisionException -> {
                                    Flashbar.Builder(this)
                                        .dismissOnTapOutside()
                                        .enableSwipeToDismiss()
                                        .title(R.string.err_account_made_title)
                                        .message(R.string.err_acount_made_desc)
                                        .positiveActionText(R.string.dismiss)
                                        .positiveActionTapListener(object : Flashbar.OnActionTapListener {
                                            override fun onActionTapped(bar: Flashbar) = bar.dismiss()
                                        })
                                        .barDismissListener(object: Flashbar.OnBarDismissListener {
                                            override fun onDismissProgress(bar: Flashbar, progress: Float) {}
                                            override fun onDismissing(bar: Flashbar, isSwiped: Boolean) {}

                                            override fun onDismissed(bar: Flashbar, event: Flashbar.DismissEvent) {
                                                showSignIn(true)
                                            }
                                        })
                                        .build()
                                        .show()
                                }
                                else -> {
                                    Log.e(TAG, "Unexpected task result: ${task.result} (${task.exception})")
                                    Flashbar.Builder(this)
                                        .dismissOnTapOutside()
                                        .enableSwipeToDismiss()
                                        .title(R.string.err_sign_in_title)
                                        .message(R.string.err_sign_in_desc)
                                        .positiveActionText(R.string.dismiss)
                                        .positiveActionTapListener(object : Flashbar.OnActionTapListener {
                                            override fun onActionTapped(bar: Flashbar) = bar.dismiss()
                                        })
                                        .negativeActionText(R.string.report_issue)
                                        .negativeActionTapListener(object : Flashbar.OnActionTapListener {
                                            override fun onActionTapped(bar: Flashbar) {
                                                bar.dismiss()
                                                TODO("Not yet implemented")
                                            }
                                        })
                                        .build()
                                        .show()
                                }
                            }
                    }
            }
            LayoutState.SHOW_USERNAME -> {
                val username : String = binding.email.editText?.text.toString()
                val validUsername = username.validator()
                    .nonEmpty()
                    .noSpecialCharacters()
                    .minLength(5)
                    .maxLength(15)
                    .addErrorCallback {
                        binding.email.error = getString(R.string.err_invalid_username)
                    }.addSuccessCallback {
                        binding.email.error = ""
                    }
                    .check()

                if (validUsername) {
                    when {
                        // Signed in with Google
                        GoogleSignIn.getLastSignedInAccount(this) != null -> {
                            Log.d("TEST", "Adding username to google account")
                        }

                        // Create account with email and password
                        else -> {
                            Log.d("TEST", "Adding creating account from email and password")
                        }
                    }
                }
            }
            else -> Log.e(TAG, "btnSignUpClicked was called when state was ${splashState.layoutState.getString()}")
        }
    }

    /* -------------------- Transition Functions -------------------- */

    /**
     * Transition to the username layout
     */
    private fun showCreateUsername(bAnimate: Boolean) {
        Log.d("TEST###", "Starting Username Transition...")
        splashState.layoutState = LayoutState.SHOW_USERNAME

        // Clear input from edit texts
        binding.welcomeTitle.text = getString(R.string.almost_done)
        binding.welcomeDescription.text = getString(R.string.pick_username)
        binding.email.editText?.setText(splashState.usernameStr)
        binding.password.editText?.setText("")
        binding.btnSignUp.text = getString(R.string.finish_sign_up)

        // Get layout transition
        val newLayout = ConstraintSet().apply {
            clone(this@SplashActivity, R.layout.activity_splash_username)
            centerHorizontally(binding.email.id, ConstraintSet.PARENT_ID)
        }

        if (bAnimate) {
            TransitionManager.beginDelayedTransition(binding.splashContainer, layoutTransition)
        }
        newLayout.applyTo(binding.splashContainer)
    }

    /**
     * Transition to the sign in layout
     */
    private fun showSignIn(bAnimate : Boolean) {
        Log.d("TEST###", "Starting Sign In Transition...")
        splashState.layoutState = LayoutState.SHOW_SIGN_IN

        // Set Sign In Values
        binding.welcomeTitle.text = getString(R.string.welcomeTitle)
        binding.welcomeDescription.text = getString(R.string.welcome_desc)
        binding.email.editText?.setText(splashState.emailStr)
        binding.password.editText?.setText(splashState.passStr)
        binding.btnSignUp.text = getString(R.string.sign_up)

        // Get Layout transition
        val constraintSet = ConstraintSet().apply {
            clone(this@SplashActivity, R.layout.activity_splash_login)
            constrainHeight(binding.splashLogo.id, 375)
        }

        if (bAnimate) {
            TransitionManager.beginDelayedTransition(binding.splashContainer, layoutTransition)
        }
        constraintSet.applyTo(binding.splashContainer)
    }


    /**
     * Show the splash screen
     */
    private fun showSplash(bAnimate: Boolean) {
        splashState.layoutState = LayoutState.SHOW_SPLASH

        // Get Layout transition
        val constraintSet = ConstraintSet().apply {
            clone(this@SplashActivity, R.layout.activity_splash)
        }

        if (bAnimate) {
            TransitionManager.beginDelayedTransition(binding.splashContainer, layoutTransition)
        }
        constraintSet.applyTo(binding.splashContainer)
    }

    /* -------------------- Overridden Functions -------------------- */

    /**
     * Write all State variables to SavedInstanceState
     */
    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.let {
            it.putString(SPLASH_INSTANCE_STATE + "_LAYOUT", splashState.layoutState.getString())
            it.putString(SPLASH_INSTANCE_STATE + "_INPUT_EMAIL", splashState.emailStr)
            it.putString(SPLASH_INSTANCE_STATE + "_INPUT_PASS", splashState.passStr)
            it.putString(SPLASH_INSTANCE_STATE + "_INPUT_USERNAME", splashState.usernameStr)
        }
    }

    override fun onPause() {
        super.onPause()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
    }

    override fun onResume() {
        super.onResume()

        // Show sign in once FirebaseAuth has initialized
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    override fun onBackPressed() {
        when (splashState.layoutState) {
            LayoutState.SHOW_USERNAME -> { showSignIn(true) }
            else -> super.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)

        // Restore state
        savedInstanceState?.let {
            splashState.layoutState = layoutStateFromString(it.getString(SPLASH_INSTANCE_STATE + "_LAYOUT", SHOW_SPLASH))
            splashState.emailStr = it.getString(SPLASH_INSTANCE_STATE + "_INPUT_EMAIL", "")
            splashState.passStr = it.getString(SPLASH_INSTANCE_STATE + "_INPUT_PASS", "")
            splashState.usernameStr = it.getString(SPLASH_INSTANCE_STATE + "_INPUT_USERNAME", "")
        }

        // Bind click listeners
        binding.signInGoogle.setOnClickListener { btnGoogleSignInClicked() }
        binding.btnSignUp.setOnClickListener { btnSignUpClicked() }
        binding.btnSignIn.setOnClickListener { btnSignInClicked() }

        // Set proper constraints
        when (splashState.layoutState) {
            LayoutState.SHOW_SPLASH -> showSplash(false)
            LayoutState.SHOW_SIGN_IN -> showSignIn(false)
            LayoutState.SHOW_USERNAME -> showCreateUsername(false)
            else -> showSplash(false)
        }
    }

    /**
     * Check for successful sign in with Google and either show username layout or
     * show error message
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("TEST###", "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FirebaseAuthActions.RC_SIGN_IN -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Log.d("TEST###", "Result OK")
                        when (FirebaseAuthActions.handleGoogleSignInResult(data)) {
                            // No Error
                            null -> { showCreateUsername(true) }
                            // Google Sign In failed
                            else -> FirebaseAuthActions.showSignInWithGoogleError(this)
                        }
                    }
                    else -> Log.w("TEST####", "Result no ok... $resultCode")
                }
            }
            else -> Log.w(TAG, "Unknown request code ($requestCode)")
        }
    }

    /* -------------------- Helper Functions -------------------- */

    private fun gotoMainPage() = startActivity(Intent(this, MainActivity::class.java))

    /**
     * Does sanitation checks on email and password input
     */
    private fun isValidEmailAndPassword() : Boolean {
        val emailString : String = binding.email.editText?.text.toString()
        val passwordString : String = binding.password.editText?.text.toString()
        val validPassword = passwordString.validator()
            .nonEmpty()
            .atleastOneLowerCase()
            .atleastOneNumber()
            .atleastOneUpperCase()
            .minLength(5)
            .addErrorCallback {
                binding.password.error = getString(R.string.err_pass)
            }.addSuccessCallback {
                binding.password.error = ""
            }
            .check()
        val validEmail = emailString.validator()
            .validEmail()
            .addErrorCallback {
                binding.email.error = getString(R.string.err_invalid_email)
            }.addSuccessCallback {
                binding.email.error = ""
            }
            .check()

        return validEmail && validPassword
    }

    /* -------------------- Static Stuff -------------------- */

    companion object {

        const val SHOW_SPLASH = "STATE_SHOW_SPLASH"
        const val SHOW_SIGN_IN = "STATE_SHOW_SIGN_IN"
        const val SHOW_USER_NAME = "STATE_SHOW_USERNAME"
        const val DONE = "STATE_SPLASH_DONE"
        const val SPLASH_INSTANCE_STATE = "SPLASH_STATE"

        const val TAG = "SplashActivity"

        private fun layoutStateFromString(string: String): LayoutState {
            return when (string) {
                SHOW_SPLASH -> LayoutState.SHOW_SPLASH
                SHOW_SIGN_IN -> LayoutState.SHOW_SIGN_IN
                SHOW_USER_NAME -> LayoutState.SHOW_USERNAME
                DONE -> LayoutState.DONE
                else -> LayoutState.SHOW_SPLASH
            }
        }
    }

    class SplashState {
        var layoutState : LayoutState = LayoutState.SHOW_SPLASH
        var emailStr  = ""
        var passStr = ""
        var usernameStr = ""

        // Whether any sign in process is currently underway
        fun isSigningIn(): Boolean = (layoutState == LayoutState.SHOW_SIGN_IN ||  layoutState == LayoutState.SHOW_USERNAME)
    }

    enum class LayoutState {SHOW_SPLASH, SHOW_SIGN_IN, SHOW_USERNAME, DONE}

    private fun LayoutState.getString() : String {
        return when (this) {
            LayoutState.SHOW_SPLASH -> SHOW_SPLASH
            LayoutState.SHOW_SIGN_IN -> SHOW_SIGN_IN
            LayoutState.SHOW_USERNAME -> SHOW_USER_NAME
            LayoutState.DONE -> DONE
        }
    }
}
