package com.ms8.smartirhub.android

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.os.Handler
import android.support.constraint.ConstraintSet
import android.support.v7.app.AppCompatActivity
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import com.google.firebase.auth.FirebaseAuth
import com.ms8.smartirhub.android.databinding.ASplashBinding

class SplashActivity2 : AppCompatActivity() {
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
        when {
        // First time using app
            auth.currentUser == null -> {
                Log.d("T#AuthState", "Not signed in")
                FirebaseAuth.getInstance().signInAnonymously()
            }
        // Hasn't signed in yet
            auth.currentUser!!.isAnonymous -> {
                Log.d("T#AuthState", "Signed in anonymously")
                showSignInOptions(true)
            }
        // Start fetching new user data
            else -> {
                Log.d("t#AuthState", "now signed in under uid: ${auth.currentUser!!.uid} " +
                        "and email ${auth.currentUser!!.email}")
                //todo fetch user data
            }
        }
    }

/* ------------------------------------------------ OnClick Functions ----------------------------------------------- */

    private fun btnSignInClicked() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun btnSignUpClicked() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun btnGoogleSignInClicked() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

/* ------------------------------------------- Layout Transition Functions ------------------------------------------ */

    private fun showSignInOptions(bAnimate: Boolean) {
        binding.welcomeTitle.text = getString(R.string.welcomeTitle)
        binding.welcomeDescription.text = getString(R.string.welcome_desc)

        // Enable Buttons
        binding.btnSignIn.isEnabled = true
        binding.signInGoogle.isEnabled = true
        binding.password.isEnabled = true

        val newLayout = ConstraintSet().apply {
            clone(this@SplashActivity2, R.layout.a_splash_show_options)
            constrainHeight(binding.splashLogo.id, 375)
            constrainHeight(binding.password.id, 0)
            constrainHeight(binding.passwordConfirm.id, 0)
            constrainHeight(binding.email.id, 0)
        }

        val alphaAnimation = AlphaAnimation(0f, 1f).apply {
            duration = if (bAnimate) TRANSITION_DURATION.toLong() else 1
        }

        if (bAnimate) {
            TransitionManager.beginDelayedTransition(binding.splashContainer, layoutTransition)
        }

        newLayout.applyTo(binding.splashContainer)
        binding.or.startAnimation(alphaAnimation)
        binding.signInGoogle.startAnimation(alphaAnimation)
        binding.btnSignIn.startAnimation(alphaAnimation)
        binding.btnSignUp.startAnimation(alphaAnimation)
    }

    private fun showSignIn(bAnimate: Boolean) {
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
        binding.signInGoogle.isEnabled = true
        binding.password.isEnabled = true

        // Get Layout transition
        val constraintSet = ConstraintSet().apply {
            clone(this@SplashActivity2, R.layout.activity_splash_login)
            constrainHeight(binding.splashLogo.id, 375)
        }

        if (bAnimate) {
            TransitionManager.beginDelayedTransition(binding.splashContainer, layoutTransition)
        }
        constraintSet.applyTo(binding.splashContainer)
    }

    private fun showSplash(bAnimate: Boolean) {
        // Disable Inputs
        binding.btnSignIn.isEnabled = false
        binding.signInGoogle.isEnabled = false
        binding.btnSignUp.isEnabled = false
        binding.password.isEnabled = false
        binding.email.isEnabled = false
        binding.password.isEnabled = false
        binding.passwordConfirm.isEnabled = false

        val newLayout = ConstraintSet().apply {
            clone(this@SplashActivity2, R.layout.a_splash)
        }

        val alphaAnimation = AlphaAnimation(1f, 0f).apply {
            duration = if (bAnimate) TRANSITION_DURATION.toLong() else 1
        }

        if (bAnimate) { TransitionManager.beginDelayedTransition(binding.splashContainer, layoutTransition) }

        newLayout.applyTo(binding.splashContainer)
        binding.or.startAnimation(alphaAnimation)
        binding.welcomeTitle.startAnimation(alphaAnimation)
        binding.welcomeDescription.startAnimation(alphaAnimation)

        binding.btnSignUp.startAnimation(alphaAnimation)
        binding.btnSignIn.startAnimation(alphaAnimation)
        binding.signInGoogle.startAnimation(alphaAnimation)
        binding.password.startAnimation(alphaAnimation)
        binding.email.startAnimation(alphaAnimation)
        binding.password.startAnimation(alphaAnimation)
        binding.passwordConfirm.startAnimation(alphaAnimation)
    }

    private fun showCreateUsername(bAnimate: Boolean) {

    }

