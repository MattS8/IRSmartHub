package com.ms8.smartirhub.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableArrayMap
import androidx.databinding.ObservableMap
import android.os.Bundle
import android.os.Handler
import androidx.constraintlayout.widget.ConstraintSet
import androidx.appcompat.app.AppCompatActivity
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.andrognito.flashbar.Flashbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.ms8.smartirhub.android.data.Group
import com.ms8.smartirhub.android.data.RemoteProfile
import com.ms8.smartirhub.android.data.User
import com.ms8.smartirhub.android.database.LocalData
import com.ms8.smartirhub.android.databinding.ASplashBinding
import com.ms8.smartirhub.android.firebase.FirebaseAuthActions
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.utils.SignInUtils
import com.ms8.smartirhub.android.viewmodels.RemoteProfileViewModel

class SplashActivity2 : AppCompatActivity() {
    private val userGroupsListener = UserGroupsListener(null)

    lateinit var binding : ASplashBinding

    /**
     * Keeps track of activity metadata
     */
    private var splashState = SplashState()

    /**
     * Transition animation properties
     */
    private var layoutTransition = AutoTransition().apply {
        interpolator = AccelerateDecelerateInterpolator()
        duration = TRANSITION_DURATION.toLong()
    }

    /**
     * When user is logged in, start fetching user data
     */
    private val authStateListener : FirebaseAuth.AuthStateListener = FirebaseAuth.AuthStateListener { auth ->
        if (auth.currentUser == null) {
            Log.d("T#AuthState", "Not signed in")
            FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener { showSignInOptions(true) }
                .addOnFailureListener { showErrorWithAction(R.string.error, R.string.err_no_internet, R.string.report_issue,
                    { FirestoreActions.reportError("AuthState.SignInAnonymously")},
                    { android.os.Process.killProcess(android.os.Process.myPid()).also { System.exit(1) } })
                }
        } else if (splashState.layoutState == LayoutState.SHOW_SPLASH) {
            showSignInOptions(true)
        }
    }

/* ------------------------------------------------ OnClick Functions ----------------------------------------------- */

    private fun signUpWithEmail() {
        val email = binding.email.editText?.text.toString()
        val password = binding.password.editText?.text.toString()
        val passwordConfirm = binding.passwordConfirm.editText?.text.toString()

        Log.d("TEST", "password = $password")
        if (isValidEmailAndPassword(email, password) && passwordsMatch(password, passwordConfirm)) {
            binding.btnSignIn.onStartLoading()
            FirebaseAuthActions.createAccount(email, password)
                .addOnSuccessListener { showCreateUsername(true) }
                .addOnFailureListener { e -> onSignInError(e) }
        }
    }

    private fun signInWithGoogle() {
        FirebaseAuthActions.signInWithGoogle(this)
    }

    private fun signInWithEmail() {
        val email = binding.email.editText?.text.toString()
        val password = binding.password.editText?.text.toString()

        if (isValidEmailAndPassword(email, password)) {
            binding.btnSignIn.onStartLoading()
            FirebaseAuthActions.signInWithEmail(email, password)
                .addOnSuccessListener { onSignInSuccess() }
                .addOnFailureListener { e ->  onSignInError(e) }
        }
    }

    private fun createUser() {
        val username : String = binding.email.editText?.text.toString()
        if (isValidUsername(username)) {
            binding.btnSignIn.onStartLoading()
            FirestoreActions.createNewUser(username)
                .addOnSuccessListener { onSignInSuccess() }
                .addOnFailureListener { e -> onSignInError(e) }
        }
    }

/* ------------------------------------------- Layout Transition Functions ------------------------------------------ */

    private fun showSignInOptions(bAnimate: Boolean) {
        Log.d(TAG, "Showing sign in options (bAnimnate = $bAnimate)")
        splashState.layoutState = LayoutState.SHOW_SIGN_IN_OPTIONS

        binding.btnSignIn.onStopLoading()

        binding.welcomeTitle.text = getString(R.string.welcomeTitle)
        binding.welcomeDescription.text = getString(R.string.welcome_desc)
        binding.btnSignIn.setButtonText(getString(R.string.sign_in))

        // Set onClick Listeners
        binding.btnSignIn.setOnClickListener { showSignIn(true) }
        binding.btnSignUp.setOnClickListener { showSignUp(true) }
        binding.signInGoogle.setOnClickListener { signInWithGoogle() }

        val newLayout = ConstraintSet().apply {
            clone(this@SplashActivity2, R.layout.a_splash_show_options)
            constrainHeight(binding.splashLogo.id, 375)
//            constrainHeight(binding.password.id, 1)
//            constrainHeight(binding.passwordConfirm.id, 1)
//            constrainHeight(binding.email.id, 1)
        }

        if (bAnimate) TransitionManager.beginDelayedTransition(binding.splashContainer, layoutTransition)

        newLayout.applyTo(binding.splashContainer)


        binding.password.editText?.isEnabled = false
        binding.passwordConfirm.editText?.isEnabled = false
        binding.email.editText?.isEnabled = false
        binding.selectUsername.editText?.isEnabled = false
    }

