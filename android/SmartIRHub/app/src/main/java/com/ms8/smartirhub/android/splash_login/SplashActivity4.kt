package com.ms8.smartirhub.android.splash_login

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.databinding.ObservableArrayMap
import androidx.databinding.ObservableMap
import com.andrognito.flashbar.Flashbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.ASplashLoginMainBinding
import com.ms8.smartirhub.android.firebase.AuthActions
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.main_view.MainViewActivity
import com.ms8.smartirhub.android.models.firestore.Hub
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import com.ms8.smartirhub.android.utils.MyValidators

class SplashActivity4 : AppCompatActivity() {
    lateinit var binding : ASplashLoginMainBinding

    var waitingOnGoogleSignIn = false
    var layoutState = SHOW_SPLASH

    /**
     * Transition animation properties
     */
    private var layoutTransition = androidx.transition.AutoTransition().apply {
        interpolator = DecelerateInterpolator()
        duration = TRANSITION_DURATION.toLong()
    }

    companion object {
        const val SHOW_SPLASH = 0
        const val SHOW_OPTIONS = 1
        const val SHOW_SIGN_IN = 2
        const val SHOW_SIGN_UP = 3
        const val SHOW_USERNAME = 4

        const val TRANSITION_DURATION = 800
    }

/*
    ------------------------------------------------
        Listeners
    -----------------------------------------------
*/

    private val remotesListener = object : ObservableMap.OnMapChangedCallback<ObservableArrayMap<String, RemoteProfile>, String, RemoteProfile>() {
        override fun onMapChanged(sender: ObservableArrayMap<String, RemoteProfile>?, key: String?) { checkLoginState() }
    }

    private val hubsListener = object : ObservableMap.OnMapChangedCallback<ObservableArrayMap<String, Hub>, String, Hub>() {
        override fun onMapChanged(sender: ObservableArrayMap<String, Hub>?, key: String?) { checkLoginState() }
    }

