package com.ms8.irsmarthub.splash_screen

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.databinding.ObservableMap
import com.andrognito.flashbar.Flashbar
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.ms8.irsmarthub.R
import com.ms8.irsmarthub.database.AppState.errorData
import com.ms8.irsmarthub.database.AppState.tempData
import com.ms8.irsmarthub.database.AppState.userData
import com.ms8.irsmarthub.database.AuthFunctions
import com.ms8.irsmarthub.database.FirestoreFunctions
import com.ms8.irsmarthub.databinding.ASplashMainBinding
import com.ms8.irsmarthub.hub.models.Hub
import com.ms8.irsmarthub.main_menu.MainActivity
import com.ms8.irsmarthub.remote_control.remote.models.Remote
import com.ms8.irsmarthub.utils.DynamicStrings.getPasswordErrorString
import com.ms8.irsmarthub.utils.DynamicStrings.getUsernameErrorString
import com.ms8.irsmarthub.utils.MyValidators
import com.ms8.irsmarthub.utils.RequestCodes.REQ_GOOGLE_SIGN_IN

class SplashActivity : AppCompatActivity() {

    private var state = State()

    private lateinit var binding: ASplashMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.a_splash_main)
        (savedInstanceState?.get(STATE) as State?)?.let { state = it }

        binding.apply {
            layoutSignIn.btnSignIn.setOnClickListener { signInWithEmail() }
            layoutSignUp.btnSignUp.setOnClickListener { signUpWithEmail() }
            layoutUsername.btnSelectUsername.setOnClickListener { createUsername() }
            btnSignIn.setOnClickListener { showSignInLayout() }
            btnSignUpEmail.setOnClickListener { showSignUpLayout() }
            signInGoogle.setOnClickListener { signInWithGoogle() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE, state)
    }

    override fun onResume() {
        super.onResume()
        // Set up listeners
        userData.hubs.addOnMapChangedCallback(hubDataListener)
        userData.remotes.addOnMapChangedCallback(remoteDataListener)
        tempData.tempUser.uid.addOnPropertyChangedCallback(signInListener)
        tempData.tempUser.username.addOnPropertyChangedCallback(usernameListener)
        errorData.fetchUserDataError.addOnPropertyChangedCallback(fetchUserDataErrorListener)
        errorData.signInError.addOnPropertyChangedCallback(signInErrorListener)

        when {
            tempData.tempUser.hasFetchedInitialUserData() -> nextActivity()
            errorData.fetchUserDataError.get() != null ->
                fetchUserDataErrorListener.onFetchUserDataError()
            errorData.signInError.get() != null ->
                signInErrorListener.onSignInError()
            else -> {
                FirestoreFunctions.fetchAllData()
                applyLayout()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        userData.hubs.removeOnMapChangedCallback(hubDataListener)
        userData.remotes.removeOnMapChangedCallback(remoteDataListener)
        tempData.tempUser.uid.removeOnPropertyChangedCallback(signInListener)
        tempData.tempUser.username.removeOnPropertyChangedCallback(usernameListener)
        errorData.fetchUserDataError.removeOnPropertyChangedCallback(fetchUserDataErrorListener)
        errorData.signInError.removeOnPropertyChangedCallback(signInErrorListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_GOOGLE_SIGN_IN ->
            {
                if (resultCode == Activity.RESULT_OK) {
                    showLoadingView()
                    AuthFunctions.signInWithGoogle(data)
                }
            }
        }
    }

    override fun onBackPressed() {
        when (state.layoutState) {
            LayoutState.STATE_SIGN_IN, LayoutState.STATE_SIGN_UP, LayoutState.STATE_USERNAME ->
            {
                resetSignInFlow()
                showSignInOptionsLayout()
            }
            else -> super.onBackPressed()
        }
    }

/*
------------------------------------------------
    Sign In/Up Functions
-----------------------------------------------
*/

    private fun createUsername() {
        // clear textInput errors
        binding.layoutUsername.selectUsername.error = ""

        // get username input
        val username = binding.layoutUsername.selectUsername.editText?.text.toString()

        // check validity
        val isValidUsername = MyValidators.UsernameValidator(username)
            .addErrorCallback { binding.layoutUsername.selectUsername.error = getUsernameErrorString() }
            .check()

        // act if valid
        if (isValidUsername) {
            // start loading animation
            binding.layoutUsername.btnSelectUsername.startAnimation()

            // add user to Firebase
            FirestoreFunctions.User.addUser(username)
        }
    }

    private fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.client_id))
            .requestEmail()
            .requestProfile()
            .build()
        startActivityForResult(GoogleSignIn.getClient(this, gso).signInIntent, REQ_GOOGLE_SIGN_IN)
    }

    private fun signInWithEmail() {
        // clear textInput errors
        binding.layoutSignIn.email.error = ""
        binding.layoutSignIn.password.error = ""

        // get inputs
        val email = binding.layoutSignIn.email.editText?.text.toString()
        val password = binding.layoutSignIn.password.editText?.text.toString()

        // check validity
        val isValidEmailAndPassword = MyValidators.PasswordValidator(password)
            .addErrorCallback { binding.layoutSignIn.password.error = getPasswordErrorString() }
            .check()
                &&
                MyValidators.EmailValidator(email)
                    .addErrorCallback { binding.layoutSignIn.email.error = getString(R.string.err_invalid_email) }
                    .check()

        // act if valid
        if (isValidEmailAndPassword) {
            binding.layoutSignIn.btnSignIn.startAnimation()
            AuthFunctions.signInWithEmail(email, password)
        }
    }

    private fun signUpWithEmail() {
        // clear textInput errors
        binding.layoutSignUp.passwordConfirm.error = ""
        binding.layoutSignUp.password.error = ""
        binding.layoutSignUp.email.error = ""

        // get input
        val email = binding.layoutSignUp.email.editText?.text.toString()
        val password = binding.layoutSignUp.password.editText?.text.toString()
        val passwordConfirm = binding.layoutSignUp.passwordConfirm.editText?.text.toString()

        // check validity
        val isValidEmailAndPassword = MyValidators.PasswordValidator(password)
                .addErrorCallback { binding.layoutSignUp.password.error = getPasswordErrorString() }
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

        // act if valid
        if (isValidEmailAndPassword && passwordsMatch) {
            // start loading animation
            binding.layoutSignUp.btnSignUp.startAnimation()
            AuthFunctions.signUpWithEmail(email, password)
        }
    }