    private fun showSignUp(bAnimate: Boolean) {
        splashState.layoutState = LayoutState.SHOW_SIGN_UP

        binding.btnSignIn.onStopLoading()

        // Set Sign In Values
        binding.welcomeTitle.text = getString(R.string.welcomeTitle)
        binding.welcomeDescription.text = getString(R.string.welcome_sign_up)
        binding.email.editText?.setText(splashState.emailStr)
        binding.password.editText?.setText(splashState.passStr)
        binding.passwordConfirm.editText?.setText(splashState.passConfirmStr)
        binding.btnSignIn.setButtonText(getString(R.string.sign_up))
        binding.passwordConfirm.hint = getString(R.string.passwordConfirm)
        binding.email.error = ""
        binding.password.error = ""
        binding.passwordConfirm.error = ""

        // Set onClick Listeners
        binding.btnSignIn.setOnClickListener { signUpWithEmail() }

        // Get Layout transition
        val constraintSet = ConstraintSet().apply {
            clone(this@SplashActivity2, R.layout.a_splash_sign_in)
            constrainHeight(binding.splashLogo.id, 375)
            constrainHeight(binding.signInGoogle.id, 1)
        }

        if (bAnimate) {
            TransitionManager.beginDelayedTransition(binding.splashContainer, layoutTransition)
        }
        constraintSet.applyTo(binding.splashContainer)
        binding.passwordConfirm.alpha = 1f
        if (bAnimate) {
            Handler().postDelayed({
                if (splashState.layoutState == LayoutState.SHOW_SIGN_UP)
                    binding.signInGoogle.alpha = 0f
            }, TRANSITION_DURATION.toLong())
        } else {
            binding.signInGoogle.alpha = 0f
        }


        binding.password.editText?.isEnabled = true
        binding.passwordConfirm.editText?.isEnabled = true
        binding.email.editText?.isEnabled = true
        binding.selectUsername.editText?.isEnabled = false
    }

    private fun showSignIn(bAnimate: Boolean) {
        splashState.layoutState = LayoutState.SHOW_SIGN_IN

        binding.btnSignIn.onStopLoading()

        // Set Sign In Values
        binding.welcomeTitle.text = getString(R.string.welcomeTitle)
        binding.welcomeDescription.text = getString(R.string.welcome_sign_in)
        binding.email.editText?.setText(splashState.emailStr)
        binding.password.editText?.setText(splashState.passStr)
        binding.btnSignIn.setButtonText(getString(R.string.sign_in))
        binding.email.error = ""
        binding.password.error = ""

        // Set onClick Listeners
        binding.btnSignIn.setOnClickListener { signInWithEmail() }

        // Get Layout transition
        val constraintSet = ConstraintSet().apply {
            clone(this@SplashActivity2, R.layout.a_splash_sign_in)
            constrainHeight(binding.splashLogo.id, 375)
            constrainHeight(binding.passwordConfirm.id, 1)
            constrainHeight(binding.signInGoogle.id, 1)
            setAlpha(binding.passwordConfirm.id, 0f)
        }

        if (bAnimate) TransitionManager.beginDelayedTransition(binding.splashContainer, layoutTransition)

        constraintSet.applyTo(binding.splashContainer)
        binding.passwordConfirm.alpha = 0f

        if (bAnimate) {
            Handler().postDelayed({
                if (splashState.layoutState == LayoutState.SHOW_SIGN_IN)
                    binding.signInGoogle.alpha = 0f
            }, TRANSITION_DURATION.toLong())
        } else {
            binding.signInGoogle.alpha = 0f
        }


        binding.password.editText?.isEnabled = true
        binding.passwordConfirm.editText?.isEnabled = false
        binding.email.editText?.isEnabled = true
        binding.selectUsername.editText?.isEnabled = false
    }