    private val usernameListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) { checkLoginState() }

    }

    private val uidListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) { checkLoginState() }

    }

    private val errorListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            if (AppState.errorData.userSignInError.get() != null) {
                stopLoadingViews()
                onSignInError(AppState.errorData.userSignInError.get()!!)
                AppState.errorData.userSignInError.set(null)
            }
        }
    }

    private fun checkLoginState() {
        if (FirebaseAuth.getInstance().currentUser != null
            && AppState.userData.user.uid.get() != ""
            && AppState.userData.user.username.get() != ""
            && AppState.userData.remotes.size == AppState.userData.user.remotes.size
            && AppState.userData.hubs.size == AppState.userData.user.hubs.size)
            nextActivity()

        Log.d("TEST###", "uid = ${AppState.userData.user.uid.get()} | username = ${AppState.userData.user.username.get()}")

        when {
        // Not signed in
            FirebaseAuth.getInstance().currentUser == null -> {
                stopLoadingViews()
                if (waitingOnGoogleSignIn) {
                    // todo show loading view
                } else {
                    showSignInOptionsLayout(true)
                }
            }
        // Log in with UID
            AppState.userData.user.uid.get() == "" -> {
                FirestoreActions.getUserFromUID()
            }
        // Create username
            AppState.userData.user.username.get() == "" -> {
                stopLoadingViews()
                showUsernameLayout(true)
            }
        }
    }

    private fun nextActivity() {
        when {
            // Show 'setup first hub' or 'hub invitations' activity
            AppState.userData.hubs.size == 0 -> {
                //todo check for hub invitations
                startActivity(Intent(this, MainViewActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
            // Show 'main view'
            else -> {
                startActivity(Intent(this, MainViewActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
        }
    }

/*
    ------------------------------------------------
        Overridden Functions
    -----------------------------------------------
*/

    override fun onBackPressed() {
        when (layoutState) {
            SHOW_SIGN_IN, SHOW_SIGN_UP, SHOW_USERNAME -> {
                stopLoadingViews()
                showSignInOptionsLayout(true)
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean("WAIT_ON_GOOGLE", waitingOnGoogleSignIn)
        outState.putInt("LAYOUT_STATE", layoutState)
    }

    @SuppressLint("LogNotTimber")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            AuthActions.RC_SIGN_IN -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        AuthActions.handleGoogleSignInResult2(data)
                    }
                }
            }
            else -> Log.w("SplashActivity", "Unknown request requestCode ($requestCode)")
        }
    }

    override fun onPause() {
        super.onPause()
        AppState.userData.hubs.removeOnMapChangedCallback(hubsListener)
        AppState.userData.remotes.removeOnMapChangedCallback(remotesListener)
        AppState.userData.user.username.removeOnPropertyChangedCallback(usernameListener)
        AppState.userData.user.uid.removeOnPropertyChangedCallback(uidListener)
        AppState.errorData.userSignInError.addOnPropertyChangedCallback(errorListener)
    }

    override fun onResume() {
        super.onResume()
        AppState.userData.user.username.addOnPropertyChangedCallback(usernameListener)
        AppState.userData.hubs.addOnMapChangedCallback(hubsListener)
        AppState.userData.remotes.addOnMapChangedCallback(remotesListener)
        AppState.userData.user.uid.addOnPropertyChangedCallback(uidListener)
        AppState.errorData.userSignInError.addOnPropertyChangedCallback(errorListener)
        checkLoginState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // New firebase timestamp bs
        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setTimestampsInSnapshotsEnabled(true)
            .build()

        // Restore state
        waitingOnGoogleSignIn = savedInstanceState?.getBoolean("WAIT_ON_GOOGLE") ?: waitingOnGoogleSignIn
        layoutState = savedInstanceState?.getInt("LAYOUT_STATE") ?: layoutState

        // Bind layout
        binding = DataBindingUtil.setContentView(this, R.layout.a_splash_login_main)
        binding.layoutSignIn.btnSignIn.setOnClickListener { signInWithEmail() }
        binding.layoutSignUp.btnSignUp.setOnClickListener { signUpWithEmail() }
        binding.layoutUsername.btnSelectUsername.setOnClickListener { createUser() }
        binding.btnSignIn.setOnClickListener { showSignInLayout(true) }
        binding.btnSignUpEmail.setOnClickListener { showSignUpLayout(true) }
        binding.signInGoogle.setOnClickListener { AuthActions.signInWithGoogle(this) }
    }

/*
    ------------------------------------------------
        OnClick Functions
    -----------------------------------------------
*/
    private fun createUser() {
        // clear textInput errors
        binding.layoutUsername.selectUsername.error = ""

        // get username input
        val username = binding.layoutUsername.selectUsername.editText?.text.toString()

        // check validity
        val isValidUsername = MyValidators.UsernameValidator(username)
            .addErrorCallback { binding.layoutUsername.selectUsername.error = getString(R.string.err_invalid_username) }
            .check()
        if (isValidUsername) {
            // start loading animation
            binding.layoutUsername.btnSelectUsername.startAnimation()

            // add user to firebase
            FirestoreActions.addUser(username)
        }
    }

    private fun signUpWithEmail() {
        // clear textInput errors
        binding.layoutSignUp.passwordConfirm.error = ""
        binding.layoutSignUp.password.error = ""

        // get input
        val email = binding.layoutSignUp.email.editText?.text.toString()
        val password = binding.layoutSignUp.password.editText?.text.toString()
        val passwordConfirm = binding.layoutSignUp.passwordConfirm.editText?.text.toString()

        // check validity
        val isValidEmailAndPassword =
            MyValidators.PasswordValidator(password)
                .addErrorCallback { binding.layoutSignUp.password.error = getString(R.string.err_pass) }
                .check()
                    &&
                    MyValidators.EmailValidator(email)
                        .addErrorCallback { binding.layoutSignUp.email.error = getString(R.string.err_invalid_email) }
                        .check()
        val passwordsMatch = when (password) {
            passwordConfirm -> true
            else -> {
                binding.layoutSignUp.password.error = getString(R.string.err_pass_match)
                binding.layoutSignUp.passwordConfirm.error = getString(R.string.err_pass_match)
                false
            }
        }
        if (isValidEmailAndPassword && passwordsMatch) {
            // start loading animation
            binding.layoutSignUp.btnSignUp.startAnimation()
            AuthActions.createAccount(email, password)
//                .addOnSuccessListener {
//                    binding.layoutSignUp.btnSignUp.revertAnimation()
//                    showUsernameLayout(true)
//                }
//                .addOnFailureListener { e ->
//                    binding.layoutSignUp.btnSignUp.revertAnimation()
//                    onSignInError(e)
//                }
        }
    }

    private fun signInWithEmail() {
        val email = binding.layoutSignIn.email.editText?.text.toString()
        val password = binding.layoutSignIn.password.editText?.text.toString()

        val isValidEmailAndPassword = MyValidators.PasswordValidator(password)
            .addErrorCallback { binding.layoutSignIn.password.error = getString(R.string.err_pass) }
            .check()
                &&
                MyValidators.EmailValidator(email)
                    .addErrorCallback { binding.layoutSignIn.email.error = getString(R.string.err_invalid_email) }
                    .check()

        if (isValidEmailAndPassword) {
            binding.layoutSignIn.btnSignIn.startAnimation()
            AuthActions.signInWithEmail(email, password)
        }
    }

/*
    ------------------------------------------------
        Layout Functions
    -----------------------------------------------
*/
    private fun moveLogoUp(animate: Boolean) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(this, R.layout.a_splash_login_show)
        constraintSet.constrainHeight(binding.splashLogo.id, 375)
        if (animate) {
            Handler().postDelayed({
                androidx.transition.TransitionManager.beginDelayedTransition(binding.splashContainer, layoutTransition)
                constraintSet.applyTo(binding.splashContainer)
                ObjectAnimator.ofFloat(binding.cardContainer, "alpha", 1f).apply {
                    duration = TRANSITION_DURATION.toLong() + 250
                    interpolator = AccelerateInterpolator()
                }.start()
            }, 500)
        } else {
            constraintSet.applyTo(binding.splashContainer)
        }

    }

    private fun showUsernameLayout(animate: Boolean) {
        moveLogoUp(true)

        if (animate) {
            val anim = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(p0: Animation?) {}

                override fun onAnimationEnd(p0: Animation?) {
                    hideButtons()
                    binding.layoutSignIn.signInContainer.visibility = View.GONE
                    binding.layoutSignUp.signUpContainer.visibility = View.GONE
                    binding.layoutUsername.usernameContainer.visibility = View.VISIBLE

                    val upAnim = AnimationUtils.loadAnimation(this@SplashActivity4,
                        R.anim.slide_up
                    )
                    binding.signInContainer.startAnimation(upAnim)
                }

                override fun onAnimationStart(p0: Animation?) {}

            })
            binding.signInContainer.startAnimation(anim)
        } else {
            hideButtons()
            binding.layoutSignIn.signInContainer.visibility = View.GONE
            binding.layoutSignUp.signUpContainer.visibility = View.GONE
            binding.layoutUsername.usernameContainer.visibility = View.VISIBLE
        }
    }

    private fun showSignInOptionsLayout(animate: Boolean) {
        moveLogoUp(animate)

        if (animate) {
            val anim = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(p0: Animation?) {}

                override fun onAnimationEnd(p0: Animation?) {
                    showButtons()
                    binding.layoutSignIn.signInContainer.visibility = View.GONE
                    binding.layoutSignUp.signUpContainer.visibility = View.GONE
                    binding.layoutUsername.usernameContainer.visibility = View.GONE

                    val upAnim = AnimationUtils.loadAnimation(this@SplashActivity4,
                        R.anim.slide_up
                    )
                    binding.signInContainer.startAnimation(upAnim)
                }

                override fun onAnimationStart(p0: Animation?) {}

            })
            binding.signInContainer.startAnimation(anim)
        } else {
            showButtons()
            binding.layoutSignIn.signInContainer.visibility = View.GONE
            binding.layoutSignUp.signUpContainer.visibility = View.GONE
            binding.layoutUsername.usernameContainer.visibility = View.GONE
        }
    }

    private fun showSignUpLayout(animate : Boolean) {
        moveLogoUp(true)

        if (animate) {
            val anim = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(p0: Animation?) {}

                override fun onAnimationEnd(p0: Animation?) {
                    hideButtons()
                    binding.layoutSignIn.signInContainer.visibility = View.GONE
                    binding.layoutSignUp.signUpContainer.visibility = View.VISIBLE
                    binding.layoutUsername.usernameContainer.visibility = View.GONE

                    val upAnim = AnimationUtils.loadAnimation(this@SplashActivity4,
                        R.anim.slide_up
                    )
                    binding.signInContainer.startAnimation(upAnim)
                }

                override fun onAnimationStart(p0: Animation?) {}

            })
            binding.signInContainer.startAnimation(anim)
        } else {
            hideButtons()
            binding.layoutSignIn.signInContainer.visibility = View.GONE
            binding.layoutSignUp.signUpContainer.visibility = View.VISIBLE
            binding.layoutUsername.usernameContainer.visibility = View.GONE
        }
    }

    private fun showSignInLayout(animate: Boolean) {
        moveLogoUp(animate)

        if (animate) {
            val anim = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(p0: Animation?) {}

                override fun onAnimationEnd(p0: Animation?) {
                    hideButtons()
                    binding.layoutSignIn.signInContainer.visibility = View.VISIBLE
                    binding.layoutSignUp.signUpContainer.visibility = View.GONE
                    binding.layoutUsername.usernameContainer.visibility = View.GONE

                    val upAnim = AnimationUtils.loadAnimation(this@SplashActivity4,
                        R.anim.slide_up
                    )
                    binding.signInContainer.startAnimation(upAnim)
                }

                override fun onAnimationStart(p0: Animation?) {}

            })
            binding.signInContainer.startAnimation(anim)
        } else {
            hideButtons()
            binding.layoutSignIn.signInContainer.visibility = View.VISIBLE
            binding.layoutSignUp.signUpContainer.visibility = View.GONE
            binding.layoutUsername.usernameContainer.visibility = View.GONE
        }
    }

    private fun showButtons() {
        binding.btnSignIn.visibility = View.VISIBLE
        binding.btnSignUpEmail.visibility = View.VISIBLE
        binding.signInGoogle.visibility = View.VISIBLE
        binding.welcomeDescription.visibility = View.VISIBLE
    }

    private fun hideButtons() {
        binding.btnSignIn.visibility = View.GONE
        binding.btnSignUpEmail.visibility = View.GONE
        binding.signInGoogle.visibility = View.GONE
        binding.welcomeDescription.visibility = View.GONE
    }

    private fun stopLoadingViews() {
        binding.layoutUsername.btnSelectUsername.revertAnimation()
        binding.layoutSignUp.btnSignUp.revertAnimation()
        binding.layoutSignIn.btnSignIn.revertAnimation()
    }

