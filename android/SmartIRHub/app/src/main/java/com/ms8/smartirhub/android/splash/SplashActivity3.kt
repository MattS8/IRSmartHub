package com.ms8.smartirhub.android.splash

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableArrayList
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
import com.google.firebase.firestore.QuerySnapshot
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.data.Group
import com.ms8.smartirhub.android.data.User
import com.ms8.smartirhub.android.database.LocalData
import com.ms8.smartirhub.android.databinding.ASplashLoginMainBinding
import com.ms8.smartirhub.android.firebase.FirebaseAuthActions
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.main_view.MainViewActivity
import com.ms8.smartirhub.android.utils.MyValidators
import java.io.Serializable

class SplashActivity3 : AppCompatActivity() {
    lateinit var binding : ASplashLoginMainBinding
    lateinit var state: InstanceState

    private val userGroupsListener = UserGroupsListener(null)

    /**
     * Transition animation properties
     */
    private var layoutTransition = androidx.transition.AutoTransition().apply {
        interpolator = DecelerateInterpolator()
        duration = TRANSITION_DURATION.toLong()
    }

/*
    ----------------------------------------------
        Overridden Functions
    ----------------------------------------------
*/

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putSerializable(STATE, state)
    }

    override fun onBackPressed() {
        when (state.layoutState) {
            SHOW_SIGN_IN, SHOW_SIGN_UP, SHOW_USERNAME -> showSignInOptionsLayout(true)
            else -> super.onBackPressed()
        }
    }

    @SuppressLint("LogNotTimber")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FirebaseAuthActions.RC_SIGN_IN -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        handleGoogleSignInResult(data)
                    }
                }
            }
            else -> Log.w(TAG, "Unknown request requestCode ($requestCode)")
        }
    }

    override fun onPause() {
        super.onPause()
        LocalData.userGroups.removeOnMapChangedCallback(userGroupsListener)
        userGroupsListener.context = null
    }

    override fun onResume() {
        super.onResume()
        LocalData.userGroups.addOnMapChangedCallback(userGroupsListener.apply { context = this@SplashActivity3 })
        val groupSize = LocalData.user?.groups?.size ?: -1
        if (LocalData.user != null && LocalData.user!!.groups.size == LocalData.userGroups.size) {
            Log.d("Test##", "All group data got! ($groupSize)")
            startActivity(Intent(this, MainViewActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        } else {
            Log.d("Test##", "Still waiting on something... user: ${LocalData.user} | groupSize = $groupSize | LocalData.userGroups.size = ${LocalData.userGroups.size}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // New firebase timestamp bs
        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setTimestampsInSnapshotsEnabled(true)
            .build()

        // Retrieve saved state
        state = savedInstanceState?.getSerializable(STATE) as InstanceState? ?: InstanceState()

        // Bind layout - Show sign in options immediately unless this is first time loading activity
        binding = DataBindingUtil.setContentView(this, R.layout.a_splash_login_main)

        binding.layoutSignIn.btnSignIn.setOnClickListener { signInWithEmail() }
        binding.layoutSignUp.btnSignUp.setOnClickListener { signUpWithEmail() }
        binding.layoutUsername.btnSelectUsername.setOnClickListener { createUser() }
        binding.btnSignIn.setOnClickListener { showSignInLayout(true) }
        binding.btnSignUpEmail.setOnClickListener { showSignUpLayout(true) }
        binding.signInGoogle.setOnClickListener { FirebaseAuthActions.signInWithGoogle(this) }

        when (state.layoutState) {
            SHOW_SPLASH -> { checkLoginStatus() }
            SHOW_OPTIONS -> showSignInOptionsLayout(false)
            SHOW_SIGN_IN -> showSignInLayout(false)
            SHOW_SIGN_UP -> showSignUpLayout(false)
            SHOW_USERNAME -> showUsernameLayout(false)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun checkLoginStatus() {
        state.layoutState = SHOW_OPTIONS

        if (FirebaseAuth.getInstance().currentUser == null) {
            moveLogoUp(true)
            return
        }

        FirestoreActions.getUserFromUID()
            .addOnSuccessListener {querySnapshot ->
                onUserFromUidSuccess(querySnapshot)
            }
            .addOnFailureListener {
                moveLogoUp(true)
            }

    }

/*
    ------------------------------------------------
        OnClick Functions
    -----------------------------------------------
*/

    private fun createUser() {
        val username : String = binding.layoutUsername.selectUsername.editText?.text.toString()
        if (isValidUsername(username)) {
            binding.layoutUsername.btnSelectUsername.startAnimation()
            FirestoreActions.addUser(username)
                .addOnSuccessListener {
                    FirestoreActions.listenToUserData(username)
                }
                .addOnFailureListener { e ->
                    onSignInError(e)
                    binding.layoutUsername.btnSelectUsername.revertAnimation()
                }
        }
    }

    private fun signUpWithEmail() {
        val email = binding.layoutSignUp.email.editText?.text.toString()
        val password = binding.layoutSignUp.password.editText?.text.toString()
        val passwordConfirm = binding.layoutSignUp.passwordConfirm.editText?.text.toString()

        val isValidEmailAndPassword =
            MyValidators.PasswordValidator(password)
                .addErrorCallback { binding.layoutSignUp.password.error = getString(R.string.err_pass) }
                .check()
                    &&
            MyValidators.EmailValidator(email)
                .addErrorCallback { binding.layoutSignUp.email.error = getString(R.string.err_invalid_email) }
                .check()

        if (isValidEmailAndPassword && passwordsMatch(password, passwordConfirm)) {
            binding.layoutSignUp.btnSignUp.startAnimation()
            FirebaseAuthActions.createAccount(email, password)
                .addOnSuccessListener {
                    binding.layoutSignUp.btnSignUp.revertAnimation()
                    showUsernameLayout(true)
                }
                .addOnFailureListener { e ->
                    binding.layoutSignUp.btnSignUp.revertAnimation()
                    onSignInError(e)
                }
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
            FirebaseAuthActions.signInWithEmail(email, password)
                .addOnSuccessListener {
                    onSignInSuccess()
                }
                .addOnFailureListener { e ->
                    onSignInError(e)
                    binding.layoutSignIn.btnSignIn.revertAnimation()
                }
        }
    }

    /**
     * Handles whether to show an error or continue with sign in process based on Google sign in result
     */
    private fun handleGoogleSignInResult(data: Intent?) {
        when (val task = FirebaseAuthActions.handleGoogleSignInResult2(data)) {
            // Google Sign In failed
            null -> showErrorWithAction(
                R.string.error,
                R.string.err_sign_in_desc,
                R.string.report_issue,
                { FirestoreActions.reportError("Error signing in with Google")}, {})
            // Check task result
            else -> {
                task.addOnSuccessListener { onSignInSuccess() }
                    .addOnFailureListener {e -> onSignInError(e) }
            }
        }
    }

    /**
     * When a listener failure occurs, an error is shown. Otherwise, the user data is stored
     * and the sign in process continues by fetching all group info
     */
    @SuppressLint("LogNotTimber")
    private fun onSignInSuccess() {
        FirestoreActions.getUserFromUID()
            .addOnSuccessListener { querySnapshot ->
                onUserFromUidSuccess(querySnapshot)
            }
            .addOnFailureListener { e ->
                binding.layoutSignIn.btnSignIn.revertAnimation()
                binding.layoutSignUp.btnSignUp.revertAnimation()
                binding.layoutUsername.btnSelectUsername.revertAnimation()
                Log.e(TAG, "Username query failed for user with uid:" +
                        " ${FirebaseAuth.getInstance().currentUser?.uid} ($e)")
                onSignInError(e)
            }
    }

    @Suppress("UNCHECKED_CAST")
    @SuppressLint("LogNotTimber")
    private fun onUserFromUidSuccess(querySnapshot: QuerySnapshot) {
        when {
            querySnapshot.isEmpty -> {
                binding.layoutSignIn.btnSignIn.revertAnimation()
                binding.layoutSignUp.btnSignUp.revertAnimation()
                showUsernameLayout(true)
            }
            else -> {
                if (querySnapshot.size() > 1)
                    Log.e(TAG, "Received more than one user object from uid:" +
                                " ${FirebaseAuth.getInstance().currentUser?.uid}")
                val doc = querySnapshot.documents[0]
                LocalData.user = User(FirebaseAuth.getInstance().currentUser!!.uid, doc.id)
                try {
                    LocalData.user?.groups = ObservableArrayList<String>().apply { addAll(doc["groups"] as ArrayList<String>) }
                } catch (e : java.lang.Exception) {
                    Log.e("onUserFromUidSuccess", "$e")
                    showUnknownError(e)
                    LocalData.user = null
                }
                LocalData.user?.username?.let { FirestoreActions.listenToUserData(it) }
            }
        }
    }

/*
    -------------------------------------------
        Layout Transition Functions
    ------------------------------------------
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
        state.layoutState = SHOW_USERNAME

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

                    val upAnim = AnimationUtils.loadAnimation(this@SplashActivity3,
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
        state.layoutState = SHOW_OPTIONS

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

                    val upAnim = AnimationUtils.loadAnimation(this@SplashActivity3,
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
        state.layoutState = SHOW_SIGN_UP

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

                    val upAnim = AnimationUtils.loadAnimation(this@SplashActivity3,
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
        state.layoutState = SHOW_SIGN_IN

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

                    val upAnim = AnimationUtils.loadAnimation(this@SplashActivity3,
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

//    private fun playSplashTransition() {
//        state.layoutState = SHOW_OPTIONS
//
//        val anim = AnimationUtils.loadAnimation(this, R.anim.slide_up)
//        binding.signInContainer.startAnimation(anim)
//        binding.splashLogo.startAnimation(anim)
//        binding.welcomeTitle.startAnimation(anim)
//    }

/*
    -----------------------------------------------
        Validator Functions
    ----------------------------------------------
*/

    /**
     * Handles showing error messages if passwords don't match
     */
    private fun passwordsMatch(password: String, passwordConfirm: String): Boolean {
        binding.layoutSignUp.passwordConfirm.error = ""
        binding.layoutSignUp.password.error = ""

        return when (password) {
            passwordConfirm -> true
            else -> {
                binding.layoutSignUp.password.error = getString(R.string.err_pass_match)
                binding.layoutSignUp.passwordConfirm.error = getString(R.string.err_pass_match)
                false
            }
        }
    }

    /**
     * Handles showing error message if username is invalid
     */
    private fun isValidUsername(username: String) : Boolean {
        binding.layoutUsername.selectUsername.error = ""

        return MyValidators.UsernameValidator(username)
            .addErrorCallback { binding.layoutUsername.selectUsername.error = getString(R.string.err_invalid_username) }
            .check()
    }

/* ---------------------------------------------- Show Error Functions ---------------------------------------------- */

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
        Log.e(TAG, errorMessage)
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

    companion object {
        const val SHOW_SPLASH = 0
        const val SHOW_OPTIONS = 1
        const val SHOW_SIGN_IN = 2
        const val SHOW_SIGN_UP = 3
        const val SHOW_USERNAME = 4

        const val STATE = "SPLASH_STATE"
        const val TAG = "SplashActivity"

        const val TRANSITION_DURATION = 800
    }

    class InstanceState : Serializable {
//        var emailString = ""
//        var passString = ""
//        var passConfirmString = ""
//        var usernameString = ""

        var layoutState = SHOW_SPLASH
    }

    class UserGroupsListener(var context: Activity?) : ObservableMap.OnMapChangedCallback<ObservableArrayMap<String, Group>, String, Group>() {
        override fun onMapChanged(sender: ObservableArrayMap<String, Group>?, key: String?) {
            Log.d("UserGroupListener", "Map Changed!")
            val groupSize = LocalData.user?.groups?.size ?: -1
            if (LocalData.user != null && groupSize == LocalData.userGroups.size) {
                Log.d("UserGroupListener", "Done fetching user groups... (${LocalData.user!!.groups.size} == ${LocalData.userGroups.size}")
                context?.startActivity(Intent(context, MainViewActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                context?.finish()
            }
        }
    }
}