    private fun showSplash(bAnimate: Boolean) {
        splashState.layoutState = LayoutState.SHOW_SPLASH

        val newLayout = ConstraintSet().apply { clone(this@SplashActivity2, R.layout.a_splash) }
        if (bAnimate) { TransitionManager.beginDelayedTransition(binding.splashContainer, layoutTransition) }
        newLayout.applyTo(binding.splashContainer)
    }

    private fun showCreateUsername(bAnimate: Boolean) {
        splashState.layoutState = LayoutState.SHOW_USERNAME

        binding.btnSignIn.onStopLoading()

        // Disable Inputs
        binding.passwordConfirm.hint = getString(R.string.username)
        binding.passwordConfirm.editText?.setText(splashState.usernameStr)
        binding.btnSignIn.setButtonText(getString(R.string.choose_username))
        binding.btnSignIn.setOnClickListener { createUser() }

        val newLayout = ConstraintSet().apply {
            clone(this@SplashActivity2, R.layout.a_splash_username)
        }

        if (bAnimate) TransitionManager.beginDelayedTransition(binding.splashContainer, layoutTransition)
        newLayout.applyTo(binding.splashContainer)
        binding.selectUsername.editText?.isEnabled = true
    }

/* ---------------------------------------------- Show Error Functions ---------------------------------------------- */

    private fun showErrorWithAction(titleRes: Int, messageRes: Int, negText: Int, onNegAction : () -> Any,
                                    onDismissed: () -> Any?) {
        Flashbar.Builder(this)
            .dismissOnTapOutside()
            .enableSwipeToDismiss()
            .title(titleRes)
            .backgroundColorRes(R.color.colorPrimaryDark)
            .positiveActionTextColorRes(R.color.colorAccent)
            .titleColorRes(android.R.color.holo_red_dark)
            .message(messageRes)
            .negativeActionText(negText)
            .positiveActionText(R.string.dismiss)
            .positiveActionTapListener(object :Flashbar.OnActionTapListener {
                override fun onActionTapped(bar: Flashbar) { bar.dismiss() }
            })
            .barDismissListener(object : Flashbar.OnBarDismissListener {
                override fun onDismissProgress(bar: Flashbar, progress: Float) {}
                override fun onDismissing(bar: Flashbar, isSwiped: Boolean) { }
                override fun onDismissed(bar: Flashbar, event: Flashbar.DismissEvent) {
                    bar.dismiss()
                    onDismissed()
                }
            })
            .negativeActionTapListener(object : Flashbar.OnActionTapListener {
                override fun onActionTapped(bar: Flashbar) {
                    bar.dismiss()
                    onNegAction()
                }
            })
            .build()
            .show()
    }

    /**
     * Shows an error message with title: [titleRes] and message: [messageRes]
     * and runs [onDismissed] when [Flashbar] is dismissed.
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


/* ---------------------------------------------- Overridden Functions ---------------------------------------------- */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bind layout
        binding = DataBindingUtil.setContentView(this, R.layout.a_splash)

        // Restore state or start up
        if (savedInstanceState != null) {
            splashState.layoutState = layoutStateFromString(savedInstanceState
                .getString(SPLASH_INSTANCE_STATE + "_LAYOUT", SHOW_SPLASH))
            splashState.emailStr = savedInstanceState.getString(SPLASH_INSTANCE_STATE + "_INPUT_EMAIL", "")
            splashState.passStr = savedInstanceState.getString(SPLASH_INSTANCE_STATE + "_INPUT_PASS", "")
            splashState.usernameStr = savedInstanceState.getString(SPLASH_INSTANCE_STATE + "_INPUT_USERNAME", "")
            splashState.passConfirmStr = savedInstanceState.getString(SPLASH_INSTANCE_STATE + "_INPUT_PASS_CONFRM", "")
        }

