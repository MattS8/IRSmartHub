package com.ms8.smartirhub.android.splash_login

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
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
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
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
import com.ms8.smartirhub.android._tests.dev_playground.TestEnvActivity
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.ASplashLoginMainBinding
import com.ms8.smartirhub.android.firebase.AuthActions
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.main_view.MainViewActivity
import com.ms8.smartirhub.android.models.firestore.Hub
import com.ms8.smartirhub.android.models.firestore.IrSignal
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import com.ms8.smartirhub.android.utils.MyValidators
import com.ms8.smartirhub.android.utils.extensions.DynamicStrings.getPasswordErrorString
import com.ms8.smartirhub.android.utils.extensions.DynamicStrings.getUsernameErrorString
import com.ms8.smartirhub.android.utils.extensions.RES_SIGN_IN
import com.ms8.smartirhub.android.utils.extensions.getGenericErrorFlashbar
import kotlinx.android.synthetic.main.a_splash_login_main.view.*
import kotlinx.android.synthetic.main.v_splash_login_sign_up.view.*
import kotlinx.android.synthetic.main.v_splash_login_username.view.*


class SplashActivity5 : AppCompatActivity() {
    lateinit var binding : ASplashLoginMainBinding
    lateinit var state : State

/*
    ------------------------------------------------
        Listeners
    -----------------------------------------------
*/

    private val errorListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            if (AppState.errorData.userSignInError.get() != null) {
                // stop any loading indicators
                stopLoadingViews()

                // stop listening for response
                removeResponseListeners()
                state.isListeningForCreateUsernameResponse = false
                state.isListeningForSignInResponse = false
                state.isListeningForUserData = false

                // show error message
                onSignInError(AppState.errorData.userSignInError.get()!!)

                // consume error
                AppState.errorData.userSignInError.set(null)

                // Go back to sign in options if came from Google Sign In process
                if (state.layoutState == SHOW_GOOGLE_PROG) {
                    state.layoutState = SHOW_OPTIONS
                    applyLayout()
                }
            }
        }
    }

    private val signInResponseListener = object : Observable.OnPropertyChangedCallback() {
        @SuppressLint("LogNotTimber")
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            when {
                // Sign In response completed
                AppState.userData.user.uid.get()?.isNotEmpty() == true -> {
                    // stop listening for more changes
                    state.isListeningForSignInResponse = false
                    stopListeningForSignInResponse()

                    // check if we already have username
                    if (AppState.userData.user.username.get()?.isNotEmpty() == true) {
                        FirestoreActions.listenToUserData2()
                        listenForUserData()
                    } else {
                        stopLoadingViews()
                        showUsernameLayout()
                    }
                }
                else -> {
                    Log.w("SplashActivity", "signInResponseListener called but user.uid is EMPTY")
                }
            }
        }
    }

    private val userFromUIDListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            state.isListeningForUserFromUID = false
            stopListeningForUserFromUID()

            if (AppState.userData.user.username.get()?.isNotEmpty() == true) {
                // got user
                FirestoreActions.listenToUserData2()
                listenForUserData()
            } else {
                // show 'create username' layout
                stopLoadingViews()
                showUsernameLayout()
            }
        }
    }

    private val usernameResponseListener = object : Observable.OnPropertyChangedCallback() {
        @SuppressLint("LogNotTimber")
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            if (AppState.userData.user.username.get()?.isNotEmpty() == true) {
                Log.d("T#usernameListener", "got username: ${AppState.userData.user.username.get()}")
                // stop any loading indicators
                stopLoadingViews()

                // stop listening for more changes
                state.isListeningForCreateUsernameResponse = false
                stopListeningForCreateUsernameResponse()

                // ensure that uid is still valid
                if (AppState.userData.user.uid.get()?.isNotEmpty() == true) {
                    nextActivity()
                } else {
                    onSignInError(Exception("Returned username '${AppState.userData.user.username}', but uid was previously wiped"))
                    AppState.userData.user.username.set("")
                    Log.w("SplashActivity", "returned a username, but uid was previously wiped")
                }
            } else {
                Log.w("SplashActivity", "usernameResponseListener called but user.username is EMPTY")
            }
        }
    }

    private val irSignalDataListener  = object : ObservableMap.OnMapChangedCallback<ObservableMap<String, IrSignal>, String, IrSignal>() {
        override fun onMapChanged(sender: ObservableMap<String, IrSignal>?, key: String?) {
            checkUserData()
        }
    }
    private val remoteDataListener  = object : ObservableMap.OnMapChangedCallback<ObservableMap<String, RemoteProfile>, String, RemoteProfile>() {
        override fun onMapChanged(sender: ObservableMap<String, RemoteProfile>?, key: String?) {
            checkUserData()
        }
    }
    private val hubDataListener  = object : ObservableMap.OnMapChangedCallback<ObservableMap<String, Hub>, String, Hub>() {
        override fun onMapChanged(sender: ObservableMap<String, Hub>?, key: String?) {
            checkUserData()
        }
    }