/*
------------------------------------------------
    Backend Listening Functions
-----------------------------------------------
*/

    private val signInErrorListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            onSignInError()
        }

        fun onSignInError() {
            when (val error = errorData.signInError.get()) {
                is FirebaseAuthUserCollisionException ->
                {
                    Log.e(TAG, "(signInErrorListener)1 - Error: $error")
                    stopLoadingViews()
                    showSignInError(R.string.err_account_made_title, R.string.err_account_made_desc)
                    errorData.signInError.set(null)
                }
                is FirebaseAuthInvalidCredentialsException ->
                {
                    Log.e(TAG, "(signInErrorListener)2 - Error: $error")
                    stopLoadingViews()
                    showSignInError(R.string.err_inv_email_pass, R.string.err_inv_email_pass_desc)
                    errorData.signInError.set(null)
                }
                is FirebaseAuthInvalidUserException ->
                {
                    Log.e(TAG, "(signInErrorListener)3 - Error: $error")
                    stopLoadingViews()
                    showSignInError(R.string.err_email_nonexistant, R.string.err_email_nonexistant_desc)
                    errorData.signInError.set(null)
                }
                is FirebaseFirestoreException ->
                {
                    Log.e(TAG, "(signInErrorListener)4 - Error: $error")
                    stopLoadingViews()
                    when (error.code) {
                        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                            showSignInError(R.string.username_taken_title, R.string.err_username_taken_desc)
                        else ->
                            resetSignInFlow()
                    }
                    errorData.signInError.set(null)
                }
                is Exception -> {
                    Log.e(TAG, "(signInErrorListener)5 - General Error: $error")
                    resetSignInFlow()
                    errorData.signInError.set(null)
                }
            }
        }
    }

    private val fetchUserDataErrorListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            onFetchUserDataError()
        }

        fun onFetchUserDataError() {
            val error = errorData.fetchUserDataError.get()
            if (error != null) {
                Log.e(TAG, "(fetchUserDataErrorListener) - Error: $error")
                resetSignInFlow()
                showUnknownError()
                errorData.fetchUserDataError.set(null)
            }
        }
    }

    private val usernameListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            if (tempData.tempUser.username.get()?.isNotEmpty() == true) {
                if (tempData.tempUser.hasFetchedInitialUserData())
                    nextActivity()
                else
                    FirestoreFunctions.fetchAllData()
            }
        }
    }

    private val signInListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            if (tempData.tempUser.uid.get()?.isNotEmpty() == true) {
                if (tempData.tempUser.username.get()?.isNotEmpty() == true) {
                    FirestoreFunctions.fetchAllData()
                    applyLayout(LayoutState.STATE_LOADING)
                } else {
                    stopLoadingViews()
                    applyLayout(LayoutState.STATE_USERNAME)
                }
            }
        }
    }

    private val hubDataListener =
        object : ObservableMap.OnMapChangedCallback<ObservableMap<String, Hub>, String, Hub>() {
            override fun onMapChanged(sender: ObservableMap<String, Hub>?, key: String?) {
                if (tempData.tempUser.hasFetchedInitialUserData())
                    nextActivity()
            }
        }

    private val remoteDataListener = object :
        ObservableMap.OnMapChangedCallback<ObservableMap<String, Remote>, String, Remote>() {
            override fun onMapChanged(sender: ObservableMap<String, Remote>?, key: String?) {
                if (tempData.tempUser.hasFetchedInitialUserData())
                    nextActivity()
            }
        }