        // Set proper constraints
        when (splashState.layoutState) {
            LayoutState.SHOW_SPLASH -> showSplash(false)
            LayoutState.SHOW_SIGN_IN -> showSignIn(false)
            LayoutState.SHOW_SIGN_UP -> showSignUp(false)
            LayoutState.SHOW_USERNAME -> showCreateUsername(false)
            LayoutState.SHOW_SIGN_IN_OPTIONS -> showSignInOptions(false)
            else -> showSplash(false)
        }
        val remoteProfilesViewModel = ViewModelProviders.of(this).get(RemoteProfileViewModel::class.java)
        remoteProfilesViewModel.getRemoteProfiles().observe(this, Observer<HashMap<String, RemoteProfile>> {

        })
    }

    /**
     * Checks for successful sign-in with Google.
     * * If error -> [showError]
     * * If no error -> ]
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("TEST###", "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FirebaseAuthActions.RC_SIGN_IN -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Log.d("TEST###", "Result OK")
                        handleGoogleSignInResult(data)
                    }
                    else -> Log.w("TEST####", "Result no ok... $resultCode")
                }
            }
            else -> Log.w(TAG, "Unknown request code ($requestCode)")
        }
    }

    override fun onResume() {
        super.onResume()
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        LocalData.userGroups.addOnMapChangedCallback(userGroupsListener.apply { context = this@SplashActivity2 })
    }

    override fun onPause() {
        super.onPause()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        LocalData.userGroups.removeOnMapChangedCallback(userGroupsListener.apply { context = null })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState?.let {
            it.putString(SPLASH_INSTANCE_STATE + "_LAYOUT", splashState.layoutState.getString())
            it.putString(SPLASH_INSTANCE_STATE + "_INPUT_EMAIL", splashState.emailStr)
            it.putString(SPLASH_INSTANCE_STATE + "_INPUT_PASS", splashState.passStr)
            it.putString(SPLASH_INSTANCE_STATE + "_INPUT_USERNAME", splashState.usernameStr)
            it.putString(SPLASH_INSTANCE_STATE + "_INPUT_PASS_CONFRM", splashState.passConfirmStr )
        }
    }

    override fun onBackPressed() {
        when (splashState.layoutState) {
            LayoutState.SHOW_USERNAME -> showSignInOptions(true)
            LayoutState.SHOW_SIGN_IN -> showSignInOptions(true)
            LayoutState.SHOW_SIGN_UP -> showSignInOptions(true)
            else -> super.onBackPressed()
        }
    }

/* ------------------------------------------ Firebase Listening Functions ------------------------------------------ */

    /**
     * When a listener failure occurs, an error is shown. Otherwise, the user data is stored
     * and the sign in process continues by fetching all group info
     */
    private fun onSignInSuccess() {
        Log.d(TAG, "onSignInSuccess (${FirebaseAuth.getInstance().currentUser?.displayName})")
        binding.btnSignIn.onStartLoading()
        FirestoreActions.getUserFromUID()
            .addOnSuccessListener { querySnapshot ->
                Log.d(TAG, "Success: isEmpty = ${querySnapshot.isEmpty}")
                when {
                    querySnapshot.isEmpty -> {
                        binding.btnSignIn.onStopLoading()
                        showCreateUsername(true)
                    }
                    else -> {
                        if (querySnapshot.size() > 1)
                            Log.e(TAG, "Received more than one user object from uid:" +
                                    " ${FirebaseAuth.getInstance().currentUser?.uid}")

                        LocalData.setupUser(querySnapshot.toObjects(User::class.java)[0], querySnapshot.documents[0].id)
                        FirestoreActions.listenToUserGroups()
                        FirestoreActions.listenToRemoteProfiles()
                    }
                }
            }
            .addOnFailureListener { e ->
                binding.btnSignIn.onStopLoading()
                Log.e(TAG, "Username query failed for user with uid:" +
                        " ${FirebaseAuth.getInstance().currentUser?.uid} ($e)")
                onSignInError(e)
            }
    }