/*
    ------------------------------------------------
        Error-Handling Functions
    -----------------------------------------------
*/

    /**
     * Handles what message to show a user based on sign-in error.
     */
    private fun onSignInError(exception: Exception) {
        when (exception) {
            is FirebaseAuthUserCollisionException -> showError(
                R.string.err_account_made_title,
                R.string.err_acount_made_desc
            )
            is FirebaseAuthInvalidCredentialsException -> showError(
                R.string.err_inv_email_pass,
                R.string.err_inv_email_pass_desc
            )
            is FirebaseAuthInvalidUserException -> showError(
                R.string.err_email_nonexistant,
                R.string.err_email_nonexistant_desc
            )
            is FirebaseFirestoreException -> {
                when (exception.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> {showError(
                        R.string.username_taken_title,
                        R.string.err_username_taken_desc
                    )}
                    else -> { showUnknownError(exception) }
                }
            }
            else -> { showUnknownError(exception) }
        }
    }


    @SuppressLint("LogNotTimber")
    private fun showUnknownError(exception: Exception) {
        val errorMessage = "Unexpected error during SplashActivity: ($exception)"
        Log.e("SplashActivity", errorMessage)
        showErrorWithAction(
            R.string.error,
            R.string.err_sign_in_desc,
            R.string.report_issue,
            {FirestoreActions.reportError(errorMessage)},
            {})
    }

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
            .positiveActionTapListener(object : Flashbar.OnActionTapListener {
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
}
