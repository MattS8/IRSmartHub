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
import android.view.animation.AccelerateInterpolator
import com.google.firebase.auth.FirebaseAuth
import com.ms8.smartirhub.android.databinding.ActivitySplashBinding
import com.ms8.smartirhub.android.firebase.FirebaseAuthActions
import com.andrognito.flashbar.Flashbar


class SplashActivity : AppCompatActivity() {
    lateinit var binding : ActivitySplashBinding

    // The current state of the splash screen
    var state = State.SHOW_SPLASH

    // Transition used to show sign in views
    var signInTransition = AutoTransition().apply {
        interpolator = AccelerateInterpolator()
        duration = 5000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)

        // Bind functions
        binding.signInGoogle.setOnClickListener { FirebaseAuthActions.signInWithGoogle(this, getString(R.string.client_id)) }

        // Restore state
        savedInstanceState?.let { state = getState(it.getString(SPLASH_INSTANCE_STATE, SHOW_SPLASH)) }

        // Set proper constraints
        when (state) {
            State.SHOW_SPLASH -> ConstraintSet()
                .apply { clone(this@SplashActivity, R.layout.activity_splash) }.applyTo(binding.splashContainer)
            else -> ConstraintSet()
                .apply { clone(this@SplashActivity, R.layout.activity_splash_login) }.applyTo(binding.splashContainer)
        }

        // Listen for FirebaseAuth initialization
        FirebaseAuth.getInstance().addAuthStateListener {
            // Show sign in once FirebaseAuth has initialized
            when {
                it.currentUser == null && state == State.SHOW_SPLASH -> showSignIn()
                it.currentUser != null -> { startActivity(Intent(this, MainActivity::class.java))} //TODO start mainActivity
            }
        }
    }

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
                            null -> {}
                            // Google Sign In failed
                            else -> Flashbar.Builder(this)
                                .title(R.string.err_sign_in_title)
                                .message(R.string.err_sign_in_google_desc)
                                .primaryActionText(android.R.string.ok)
                                .primaryActionTapListener(object : Flashbar.OnActionTapListener {
                                    override fun onActionTapped(bar: Flashbar) { bar.dismiss() }
                                })
                                .build()
                                .show()
                        }
                    }
                    else -> Log.w("TEST####", "Result no ok... $resultCode")
                }
            }
            else -> Log.w(TAG, "Unknown request code ($requestCode)")
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(SPLASH_INSTANCE_STATE, state.getString())
    }

    /**
     * Starts transition animation from splash screen to sign in layout
     */
    private fun showSignIn() {
        Log.d("TEST###", "Starting Sign In Transition...")
        // Change activity state to SHOW_SIGN_IN
        state = State.SHOW_SIGN_IN

        // Get Sign In layout
        val constraintSet2 = ConstraintSet().apply { clone(this@SplashActivity, R.layout.activity_splash_login) }

        // Apply transition
        TransitionManager.beginDelayedTransition(binding.splashContainer, signInTransition)
        Handler().postDelayed({ constraintSet2.applyTo(binding.splashContainer) }, 100)
    }


    companion object {

        const val SHOW_SPLASH = "STATE_SHOW_SPLASH"
        const val SHOW_SIGN_IN = "STATE_SHOW_SIGN_IN"
        const val DONE = "STATE_SPLASH_DONE"
        const val SPLASH_INSTANCE_STATE = "SPLASH_STATE"

        const val TAG = "SplashActivity"

        private fun getState(string: String): State {
            return when (string) {
                SHOW_SPLASH -> State.SHOW_SPLASH
                SHOW_SIGN_IN -> State.SHOW_SIGN_IN
                DONE -> State.DONE
                else -> State.SHOW_SPLASH
            }
        }


    }

    enum class State {SHOW_SPLASH, SHOW_SIGN_IN, DONE}

    private fun State.getString() : String {
        return when (this) {
            State.SHOW_SPLASH -> SHOW_SPLASH
            State.SHOW_SIGN_IN -> SHOW_SIGN_IN
            State.DONE -> DONE
        }
    }
}