/*
    ------------------------------------------------
        Overridden Functions
    -----------------------------------------------
*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // get State
        state = savedInstanceState?.getParcelable(STATE) ?: State()

        // new firebase timestamp bs
        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setTimestampsInSnapshotsEnabled(true)
            .build()

        // bind layout
        binding = DataBindingUtil.setContentView(this, R.layout.a_splash_login_main)
        binding.apply {
            layoutSignIn.btnSignIn.setOnClickListener { signInWithEmail() }
            layoutSignUp.btnSignUp.setOnClickListener { signUpWithEmail() }
            layoutUsername.btnSelectUsername.setOnClickListener { createUsername() }
            btnSignIn.setOnClickListener { showSignInLayout() }
            btnSignUpEmail.setOnClickListener { showSignUpLayout() }
            signInGoogle.setOnClickListener { signInWithGoogle() }
        }

        // kick off initial backend tasks if needed
        if (!state.isListeningForCreateUsernameResponse
            && !state.isListeningForSignInResponse
            && !state.isWaitingForGoogleSignIn) {

            val currentUser = FirebaseAuth.getInstance().currentUser
            when {
            // Have both uid and username -> listen for user data
                currentUser?.uid != null && AppState.userData.user.username.get()?.isNotEmpty() == true -> {
                    state.isListeningForUserData = true
                    FirestoreActions.listenToUserData2()
                    addUserDataListeners()
                }
            // Have only uid -> get user from uid
                currentUser?.uid != null -> {
                    state.isListeningForUserFromUID = true
                    FirestoreActions.getUserFromUID()
                }
            // Have neither uid nor username -> show login options
                else -> {
                    state.layoutState = SHOW_OPTIONS
                    applyLayout()
                }
            }
        }
        // otherwise apply current
        else {
            applyLayout()
        }
    }

    override fun onResume() {
        super.onResume()
        if (state.isListeningForSignInResponse)
            listenForSignInResponse()
        if (state.isListeningForCreateUsernameResponse)
            listenForCreateUsernameResponse()
        if (state.isListeningForUserData)
            listenForUserData()
        if (state.isListeningForUserFromUID)
            listenForUserFromUID()

        // Determines if all user data was received in the background
        // If so, we need to move on!
        checkUserData()
    }

    override fun onPause() {
        super.onPause()
        removeResponseListeners()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE, state)
    }

    @SuppressLint("LogNotTimber")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RES_SIGN_IN -> {
                state.isWaitingForGoogleSignIn = false
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Log.d("T#onActivityResult", "waiting for sign in via google...")
                        AuthActions.handleGoogleSignInResult2(data)
                        showGoogleProgressBar()
                        listenForSignInResponse()
                    }
                }
            }
            else -> Log.w("SplashActivity", "Unknown request requestCode ($requestCode)")
        }
    }

    override fun onBackPressed() {
        val isShowingDetailView = state.layoutState == SHOW_SIGN_IN ||
                state.layoutState == SHOW_SIGN_UP ||
                state.layoutState == SHOW_USERNAME
        when {
            isShowingDetailView -> {
                stopLoadingViews()
                AppState.userData.removeData()
                showSignInOptionsLayout(true)
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

/*
    ------------------------------------------------
        OnClick Functions
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

            // add user to firebase
            FirestoreActions.addUser(username)

            // listen for 'create username' response
            listenForCreateUsernameResponse()
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

        // act if valid
        if (isValidEmailAndPassword && passwordsMatch) {
            // start loading animation
            binding.layoutSignUp.btnSignUp.startAnimation()
            AppState.userData.removeData()
            AuthActions.createAccount(email, password)
            listenForSignInResponse()
        }
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
            AppState.userData.removeData()
            AuthActions.signInWithEmail(email, password)
            listenForSignInResponse()
        }
    }

    private fun signInWithGoogle() {
        AppState.userData.removeData()
        state.isWaitingForGoogleSignIn = true
        AuthActions.signInWithGoogle(this@SplashActivity5)
    }

/*
    ------------------------------------------------
        Listening Functions
    -----------------------------------------------
*/

    private fun listenForUserFromUID() {
        when {
        // username already received
            AppState.userData.user.username.get()?.isNotEmpty() == true -> {
                state.isListeningForUserFromUID = false
                listenForUserData()
            }
        // error was received
            AppState.errorData.userSignInError.get() != null -> {
                state.isListeningForUserFromUID = false

                // show error message
                onSignInError(AppState.errorData.userSignInError.get()!!)

                // consume error
                AppState.errorData.userSignInError.set(null)
            }

        // still waiting for username
            else -> {
                state.isListeningForUserFromUID = true
                AppState.userData.user.uid.addOnPropertyChangedCallback(userFromUIDListener)
                AppState.errorData.userSignInError.addOnPropertyChangedCallback(errorListener)
            }
        }
    }

    private fun stopListeningForUserFromUID() {
        AppState.userData.user.uid.removeOnPropertyChangedCallback(userFromUIDListener)
        AppState.errorData.userSignInError.removeOnPropertyChangedCallback(errorListener)
    }

    private fun listenForUserData() {
        Log.d("T#listenForUserData", "Listening for data from ${AppState.userData.user.username.get()}")
        when {
        // user data already received
            AppState.userData.hasFetchedUserData() -> {
                state.isListeningForUserData = false
                Log.d("T#listenForUserData", "got username: ${AppState.userData.user.username.get()}")
                nextActivity()
            }
        // error was received
            AppState.errorData.userSignInError.get() != null -> {
                state.isListeningForUserData = false

                // show error message
                onSignInError(AppState.errorData.userSignInError.get()!!)

                // consume error
                AppState.errorData.userSignInError.set(null)
            }
        // still waiting for user data
            else -> {
                state.isListeningForUserData = true
                addUserDataListeners()
            }
        }
    }

    private fun addUserDataListeners() {
        AppState.userData.hubs.addOnMapChangedCallback(hubDataListener)
        AppState.userData.remotes.addOnMapChangedCallback(remoteDataListener)
        AppState.userData.irSignals.addOnMapChangedCallback(irSignalDataListener)
        AppState.errorData.userSignInError.addOnPropertyChangedCallback(errorListener)
    }

    private fun stopListeningForUserData() {
        // remove user data listeners
        AppState.userData.hubs.removeOnMapChangedCallback(hubDataListener)
        AppState.userData.remotes.removeOnMapChangedCallback(remoteDataListener)
        AppState.userData.irSignals.removeOnMapChangedCallback(irSignalDataListener)

        // remove error listener
        AppState.errorData.userSignInError.removeOnPropertyChangedCallback(errorListener)
    }

    private fun checkUserData() {
        if (AppState.userData.user.uid.get()?.isNotEmpty() == true && AppState.userData.hasFetchedUserData()) {
            Log.d("T#checkUserData", "Got all data! username = ${AppState.userData.user.username}")
            state.isListeningForUserData = false
            stopListeningForUserData()
            nextActivity()
        } else {
            Log.d("T#Splash5", "Not all user data has been collected yet...")
        }
    }

    private fun listenForSignInResponse() {
        when {
        // sign In response completed already
            AppState.userData.user.uid.get()?.isNotEmpty() == true -> {
                state.isListeningForSignInResponse = false
                // Check if we already have username
                if (AppState.userData.user.username.get()?.isNotEmpty() == true) {
                    Log.d("T#signInListener", "got username: ${AppState.userData.user.username}")
                    listenForUserData()
                } else {
                    showUsernameLayout()
                }
            }
        // sign In response returned an error
            AppState.errorData.userSignInError.get() != null -> {
                state.isListeningForSignInResponse = false

                // show error message
                onSignInError(AppState.errorData.userSignInError.get()!!)

                // Consume error
                AppState.errorData.userSignInError.set(null)
            }
        // still waiting for Sign In response
            else -> {
                state.isListeningForSignInResponse = true
                AppState.userData.user.uid.addOnPropertyChangedCallback(signInResponseListener)
                AppState.errorData.userSignInError.addOnPropertyChangedCallback(errorListener)
            }
        }
    }

    private fun stopListeningForSignInResponse() {
        // remove Sign In listener
        AppState.userData.user.uid.removeOnPropertyChangedCallback(signInResponseListener)

        // remove Error Listener
        AppState.errorData.userSignInError.removeOnPropertyChangedCallback(errorListener)
    }

    private fun listenForCreateUsernameResponse() {
        Log.d("T#listenForCreateUsrnme", "listening for username creation...")
        when {
        // username response completed already
            AppState.userData.user.username.get()?.isNotEmpty() == true -> {
                state.isListeningForCreateUsernameResponse = false
                listenForUserData()
            }
        // username response returned an error
            AppState.errorData.userSignInError.get() != null -> {
                state.isListeningForCreateUsernameResponse = false

                // show error
                onSignInError(AppState.errorData.userSignInError.get()!!)

                // consume error
                AppState.errorData.userSignInError.set(null)
            }
        // still waiting for username response
            else -> {
                state.isListeningForCreateUsernameResponse = true
                AppState.userData.user.username.addOnPropertyChangedCallback(usernameResponseListener)
                AppState.errorData.userSignInError.addOnPropertyChangedCallback(errorListener)
            }
        }
    }

    private fun stopListeningForCreateUsernameResponse() {
        // remove Username Listener
        AppState.userData.user.username.removeOnPropertyChangedCallback(usernameResponseListener)

        // remove Error Listener
        AppState.errorData.userSignInError.removeOnPropertyChangedCallback(errorListener)
    }

    private fun removeResponseListeners() {
        if (state.isListeningForSignInResponse)
            stopListeningForSignInResponse()
        if (state.isListeningForCreateUsernameResponse)
            stopListeningForCreateUsernameResponse()
        if (state.isListeningForUserData)
            stopListeningForUserData()
    }

    private fun nextActivity() {
        when (AppState.userData.hubs.size) {
            // show 'setup first hub' or 'hub invitations' activity
            0 ->
            {
                //todo check for hub invitations
                startActivity(Intent(this, MainViewActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
        // show 'main view'
            else ->
            {
                startActivity(Intent(this, MainViewActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
        }
    }

    private fun onSignInError(exception: Exception) {
        when (exception) {
            is FirebaseAuthUserCollisionException ->
                getGenericErrorFlashbar(true)
                    .title(R.string.err_account_made_title)
                    .message(R.string.err_acount_made_desc)
                    .build().show()
            is FirebaseAuthInvalidCredentialsException ->
                getGenericErrorFlashbar(true)
                    .title(R.string.err_inv_email_pass)
                    .message(R.string.err_inv_email_pass_desc)
                    .build().show()
            is FirebaseAuthInvalidUserException ->
                getGenericErrorFlashbar(true)
                    .title(R.string.err_email_nonexistant)
                    .message(R.string.err_email_nonexistant_desc)
                    .build().show()
            is FirebaseFirestoreException -> {
                when (exception.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                        getGenericErrorFlashbar(true)
                            .title(R.string.username_taken_title)
                            .message(R.string.err_username_taken_desc)
                            .build().show()
                    }
                    else -> {
                        getGenericErrorFlashbar(true)
                            .title(R.string.error)
                            .message(R.string.err_sign_in_desc)
                            .negativeActionText(R.string.report_issue)
                            .negativeActionTapListener(object : Flashbar.OnActionTapListener {
                                override fun onActionTapped(bar: Flashbar) {
                                    bar.dismiss()
                                    FirestoreActions.reportError(exception.message ?: exception.toString())
                                }
                            })
                    }
                }
            }
            else -> {
                getGenericErrorFlashbar(true)
                    .title(R.string.error)
                    .message(R.string.err_sign_in_desc)
                    .negativeActionText(R.string.report_issue)
                    .negativeActionTapListener(object : Flashbar.OnActionTapListener {
                        override fun onActionTapped(bar: Flashbar) {
                            bar.dismiss()
                            FirestoreActions.reportError(exception.message ?: exception.toString())
                        }
                    })
            }
        }
    }


/*
    ------------------------------------------------
        Layout Functions
    -----------------------------------------------
*/

    private fun applyLayout() {
        when (state.layoutState) {
            SHOW_OPTIONS -> { showSignInOptionsLayout() }
            SHOW_SIGN_IN -> { showSignInLayout() }
            SHOW_SIGN_UP -> { showSignUpLayout() }
            SHOW_USERNAME -> { showUsernameLayout() }
            SHOW_GOOGLE_PROG -> { showGoogleProgressBar() }
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


    private fun showGoogleProgressBar() {
        Log.d("T#showGoogProgBar", "Hiding")
        hideSignInContainer {
            binding.centerProgressBar.visibility = View.VISIBLE
            binding.signInContainer.visibility = View.GONE
        }
    }

    private fun moveLogoUp(animate: Boolean = true) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(this, R.layout.a_splash_login_show)
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
        }
    }

    private fun showUsernameLayout(animate: Boolean = true) {
        state.layoutState = SHOW_USERNAME
        moveLogoUp(animate)

        if (animate) {
            hideSignInContainer {
                hideButtons()
                binding.layoutSignIn.signInContainer.visibility = View.GONE
                binding.layoutSignUp.signUpContainer.visibility = View.GONE
                binding.layoutUsername.usernameContainer.visibility = View.VISIBLE
                binding.signInContainer.visibility = View.VISIBLE

                val upAnim = AnimationUtils.loadAnimation(this@SplashActivity5, R.anim.slide_up)
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

    private fun showSignInOptionsLayout(animate: Boolean = true) {
        state.layoutState = SHOW_OPTIONS
        moveLogoUp(animate)

        if (animate) {
            hideSignInContainer {
                showButtons()
                binding.layoutSignIn.signInContainer.visibility = View.GONE
                binding.layoutSignUp.signUpContainer.visibility = View.GONE
                binding.layoutUsername.usernameContainer.visibility = View.GONE
                binding.signInContainer.visibility = View.VISIBLE

                val upAnim = AnimationUtils.loadAnimation(this@SplashActivity5, R.anim.slide_up)
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
        state.layoutState = SHOW_SIGN_UP
        moveLogoUp(animate)

        if (animate) {
            hideSignInContainer {
                hideButtons()
                binding.layoutSignIn.signInContainer.visibility = View.GONE
                binding.layoutSignUp.signUpContainer.visibility = View.VISIBLE
                binding.layoutUsername.usernameContainer.visibility = View.GONE
                binding.signInContainer.visibility = View.VISIBLE

                val upAnim = AnimationUtils.loadAnimation(this@SplashActivity5, R.anim.slide_up)
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
        state.layoutState = SHOW_SIGN_IN
        moveLogoUp(animate)

        if (animate) {
            hideSignInContainer {
                hideButtons()
                binding.layoutSignIn.signInContainer.visibility = View.VISIBLE
                binding.layoutSignUp.signUpContainer.visibility = View.GONE
                binding.layoutUsername.usernameContainer.visibility = View.GONE
                binding.signInContainer.visibility = View.VISIBLE

                val upAnim = AnimationUtils.loadAnimation(this@SplashActivity5, R.anim.slide_up)
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
        binding.centerProgressBar.visibility = View.GONE
    }

    companion object {
        const val SHOW_SPLASH       = 0
        const val SHOW_OPTIONS      = 1
        const val SHOW_SIGN_IN      = 2
        const val SHOW_SIGN_UP      = 3
        const val SHOW_USERNAME     = 4
        const val SHOW_GOOGLE_PROG  = 5

        const val TRANSITION_DURATION = 800

        const val STATE = "SPLASH_STATE"

    }

    class State() : Parcelable {
        var isListeningForUserFromUID            : Boolean   = false
        var isListeningForUserData               : Boolean   = false
        var isListeningForCreateUsernameResponse : Boolean   = false
        var isListeningForSignInResponse         : Boolean   = false
        var isWaitingForGoogleSignIn             : Boolean   = false

        var layoutState                          : Int       = SHOW_SPLASH

        constructor(parcel: Parcel) : this() {
            isListeningForUserFromUID = parcel.readByte() != 0.toByte()
            isListeningForUserData = parcel.readByte() != 0.toByte()
            isListeningForCreateUsernameResponse = parcel.readByte() != 0.toByte()
            isListeningForSignInResponse = parcel.readByte() != 0.toByte()
            isWaitingForGoogleSignIn = parcel.readByte() != 0.toByte()
            layoutState = parcel.readInt()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeByte(if (isListeningForUserFromUID) 1 else 0)
            parcel.writeByte(if (isListeningForUserData) 1 else 0)
            parcel.writeByte(if (isListeningForCreateUsernameResponse) 1 else 0)
            parcel.writeByte(if (isListeningForSignInResponse) 1 else 0)
            parcel.writeByte(if (isWaitingForGoogleSignIn) 1 else 0)
            parcel.writeInt(layoutState)
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
