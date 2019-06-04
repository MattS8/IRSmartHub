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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.ms8.smartirhub.android.databinding.ActivitySplashBinding
import com.ms8.smartirhub.android.firebase.FirebaseAuthActions
import com.ms8.smartirhub.android.firebase.FirestoreActions
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
     * Used to wait until [FirebaseAuth] has initialized.
     * * If the user has never signed in before -> [showSignIn]
     * * If the user has signed in, but never declared a username -> [showCreateUsername]
     * * If the user has completely signed in -> [gotoMainPage]
     */
    private val authStateListener : FirebaseAuth.AuthStateListener = FirebaseAuth.AuthStateListener {
        val bHasSeenSplash = MySharedPreferences.hasSeenSplash(this)
        val bHasUsername = MySharedPreferences.hasUsername(this)
        Log.d("TEST", "bHasSeenSplash = $bHasSeenSplash")
        when {
            // First time
            splashState.layoutState == LayoutState.SHOW_SPLASH && !bHasSeenSplash ->
                Handler().postDelayed({showSignIn(true)}, 50)
            // Already signed in and seen splash screen
            it.currentUser != null && bHasUsername && bHasSeenSplash -> {
                gotoMainPage()
            }
        }
    }

    /* -------------------- OnClick Functions -------------------- */

    /**
     * Attempts to sign user in with inputted email and password.
     * * If input is invalid -> [showError]
     * * If input is valid -> attempt sign in:
     *      * If sign-in successful -> check for linked username (see [onSignInSuccess])
     * * If sign in unsuccessful -> [showError]
     */
    private fun btnSignInClicked() {
        val email = binding.email.editText?.text.toString()
        val password = binding.password.editText?.text.toString()
        if (isValidEmailAndPassword(email, password)) {
            FirebaseAuthActions.signInWithEmail(email, password).addOnCompleteListener { task ->
                when {
                    task.isSuccessful -> onSignInSuccess()
                    task.exception is FirebaseAuthInvalidCredentialsException ->
                        showError(R.string.err_inv_email_pass, R.string.err_inv_email_pass_desc)
                    task.exception is FirebaseAuthInvalidUserException ->
                        showError(R.string.err_email_nonexistant, R.string.err_email_nonexistant_desc)
                    else -> {
                        Log.e(TAG, "Unknown sign in error (${task.exception})")
                        showUnknownError()
                    }
                }
            }
        }
    }

    /**
     * Handles logic when [btnSignInClicked] is successful.
     * * If username is found ->  [gotoMainPage]
     * * If username if not found -> [showCreateUsername]
     */
    private fun onSignInSuccess() {
        Log.d("TEST##", "onSignInSuccess")
        FirestoreActions.getUserFromUID()
            .addOnSuccessListener { querySnapshot ->
                when {
                    querySnapshot.isEmpty -> showCreateUsername(true)
                    else -> {
                        MySharedPreferences.setUsername(this, querySnapshot.documents[0].id)
                        gotoMainPage()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Username query failed for user with uid: ${FirebaseAuth.getInstance().currentUser?.uid} ($e)")
                showUnknownError()
            }
    }

    /**
     * Handles logic when sign-up button is clicked.
     * * If sign in layout is being shown -> [showCreateUsername]
     * * If username layout is shown -> check username validity:
     *  * If username is not valid -> [showError]
     *  * If username already exists -> [showError]
     *  * If username is valid and doesn't exist -> link username to account:
     *      * If success -> [gotoMainPage]
     *      * If failure -> [showError]
     */
    private fun btnSignUpClicked() {
        when (splashState.layoutState) {
            LayoutState.SHOW_SIGN_IN -> {
                val email = binding.email.editText?.text.toString()
                val password = binding.password.editText?.text.toString()
                Log.d("TEST", "password = $password")
                if (isValidEmailAndPassword(email, password))
                    FirebaseAuthActions.createAccount(email, password)
                        .addOnCompleteListener { task ->
                            when {
                                task.isSuccessful -> { showCreateUsername(true) }
                                task.exception is FirebaseAuthUserCollisionException ->
                                    showError(R.string.err_account_made_title, R.string.err_acount_made_desc)
                                else -> {
                                    Log.e(TAG, "Unexpected task result: ${task.result} (${task.exception})")
                                    showUnknownError()
                                }
                            }
                    }
            }
            // Finishing Sign Up Process
            LayoutState.SHOW_USERNAME -> {
                val username : String = binding.email.editText?.text.toString()
                if (isValidUsername(username)) {
                    FirestoreActions.createNewUser(username)
                        .addOnSuccessListener {
                            MySharedPreferences.setUsername(this, username)
                            gotoMainPage()
                        }
                        .addOnFailureListener { e ->
                            when {
                                e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                                    binding.email.editText?.setText("")
                                    showError(R.string.username_taken_title, R.string.err_username_taken_desc)
                                }
                                else -> showUnknownError()
                            }
                        }
                }
            }
            else -> Log.e(TAG, "btnSignUpClicked was called when state was ${splashState.layoutState.getString()}")
        }
    }

    /**
     * Attempts to sign in with Google.
     */
    private fun btnGoogleSignInClicked() {
        FirebaseAuthActions.signInWithGoogle(this)
    }

    /* -------------------- Transition Functions -------------------- */

    /**
     * Transitions to the username layout.
     */
    private fun showCreateUsername(bAnimate: Boolean) {
        Log.d("TEST###", "Starting Username Transition...")
        splashState.layoutState = LayoutState.SHOW_USERNAME

        // Clear input from edit texts
        binding.welcomeTitle.text = getString(R.string.almost_done)
        binding.welcomeDescription.text = getString(R.string.pick_username)
        binding.email.editText?.setText(splashState.usernameStr)
        binding.email.error = ""
        binding.email.hint = getString(R.string.username)
        binding.password.editText?.setText("")
        binding.password.error = ""
        binding.btnSignUp.text = getString(R.string.finish_sign_up)

        // Disable Unused Buttons
        binding.btnSignIn.isEnabled = false
        binding.btnSkip.isEnabled = false
        binding.signInGoogle.isEnabled = false
        binding.password.isEnabled = false

        // Get layout transition
        val newLayout = ConstraintSet().apply {
            clone(this@SplashActivity, R.layout.activity_splash_username)
            centerVertically(binding.btnSignUp.id, binding.email.id, ConstraintSet.BOTTOM, 16, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 16, 0.0f)
            centerHorizontally(binding.btnSignUp.id, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0, 0.0f)
            setVerticalBias(binding.email.id, 0.25f)
            constrainHeight(binding.signInGoogle.id, 1)
            constrainHeight(binding.password.id, 1)
            constrainHeight(binding.btnSignIn.id, 1)
            constrainHeight(binding.btnSkip.id, 1)
            setAlpha(binding.signInGoogle.id, 0.0f)
            setAlpha(binding.password.id, 0.0f)
            setAlpha(binding.btnSignIn.id, 0.0f)
            setAlpha(binding.btnSkip.id, 0.0f)
        }

        if (bAnimate) {
            TransitionManager.beginDelayedTransition(binding.splashContainer, layoutTransition)
        }
        newLayout.applyTo(binding.splashContainer)
    }

    /**
     * Transitions to the sign in layout.
     */
    private fun showSignIn(bAnimate : Boolean) {
        Log.d("TEST###", "Starting Sign In Transition...")
        splashState.layoutState = LayoutState.SHOW_SIGN_IN

        // Set Sign In Values
        binding.welcomeTitle.text = getString(R.string.welcomeTitle)
        binding.welcomeDescription.text = getString(R.string.welcome_desc)
        binding.email.editText?.setText(splashState.emailStr)
        binding.email.hint = getString(R.string.email_address)
        binding.password.editText?.setText(splashState.passStr)
        binding.btnSignUp.text = getString(R.string.sign_up)
        binding.email.error = ""
        binding.password.error = ""

        // Enable Buttons
        binding.btnSignIn.isEnabled = true
        binding.btnSkip.isEnabled = true
        binding.signInGoogle.isEnabled = true
        binding.password.isEnabled = true

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
     * Transitions to the splash screen layout.
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
        if (FirebaseAuth.getInstance().currentUser == null)
            FirebaseAuth.getInstance().signInAnonymously()
        Log.d("TEST####", "Current user uid = ${FirebaseAuth.getInstance().currentUser?.uid}")
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
        binding.btnSkip.setOnClickListener { gotoMainPage() }

        // Set proper constraints
        when (splashState.layoutState) {
            LayoutState.SHOW_SPLASH -> showSplash(false)
            LayoutState.SHOW_SIGN_IN -> showSignIn(false)
            LayoutState.SHOW_USERNAME -> showCreateUsername(false)
            else -> showSplash(false)
        }
    }

    /**
     * Checks for successful sign-in with Google.
     * * If error -> [showError]
     * * If no error -> check for username in offline storage:
     *  * If username found -> [gotoMainPage]
     *  * If no username found -> [showCreateUsername]
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
                            null -> onSignInSuccess()
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

    /* -------------------- Show Error Functions -------------------- */

    /**
     * Shows an error message with title: [titleRes] and message: [messageRes]
     * and run [onDismissed] when [Flashbar] is dismissed.
     */
    private fun showError(titleRes : Int, messageRes : Int, onDismissed : () -> Any? ) {
        Flashbar.Builder(this)
            .dismissOnTapOutside()
            .enableSwipeToDismiss()
            .title(titleRes)
            .backgroundColorRes(R.color.colorPrimaryDark)
            .positiveActionTextColorRes(R.color.colorAccent)
            .titleColorRes(android.R.color.holo_red_dark)
            .message(messageRes)
            .positiveActionText(R.string.dismiss)
            .positiveActionTapListener(object : Flashbar.OnActionTapListener {
                override fun onActionTapped(bar: Flashbar) = bar.dismiss()
            })
            .barDismissListener(object: Flashbar.OnBarDismissListener {
                override fun onDismissProgress(bar: Flashbar, progress: Float) {}
                override fun onDismissing(bar: Flashbar, isSwiped: Boolean) {}

                override fun onDismissed(bar: Flashbar, event: Flashbar.DismissEvent) {
                    onDismissed()
                }
            })
            .build()
            .show()
    }

    /**
     * See [showError]([titleRes] : Int, [messageRes] : Int, [onDismissed] : () -> Any?)
     */
    private fun showError(titleRes : Int, messageRes : Int) = showError(titleRes, messageRes) {}

    /**
     * Shows an error whenever there's a potential bug in the sign in process.
     */
    private fun showUnknownError() {
        Flashbar.Builder(this)
            .dismissOnTapOutside()
            .enableSwipeToDismiss()
            .titleColorRes(android.R.color.holo_red_dark)
            .backgroundColorRes(R.color.colorPrimaryDark)
            .positiveActionTextColorRes(R.color.colorAccent)
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

    /* -------------------- Helper Functions -------------------- */

    private fun gotoMainPage() {
        MySharedPreferences.setHasSeenSplash(this,true)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    /**
     * Does sanitation checks on [emailString] and [passwordString] input.
     */
    private fun isValidEmailAndPassword(emailString : String, passwordString : String) : Boolean {
        binding.email.error = ""
        binding.email.error = ""
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

    /**
     * Does sanitation checks on [username] input.
     */
    private fun isValidUsername(username: String): Boolean {
        binding.email.error = ""
        return username.validator()
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