/* ---------------------------------------------- Overridden Functions ---------------------------------------------- */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bind layout
        binding = DataBindingUtil.setContentView(this, R.layout.a_splash)

        // Restore state
        savedInstanceState?.let {
            splashState.layoutState = layoutStateFromString(
                it.getString(
                    SplashActivity.SPLASH_INSTANCE_STATE + "_LAYOUT",
                    SplashActivity.SHOW_SPLASH
                )
            )
            splashState.emailStr = it.getString(SplashActivity.SPLASH_INSTANCE_STATE + "_INPUT_EMAIL", "")
            splashState.passStr = it.getString(SplashActivity.SPLASH_INSTANCE_STATE + "_INPUT_PASS", "")
            splashState.usernameStr = it.getString(SplashActivity.SPLASH_INSTANCE_STATE + "_INPUT_USERNAME", "")
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
            LayoutState.SHOW_SIGN_IN_OPTIONS -> showSignInOptions(false)
            else -> showSplash(false)
        }
    }

    override fun onResume() {
        super.onResume()
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.let {
            it.putString(SplashActivity.SPLASH_INSTANCE_STATE + "_LAYOUT", splashState.layoutState.getString())
            it.putString(SplashActivity.SPLASH_INSTANCE_STATE + "_INPUT_EMAIL", splashState.emailStr)
            it.putString(SplashActivity.SPLASH_INSTANCE_STATE + "_INPUT_PASS", splashState.passStr)
            it.putString(SplashActivity.SPLASH_INSTANCE_STATE + "_INPUT_USERNAME", splashState.usernameStr)
        }
    }

    override fun onPause() {
        super.onPause()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
    }

    override fun onBackPressed() {
        when (splashState.layoutState) {
            LayoutState.SHOW_USERNAME -> { showSignIn(true) }
            else -> super.onBackPressed()
        }
    }

    /* ------------------------------------------------ Helper Functions ------------------------------------------------ */

    private fun gotoMainPage() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


/* -------------------------------------------------- Static Stuff -------------------------------------------------- */

    companion object {

        const val SHOW_SPLASH = "STATE_SHOW_SPLASH"
        const val SHOW_SIGN_IN = "STATE_SHOW_SIGN_IN"
        const val SHOW_USER_NAME = "STATE_SHOW_USERNAME"
        const val SHOW_SIGN_IN_OPTIONS = "STATE_SHOW_SIGN_IN_OPTIONS"
        const val DONE = "STATE_SPLASH_DONE"
        const val SPLASH_INSTANCE_STATE = "SPLASH_STATE"

        const val TAG = "SplashActivity"

        const val TRANSITION_DURATION = 700

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

    enum class LayoutState {SHOW_SPLASH, SHOW_SIGN_IN, SHOW_USERNAME, DONE, SHOW_SIGN_IN_OPTIONS}

    private fun LayoutState.getString() : String {
        return when (this) {
            LayoutState.SHOW_SPLASH -> SHOW_SPLASH
            LayoutState.SHOW_SIGN_IN -> SHOW_SIGN_IN
            LayoutState.SHOW_USERNAME -> SHOW_USER_NAME
            LayoutState.DONE -> DONE
            LayoutState.SHOW_SIGN_IN_OPTIONS -> SHOW_SIGN_IN_OPTIONS
        }
    }

}