/* ------------------------------------------------ Helper Functions ------------------------------------------------ */

    /**
     * Handles whether to show an error or continue with sign in process based on Google sign in result
     */
    private fun handleGoogleSignInResult(data: Intent?) {
        when (val task = FirebaseAuthActions.handleGoogleSignInResult2(data)) {
            // Google Sign In failed
            null -> showErrorWithAction(R.string.error, R.string.err_sign_in_desc, R.string.report_issue,
                {FirestoreActions.reportError("Error signing in with Google")}, {})
            // Check task result
            else -> {
                task.addOnSuccessListener { onSignInSuccess() }
                    .addOnFailureListener {e -> onSignInError(e) }
            }
        }
    }

    /**
     * Handles what message to show a user based on sign-in error.
     */
    private fun onSignInError(exception: Exception) {
        when (exception) {
            is FirebaseAuthUserCollisionException -> showError(R.string.err_account_made_title, R.string.err_acount_made_desc)
            is FirebaseAuthInvalidCredentialsException -> showError(R.string.err_inv_email_pass, R.string.err_inv_email_pass_desc)
            is FirebaseAuthInvalidUserException -> showError(R.string.err_email_nonexistant, R.string.err_email_nonexistant_desc)
            is FirebaseFirestoreException -> {
                when (exception.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> {showError(R.string.username_taken_title,
                        R.string.err_username_taken_desc)}
                    else -> { showUnknownError(exception) }
                }
            }
            else -> { showUnknownError(exception) }
        }
    }

    private fun showUnknownError(exception: Exception) {
        val errorMessage = "Unexpected task result from signInWithEmail: ($exception)"
        Log.e(TAG, errorMessage)
        showErrorWithAction(R.string.error, R.string.err_sign_in_desc, R.string.report_issue,
            {FirestoreActions.reportError(errorMessage)},
            {})
    }

    /**
     * Handles showing error messages if email and/or pass input(s) cre invalid
     */
    private fun isValidEmailAndPassword(email: String, password: String): Boolean {
        binding.password.error = ""
        binding.email.error = ""

        return SignInUtils.PasswordValidator(password)
                .addErrorCallback { binding.password.error = getString(R.string.err_pass) }
                .check() &&
               SignInUtils.EmailValidator(email)
                .addErrorCallback { binding.email.error = getString(R.string.err_invalid_email) }
                .check()
    }

    /**
     * Handles showing error messages if passwords don't match
     */
    private fun passwordsMatch(password: String, passwordConfirm: String): Boolean {
        binding.passwordConfirm.error = ""
        binding.password.error = ""

        return when (password) {
            passwordConfirm -> true
            else -> {
                binding.password.error = getString(R.string.err_pass_match)
                binding.passwordConfirm.error = getString(R.string.err_pass_match)
                false
            }
        }
    }

    /**
     * Handles showing error message if username is invalid
     */
    private fun isValidUsername(username: String) : Boolean {
        binding.passwordConfirm.error = ""

        return SignInUtils.UsernameValidator(username)
            .addErrorCallback { binding.passwordConfirm.error = getString(R.string.err_invalid_username) }
            .check()
    }


/* -------------------------------------------------- Static Stuff -------------------------------------------------- */

    companion object {

        const val SHOW_SPLASH = "STATE_SHOW_SPLASH"
        const val SHOW_SIGN_IN = "STATE_SHOW_SIGN_IN"
        const val SHOW_USER_NAME = "STATE_SHOW_USERNAME"
        const val SHOW_SIGN_UP = "STATE_SHOW_SIGN_UP"
        const val SHOW_SIGN_IN_OPTIONS = "STATE_SHOW_SIGN_IN_OPTIONS"
        const val DONE = "STATE_SPLASH_DONE"
        const val SPLASH_INSTANCE_STATE = "SPLASH_STATE"

        const val TAG = "SplashActivity"

        const val TRANSITION_DURATION = 700

        private fun layoutStateFromString(string: String): LayoutState {
            return when (string) {
                SHOW_SPLASH -> LayoutState.SHOW_SPLASH
                SHOW_SIGN_IN -> LayoutState.SHOW_SIGN_IN
                SHOW_SIGN_UP -> LayoutState.SHOW_SIGN_UP
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
        var passConfirmStr = ""

        // Whether any sign in process is currently underway
        fun isSigningIn(): Boolean = (layoutState == LayoutState.SHOW_SIGN_IN ||  layoutState == LayoutState.SHOW_USERNAME)
    }

    enum class LayoutState {SHOW_SPLASH, SHOW_SIGN_IN, SHOW_USERNAME, DONE, SHOW_SIGN_IN_OPTIONS, SHOW_SIGN_UP}

    private fun LayoutState.getString() : String {
        return when (this) {
            LayoutState.SHOW_SPLASH -> SHOW_SPLASH
            LayoutState.SHOW_SIGN_IN -> SHOW_SIGN_IN
            LayoutState.SHOW_SIGN_UP -> SHOW_SIGN_UP
            LayoutState.SHOW_USERNAME -> SHOW_USER_NAME
            LayoutState.DONE -> DONE
            LayoutState.SHOW_SIGN_IN_OPTIONS -> SHOW_SIGN_IN_OPTIONS
        }
    }

    class UserGroupsListener(var context: Context?) : ObservableMap.OnMapChangedCallback<ObservableArrayMap<String, Group>, String, Group>() {
        override fun onMapChanged(sender: ObservableArrayMap<String, Group>?, key: String?) {
            val groupSize = LocalData.user?.groups?.size ?: -1
            if (LocalData.user != null && groupSize == LocalData.userGroups.size) {
                Log.d(TAG, "Done fetching user groups... (${LocalData.user!!.groups.size} == ${LocalData.userGroups.size}")
                context?.startActivity(Intent(context, MainViewActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
        }
    }

}