/*
    ------------------------------------------------
        Layout Functions
    -----------------------------------------------
*/

    private fun applyLayout(layoutState: LayoutState = state.layoutState) {
        val animate = layoutState != state.layoutState
        when (layoutState) {
            LayoutState.STATE_INITIAL -> showSignInOptionsLayout()
            LayoutState.STATE_SIGN_IN_OPTIONS -> showSignInOptionsLayout(animate)
            LayoutState.STATE_SIGN_UP -> showSignUpLayout(animate)
            LayoutState.STATE_SIGN_IN -> showSignInLayout(animate)
            LayoutState.STATE_USERNAME -> showUsernameLayout(animate)
            LayoutState.STATE_LOADING -> showLoadingView()
        }
    }
    private fun moveLogoUp(animate: Boolean = true) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(this, R.layout.a_splash_show_login)
        constraintSet.constrainHeight(binding.splashLogo.id, 375)
        if (animate) {
            val layoutTransition = androidx.transition.AutoTransition().apply {
                interpolator = DecelerateInterpolator()
                duration = TRANSITION_DURATION.toLong()
            }
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
            binding.cardContainer.alpha = 1f
        }
    }

    private fun hideSignInContainer(onAnimEnd : () -> Unit) {
        val anim = AnimationUtils.loadAnimation(this, R.anim.slide_down)
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(p0: Animation?) {}
            override fun onAnimationEnd(p0: Animation?) {
                onAnimEnd()
            }
            override fun onAnimationStart(p0: Animation?) {}
        })
        binding.signInContainer.startAnimation(anim)
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

    private fun showLoadingView() {
        hideSignInContainer {
            binding.centerProgressBar.visibility = View.VISIBLE
            binding.signInContainer.visibility = View.GONE
        }
    }

    private fun stopLoadingViews() {
        binding.layoutUsername.btnSelectUsername.revertAnimation()
        binding.layoutSignUp.btnSignUp.revertAnimation()
        binding.layoutSignIn.btnSignIn.revertAnimation()
        binding.centerProgressBar.visibility = View.GONE
    }

    private fun showSignInOptionsLayout(animate: Boolean = true) {
        state.layoutState = LayoutState.STATE_SIGN_IN_OPTIONS
        moveLogoUp(animate)

        if (animate) {
            hideSignInContainer {
                showButtons()
                binding.layoutSignIn.signInContainer.visibility = View.GONE
                binding.layoutSignUp.signUpContainer.visibility = View.GONE
                binding.layoutUsername.usernameContainer.visibility = View.GONE
                binding.signInContainer.visibility = View.VISIBLE

                val upAnim = AnimationUtils.loadAnimation(this@SplashActivity, R.anim.slide_up)
                binding.signInContainer.startAnimation(upAnim)
            }
        } else {
            showButtons()
            binding.layoutSignIn.signInContainer.visibility = View.GONE
            binding.layoutSignUp.signUpContainer.visibility = View.GONE
            binding.layoutUsername.usernameContainer.visibility = View.GONE
            binding.signInContainer.visibility = View.VISIBLE
        }
    }

    private fun showSignUpLayout(animate : Boolean = true) {
        state.layoutState = LayoutState.STATE_SIGN_UP
        moveLogoUp(animate)

        if (animate) {
            hideSignInContainer {
                hideButtons()
                binding.layoutSignIn.signInContainer.visibility = View.GONE
                binding.layoutSignUp.signUpContainer.visibility = View.VISIBLE
                binding.layoutUsername.usernameContainer.visibility = View.GONE
                binding.signInContainer.visibility = View.VISIBLE

                val upAnim = AnimationUtils.loadAnimation(this@SplashActivity, R.anim.slide_up)
                binding.signInContainer.startAnimation(upAnim)
            }
        } else {
            hideButtons()
            binding.layoutSignIn.signInContainer.visibility = View.GONE
            binding.layoutSignUp.signUpContainer.visibility = View.VISIBLE
            binding.layoutUsername.usernameContainer.visibility = View.GONE
            binding.signInContainer.visibility = View.VISIBLE
        }
    }

    private fun showSignInLayout(animate: Boolean = true) {
        state.layoutState = LayoutState.STATE_SIGN_IN
        moveLogoUp(animate)

        if (animate) {
            hideSignInContainer {
                hideButtons()
                binding.layoutSignIn.signInContainer.visibility = View.VISIBLE
                binding.layoutSignUp.signUpContainer.visibility = View.GONE
                binding.layoutUsername.usernameContainer.visibility = View.GONE
                binding.signInContainer.visibility = View.VISIBLE

                val upAnim = AnimationUtils.loadAnimation(this@SplashActivity, R.anim.slide_up)
                binding.signInContainer.startAnimation(upAnim)
            }
        } else {
            hideButtons()
            binding.layoutSignIn.signInContainer.visibility = View.VISIBLE
            binding.layoutSignUp.signUpContainer.visibility = View.GONE
            binding.layoutUsername.usernameContainer.visibility = View.GONE
            binding.signInContainer.visibility = View.VISIBLE
        }
    }

    private fun showUsernameLayout(animate: Boolean = true) {
        state.layoutState = LayoutState.STATE_USERNAME
        moveLogoUp(animate)

        if (animate) {
            hideSignInContainer {
                hideButtons()
                binding.layoutSignIn.signInContainer.visibility = View.GONE
                binding.layoutSignUp.signUpContainer.visibility = View.GONE
                binding.layoutUsername.usernameContainer.visibility = View.VISIBLE
                binding.signInContainer.visibility = View.VISIBLE

                val upAnim = AnimationUtils.loadAnimation(this@SplashActivity, R.anim.slide_up)
                binding.signInContainer.startAnimation(upAnim)
            }
        } else {
            hideButtons()
            binding.layoutSignIn.signInContainer.visibility = View.GONE
            binding.layoutSignUp.signUpContainer.visibility = View.GONE
            binding.layoutUsername.usernameContainer.visibility = View.VISIBLE
            binding.signInContainer.visibility = View.VISIBLE
        }
    }

    private var errorFlashBar: Flashbar? = null
    private fun showSignInError(titleRes : Int, messageRes : Int) {
        if (errorFlashBar != null)
            errorFlashBar?.dismiss()

        errorFlashBar = Flashbar.Builder(this)
            .dismissOnTapOutside()
            .enableSwipeToDismiss()
            .title(titleRes)
            .backgroundColorRes(R.color.colorBgCardDark)
            .positiveActionTextColorRes(R.color.colorAccent)
            .titleColorRes(R.color.colorError)
            .message(messageRes)
            .positiveActionText(R.string.dismiss)
            .positiveActionTapListener(object : Flashbar.OnActionTapListener {
                override fun onActionTapped(bar: Flashbar) = bar.dismiss()
            })
            .barDismissListener(object: Flashbar.OnBarDismissListener {
                override fun onDismissProgress(bar: Flashbar, progress: Float) {}
                override fun onDismissing(bar: Flashbar, isSwiped: Boolean) {}

                override fun onDismissed(bar: Flashbar, event: Flashbar.DismissEvent) {
                    errorFlashBar = null
                }
            })
            .build()
        errorFlashBar?.show()
    }

    private var errorDialog: AlertDialog? = null
    private fun showUnknownError() {
        if (errorDialog != null)
            errorDialog?.dismiss()

        errorDialog = AlertDialog.Builder(this@SplashActivity)
            .setTitle(R.string.err_splash_sign_in_title)
            .setMessage(R.string.err_splash_sign_in_desc)
            .setPositiveButton(R.string.dismiss) { dialog, _ -> dialog.dismiss() }
            .setIcon(R.drawable.ic_error_red_24dp)
            .setOnDismissListener {
                errorDialog = null
                startActivity(Intent().setClass(this, SplashActivity::class.java))
                finish()
//                val upAnim = AnimationUtils.loadAnimation(this@SplashActivity, R.anim.slide_up)
//                binding.signInContainer.startAnimation(upAnim)
//                binding.signInContainer.visibility = View.VISIBLE
//                stopLoadingViews()
//                binding.splashContainer.requestFocusFromTouch()
                applyLayout(LayoutState.STATE_SIGN_IN_OPTIONS) }
            .show()

    }

    private fun resetSignInFlow() {
        stopLoadingViews()
        state.layoutState = LayoutState.STATE_SIGN_IN_OPTIONS
        FirestoreFunctions.clearAllListeners()
        tempData.resetAllData()
        userData.resetAllData()
        FirebaseAuth.getInstance().signOut()
    }

/*
------------------------------------------------
    Activity Switching Functions
-----------------------------------------------
*/

    private fun nextActivity() {
        when (userData.hubs.size) {
            // show 'setup first hub' or 'hub invitations' activity
            0 ->
            {
                //todo - check for hub invitations
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
            // show 'main view'
            else ->
            {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
        }
    }

    companion object {
        const val STATE = "SPLASH_STATE"
        const val TRANSITION_DURATION = 800
        const val TAG = "SplashActivity"

        enum class LayoutState {
            STATE_SIGN_IN_OPTIONS,
            STATE_SIGN_UP,
            STATE_SIGN_IN,
            STATE_USERNAME,
            STATE_LOADING,
            STATE_INITIAL
        }

        internal class State(
            var layoutState: LayoutState = LayoutState.STATE_INITIAL
        ) : Parcelable {
            constructor(parcel: Parcel) : this() {
                 layoutState = LayoutState.valueOf(parcel.readString() ?: LayoutState.STATE_SIGN_IN_OPTIONS.name)
            }

            override fun writeToParcel(parcel: Parcel, flags: Int) {
                parcel.writeString(layoutState.name)
            }

            override fun describeContents(): Int {
                return 0
            }

            companion object CREATOR : Parcelable.Creator<State> {
                override fun createFromParcel(parcel: Parcel): State {
                    return State(parcel)
                }

                override fun newArray(size: Int): Array<State?> {
                    return arrayOfNulls(size)
                }
            }

        }
    }
}
