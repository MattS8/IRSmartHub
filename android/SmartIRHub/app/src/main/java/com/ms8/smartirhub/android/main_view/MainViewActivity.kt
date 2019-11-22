package com.ms8.smartirhub.android.main_view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.firebase.auth.FirebaseAuth
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.custom_views.bottom_sheets.BottomSheet
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.ActivityMainViewBinding
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.main_view.fragments.*
import com.ms8.smartirhub.android.remote_control.button.creation.ButtonCreator
import com.ms8.smartirhub.android.remote_control.button.models.Button
import com.ms8.smartirhub.android.remote_control.command.creation.CommandCreator
import com.ms8.smartirhub.android.remote_control.command.creation.GetFromRemoteActivity
import com.ms8.smartirhub.android.remote_control.creation.RemoteCreator
import com.ms8.smartirhub.android.remote_control.models.showUnknownRemoteSaveError
import com.ms8.smartirhub.android.remote_control.views.asymmetric_gridview.Utils
import com.ms8.smartirhub.android.utils.RequestCodes
import com.ms8.smartirhub.android.utils.extensions.*
import com.ms8.smartirhub.android.remote_control.command.creation.GetFromRemoteActivity.Companion.ResultType
import kotlinx.android.synthetic.main.f_remote_current.*

class MainViewActivity : AppCompatActivity() {

/*
----------------------------------------------
    View Variables
----------------------------------------------
*/

    // main binding
    private lateinit var binding : ActivityMainViewBinding
    private fun setupBinding() {
        binding
            .apply {
                frameLayout.adapter = pagerAdapter
                frameLayout.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
                    override fun onPageScrollStateChanged(state: Int) {}

                    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

                    override fun onPageSelected(position: Int) {
                        when (layoutState) {
                            LayoutState.REMOTES_FAV, LayoutState.REMOTES_FAV_EDITING -> {
                                if (position == 1) {
                                    layoutState = LayoutState.REMOTES_ALL
                                }
                            }
                            LayoutState.REMOTES_ALL -> {
                                if (position == 0) {
                                    layoutState = if (AppState.tempData.tempRemoteProfile.inEditMode.get())
                                        LayoutState.REMOTES_FAV_EDITING
                                    else LayoutState.REMOTES_FAV
                                }
                            }
                            LayoutState.DEVICES_HUBS -> {
                                if (position == 0) {
                                    layoutState = LayoutState.DEVICES_ALL
                                }
                            }
                            LayoutState.DEVICES_ALL -> {
                                if (position == 1) {
                                    layoutState = LayoutState.DEVICES_HUBS
                                }
                            }
                        }
                        showUiElements()
                    }
                })

                navView.layoutParams = CoordinatorLayout.LayoutParams(navView.layoutParams)
                    .apply {
                        val tv = TypedValue()
                        val navBarHeight = findNavBarHeight()
                        if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true) && navBarHeight > 0) {
                            height = TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics) + findNavBarHeight()
                        }
                        gravity = Gravity.BOTTOM
                    }
                navView.setPadding(navView.paddingLeft, navView.paddingTop, navView.paddingRight, findNavBarHeight())

                btnMyRemotes.setOnClickListener { onMyRemotesClicked() }
                btnMyDevices.setOnClickListener { onMyDevicesClicked() }
            }
    }

    // pages
    private val remoteFragment = OLD_RemoteFragment()
    private lateinit var pagerAdapter: MainViewAdapter
    private fun setupPagerAdapter() {
        pagerAdapter = MainViewAdapter(supportFragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, state.getNavPosition(), state.adapterBaseID)
            .apply {
                addFragment(remoteFragment, MainViewAdapter.Companion.ViewPagerList.REMOTES)
                addFragment(MyRemotesFragment(), MainViewAdapter.Companion.ViewPagerList.REMOTES)
                addFragment(MyDevicesFragment(), MainViewAdapter.Companion.ViewPagerList.DEVICES)
                addFragment(MyIRSmartHubsFragment(), MainViewAdapter.Companion.ViewPagerList.DEVICES)
            }
    }

    // drawer
    private lateinit var drawer: Drawer
    private fun setupDrawer() {
        // build drawer header layout
        val header = AccountHeaderBuilder()
            .withActivity(this)
            .withHeaderBackground(R.drawable.side_nav_bar)
            .addProfiles(
                ProfileDrawerItem()
                    .withName(AppState.userData.user.username.get())
                    .withEmail(FirebaseAuth.getInstance().currentUser?.email)
            )
            .withCompactStyle(true)
            .build()

        // Account for nav/status bar height
        val rotation = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            try {
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                binding.navView.setPadding(0, 0, 0, getNavBarHeight())
            } catch (e :Exception) {}
        }

        // setup side drawer
        drawer = DrawerBuilder()
            .withActivity(this)
            .withToolbar(binding.toolbar)
            .withAccountHeader(header)
            .addDrawerItems()
            .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
                override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*>): Boolean {
                    onDrawerItemClicked(view, position, drawerItem)
                    return false
                }
            })
            .build()
    }
    private fun onDrawerItemClicked(view: View?, position: Int, drawerItem: IDrawerItem<*>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    // exit warning sheet
    private lateinit var exitWarningSheet : BottomSheet
    private fun setupExitWarningSheet() {
        exitWarningSheet = BottomSheet(this,
            getString(R.string.exit_app_title),
            getString(R.string.exit_app_desc),
            getString(R.string.leave),
            getString(android.R.string.cancel),
            { finishAndRemoveTask() })
            .apply {
                setup()
            }
    }

    // 'create remote' dialog
    private var remoteCreator : RemoteCreator = RemoteCreator()
        .apply {
            onCreateBlankRemote = { createBlankRemote() }
            onCreateFromDeviceTemplate = { createFromDeviceTemplate() }
            onCreateFromExistingRemote = { createFromExistingRemote() }
        }

    // 'create button' dialog
//    private var buttonCreator : ButtonCreator = ButtonCreator()
//        .apply {
//            onCreateDialogDismiss = {
//                isShowingCreateButtonView = false
//            }
//            onCreateDialogShow = {
//                isShowingCreateButtonView = true
//            }
//            onCreationComplete = { button ->
//                when (button.type) {
//                    Button.Companion.ButtonStyle.STYLE_SPACE -> getGenericNotificationFlashbar(getString(R.string.added_space_button)).build().show()
//                    else -> getGenericNotificationFlashbar(getString(R.string.added_button)).build().show()
//                }
//            }
//            onRequestCommandFromRemote = { remote ->
//                val intent = Intent(this@MainViewActivity, GetFromRemoteActivity::class.java)
//                intent.putExtra(GetFromRemoteActivity.EXTRA_REMOTE_UID, remote.uid)
//                intent.putExtra(GetFromRemoteActivity.EXTRA_TYPE, ResultType.COMMAND)
//                startActivityForResult(intent, RequestCodes.GET_COMMAND_FROM_REMOTE)
//            }
//            onRequestActionsFromRemote = { remote ->
//                val intent = Intent(this@MainViewActivity, GetFromRemoteActivity::class.java)
//                intent.putExtra(GetFromRemoteActivity.EXTRA_REMOTE_UID, remote.uid)
//                intent.putExtra(GetFromRemoteActivity.EXTRA_TYPE, ResultType.ACTIONS)
//                startActivityForResult(intent, RequestCodes.GET_ACTIONS_FROM_REMOTE)
//            }
//        }

/*
----------------------------------------------
    State Variables
----------------------------------------------
*/

    private lateinit var state : State
    private var layoutState : LayoutState = LayoutState.REMOTES_FAV
    set(value) {
        field = value
        binding.toolbar.layoutState = field
        binding.fab.layoutState = field
    }
    private var isListeningForSaveRemoteConfirmation : Boolean = false
    set(value) {
        field = value
        binding.fab.isListeningForSaveRemoteConfirmation = field
    }
    private fun setupStateVariables() {
        // pager layout state
        layoutState = state.layoutState

        // remote creation saved state
        remoteCreator.setState(state.savedCreateRemoteState, this)
//        isShowingCreateRemoteView = state.isShowingCreateRemoteFromView
        isListeningForSaveRemoteConfirmation = state.isListeningForSaveRemoteConfirmation

        // button creation saved state
        remoteFragment.remoteLayout.buttonCreator.setState(state.savedCreateButtonState)

        // command creation saved state - todo
    }

/*
----------------------------------------------
    Overridden Functions
----------------------------------------------
*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_view)


        state = savedInstanceState?.get(STATE) as State? ?: State()

        setupStateVariables()
        setupExitWarningSheet()
        setupDrawer()
        setupPagerAdapter()
        setupBinding()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        state = State(
            layoutState,
            (binding.frameLayout.adapter as MainViewAdapter).getBaseItemId(),
            isListeningForSaveRemoteConfirmation,
            remoteCreator.getState(),
            remoteFragment.remoteLayout.buttonCreator.getState())
        outState.putParcelable(STATE, state)
    }

    override fun onPause() {
        super.onPause()

        remoteCreator.context = null

        if (isListeningForSaveRemoteConfirmation)
            removeSaveResponseListener()

//        if (AppState.tempData.tempRemoteProfile.inEditMode.get())
//            AppState.tempData.tempRemoteProfile.isCreatingNewButton.removeOnPropertyChangedCallback(newButtonListener)
//        AppState.tempData.tempButton.get()?.type?.removeOnPropertyChangedCallback(buttonStyleSelectedListener)
    }

    override fun onResume() {
        super.onResume()

        remoteCreator.context = this

        if (isListeningForSaveRemoteConfirmation){
            when {
            // save response occurred already
                !AppState.tempData.tempRemoteProfile.inEditMode.get() -> { editModeListener.onEditComplete() }
            // error occurred
                AppState.errorData.remoteSaveError.get() != null -> { remoteSaveErrorListener.onErrorOccurred() }
            // still waiting for response
                else -> { addSaveResponseListener() }
            }
        }

//        if (AppState.tempData.tempRemoteProfile.inEditMode.get())
//            AppState.tempData.tempRemoteProfile.isCreatingNewButton.addOnPropertyChangedCallback(newButtonListener)

//        when {
//            isShowingCreateRemoteView -> remoteFragment.remoteLayout.remoteCreator.showBottomDialog(this)
//            isShowingCreateButtonView -> remoteFragment.remoteLayout.buttonCreator.showBottomDialog()
//        }
    }

    override fun onBackPressed() {
        when {
        // check remoteTemplateSheet state
            //remoteTemplatesSheet.onBackPressed() -> {}
//        // perform back operation on remoteCreator
//            isShowingCreateRemoteView -> remoteCreator.onBackPressed(this)
//        // perform back operation on buttonCreator
//            isShowingCreateButtonView -> buttonCreator.onBackPressed(this)
        // show exit warning before leaving
            !exitWarningSheet.isShowing -> exitWarningSheet.show()
        // proceed with normal onBackPressed
            else ->  super.onBackPressed()
        }
    }

    @SuppressLint("LogNotTimber")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RequestCodes.GET_COMMAND_FROM_REMOTE ->
            {
                if (resultCode == Activity.RESULT_OK) {
                    remoteFragment.remoteLayout.buttonCreator.onCommandFromRemote()
                }
            }
            RequestCodes.GET_ACTIONS_FROM_REMOTE ->
            {
                if (resultCode == Activity.RESULT_OK) {
                    remoteFragment.remoteLayout.buttonCreator.onActionsFromRemote()
                }
            }
        }
    }

/*
 ----------------------------------------------
    Inner Layout Functions
 ----------------------------------------------
 */

    fun switchInnerPage(position : Int, smoothScroll : Boolean = true) {
        binding.frameLayout.setCurrentItem(position, smoothScroll)
    }

    fun hideUiElements() {
        if (!binding.fab.isOrWillBeHidden) {
            val tv = TypedValue()
            val navBarDist = if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true))
                TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
            else
                Utils.dpToPx(this, 56f)


            val interpolator = AccelerateInterpolator()
            binding.fab.hide()
            binding.fab.animate()
                .translationY(200f)
                .setDuration(300)
                .setInterpolator(interpolator)
                .start()
            binding.toolbar.animate()
                .alpha(0f)
                .translationY(-100f)
                .setDuration(300)
                .setInterpolator(interpolator)
                .start()
            binding.navView.animate()
                .translationY(navBarDist.toFloat())
                .setDuration(300)
                .setInterpolator(interpolator)
                .start()
            binding.btnMyRemotes.animate()
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(interpolator)
                .start()
            binding.btnMyDevices.animate()
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(interpolator)
                .start()
        }
    }

    fun showUiElements() {
        if (binding.fab.isOrWillBeHidden) {
            val interpolator = DecelerateInterpolator()
            binding.fab.show()
            binding.toolbar.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(interpolator)
                .start()
            binding.fab.animate()
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(interpolator)
                .start()
            binding.navView.animate()
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(interpolator)
                .start()
            binding.btnMyRemotes.animate()
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(interpolator)
                .start()
            binding.btnMyDevices.animate()
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(interpolator)
                .start()
        }
    }

/*
----------------------------------------------
    OnClick Functions
----------------------------------------------
*/

    private fun onMyDevicesClicked(forceUpdate: Boolean = false) {
        val noActionNeeded = (layoutState == LayoutState.DEVICES_ALL || layoutState == LayoutState.DEVICES_HUBS)
                && !forceUpdate
        if (noActionNeeded)
            return

        // set layoutState to default for this page
        layoutState = LayoutState.DEVICES_ALL

        // set inner page to default
        binding.frameLayout.currentItem = 0

        // tell pager adapter to move to devices page
        pagerAdapter.setNavPosition(FP_MY_DEVICES)
    }

    private fun onMyRemotesClicked(forceUpdate: Boolean = false) {
        val noActionNeeded =
            (layoutState == LayoutState.REMOTES_ALL
                || layoutState == LayoutState.REMOTES_FAV
                || layoutState == LayoutState.REMOTES_FAV_EDITING
            )
                && !forceUpdate
        if (noActionNeeded)
            return

        // set layoutState to proper state for 'my remotes' page
        layoutState = if (AppState.tempData.tempRemoteProfile.inEditMode.get()) {
            // currently editing remote
            LayoutState.REMOTES_FAV_EDITING
        } else {
            // not currently editing remote
            LayoutState.REMOTES_FAV
        }

        // set inner page to default
        binding.frameLayout.currentItem = 0

        // tell pager adapter to move to remotes page
        pagerAdapter.setNavPosition(FP_MY_REMOTES)
    }

    fun createRemote() {
        FirestoreActions.getRemoteTemplates()
        remoteCreator.showBottomDialog(this)

//        if (!state.isShowingCreateRemoteFromView) {
//            isShowingCreateRemoteView = true
//            FirestoreActions.getRemoteTemplates()
//            remoteCreator.showBottomDialog(this)
//        }
    }

    private fun createFromExistingRemote() {
        //todo show list of existing remotes, then copy selected remote
        getGenericComingSoonFlashbar().build().show()
    }

    private fun createFromDeviceTemplate() {
        //todo show list of device templates, then copy selected template
        getGenericComingSoonFlashbar().build().show()
    }

    private fun createBlankRemote() {
        // Trigger update to fragment
        onMyRemotesClicked(true)

        // show keyboard
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)

        // select toolbar title
        binding.toolbar.selectTitleText()

        // listen for calls to create a new button
//        AppState.tempData.tempRemoteProfile.isCreatingNewButton.addOnPropertyChangedCallback(newButtonListener)
    }

    fun editRemote() {
        Log.d("t#", "editRemote")
        AppState.tempData.tempRemoteProfile.inEditMode.set(true)
        layoutState = LayoutState.REMOTES_FAV_EDITING

        // listen for calls to create a new button
//        AppState.tempData.tempRemoteProfile.isCreatingNewButton.addOnPropertyChangedCallback(newButtonListener)

        // Trigger update to fragment
        onMyRemotesClicked(true)
    }

    fun saveRemote() {
        // Check remote for valid name
        if (AppState.tempData.tempRemoteProfile.saveRemote(this)) {
            // stop listening for calls to create a new button
//            AppState.tempData.tempRemoteProfile.isCreatingNewButton.removeOnPropertyChangedCallback(newButtonListener)

            // set fab to loading animation
            isListeningForSaveRemoteConfirmation = true

            // listen for response
            addSaveResponseListener()
        }
    }

/*
-----------------------------------------------
    Database Listeners
-----------------------------------------------
*/

//    private val buttonStyleSelectedListener = object : Observable.OnPropertyChangedCallback() {
//        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
//            onStyleSelected()
//        }
//
//        @SuppressLint("LogNotTimber")
//        private fun onStyleSelected() {
//            Log.d("t#", "onStyleSelected: ${AppState.tempData.tempButton.get()?.type?.get()}")
//            when (AppState.tempData.tempButton.get()?.type?.get()) {
//                Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND,
//                Button.Companion.ButtonStyle.STYLE_BTN_NO_MARGIN ->
//                {
//                    buttonCreator.dialogState = ButtonCreator.ButtonDialogState.COMMAND_LIST
//                    buttonCreator.showBottomDialog(this@MainViewActivity)
//                }
//                Button.Companion.ButtonStyle.STYLE_BTN_INCREMENTER_VERTICAL,
//                Button.Companion.ButtonStyle.STYLE_BTN_RADIAL,
//                Button.Companion.ButtonStyle.STYLE_BTN_RADIAL_W_CENTER ->
//                {
//                    startActivityForResult(Intent(this@MainViewActivity, SetupButtonActivity::class.java), RES_BUTTON_SETUP)
//                }
//                Button.Companion.ButtonStyle.STYLE_SPACE ->
//                {
//                    AppState.tempData.tempRemoteProfile.buttons.add(Button(Button.Companion.ButtonStyle.STYLE_SPACE))
//                    getGenericNotificationFlashbar(getString(R.string.added_space_button)).build().show()
//                }
//                Button.Companion.ButtonStyle.STYLE_CREATE_BUTTON -> TODO()
//                null -> Log.w("MainViewActivity", "onStyleSelected - button type was NULL")
//            }
//        }
//    }

//    private val newButtonListener = object : Observable.OnPropertyChangedCallback() {
//        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
//            Log.d("t#", "newButtonListener triggered! (isCreatingNewButton = ${AppState.tempData.tempRemoteProfile.isCreatingNewButton.get()} | isShowingCreateButtonView = $isShowingCreateButtonView)")
//            if (AppState.tempData.tempRemoteProfile.isCreatingNewButton.get() && !isShowingCreateButtonView) {
//                Log.d("t#", "onTempButtonChanged - showing dialog...")
//                remoteFragment.remoteLayout.buttonCreator.showBottomDialog()
//            }
//        }
//    }

    private val editModeListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            onEditComplete()
        }

        fun onEditComplete() {
            isListeningForSaveRemoteConfirmation = false
            if (layoutState == LayoutState.REMOTES_FAV_EDITING)
                layoutState = LayoutState.REMOTES_FAV
            removeSaveResponseListener()
        }
    }

    private val remoteSaveErrorListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            onErrorOccurred()
        }

        fun onErrorOccurred() {
            AppState.errorData.remoteSaveError.get()?.let {
                isListeningForSaveRemoteConfirmation = false
                showUnknownRemoteSaveError()
                removeSaveResponseListener()
                binding.fab.applyLayoutState()
            }
        }
    }

    private fun addSaveResponseListener() {
        // listen for success via change to remote.isInEditMode
        AppState.tempData.tempRemoteProfile.inEditMode.addOnPropertyChangedCallback(editModeListener)

        // listen for failure via change to AppState.saveRemoteError
        AppState.errorData.remoteSaveError.addOnPropertyChangedCallback(remoteSaveErrorListener)
    }

    private fun removeSaveResponseListener() {
        AppState.tempData.tempRemoteProfile.inEditMode.removeOnPropertyChangedCallback(editModeListener)
        AppState.errorData.remoteSaveError.removeOnPropertyChangedCallback(remoteSaveErrorListener)
    }

    companion object {
        const val REQ_NEW_IR_SIG = 2
        const val STATE = "MAIN_STATE"

      // Fragment Positions
        const val FP_MY_REMOTES = 0
        const val FP_MY_DEVICES = 1

        enum class LayoutState(val value: Int) {
            REMOTES_FAV(1),
            REMOTES_FAV_EDITING(2),
            REMOTES_ALL(3),
            DEVICES_HUBS(4),
            DEVICES_ALL(5)
        }
        fun layoutStateFromInt(stateAsInt: Int) = LayoutState.values().associateBy(LayoutState::value)[stateAsInt]
    }

/*
----------------------------------------------
    Activity State
----------------------------------------------
*/

    internal class State() : Parcelable {
        var layoutState                          : LayoutState  = LayoutState.REMOTES_FAV
            private set
        var adapterBaseID                        : Long         = 0
            private set
        var isListeningForSaveRemoteConfirmation : Boolean      = false
            private set
        var savedCreateRemoteState         : RemoteCreator.State = RemoteCreator.State(RemoteCreator.RemoteDialogState.CREATE_FROM, false)
            private set
        var savedCreateButtonState               : ButtonCreator.State = ButtonCreator.State(
                                                    ButtonCreator.ButtonDialogState.CHOOSE_TYPE,
                                                    CommandCreator.State(
                                                        CommandCreator.CommandDialogState.COMMAND_FROM,
                                                        0,
                                                        null,
                                                        isPairing = false,
                                                        isSavingIrSignal = false,
                                                        isSavingCommand = false
                                                    ),
                                        0)
            private set

        constructor(layoutState: LayoutState,
                    adapterBaseID: Long,
                    isListeningForSaveRemoteConfirmation: Boolean,
                    savedCreateRemoteState: RemoteCreator.State,
                    savedCreateButtonState: ButtonCreator.State)
                : this()
        {
            this.layoutState = layoutState
            this.adapterBaseID = adapterBaseID
            this.isListeningForSaveRemoteConfirmation = isListeningForSaveRemoteConfirmation
            this.savedCreateRemoteState = savedCreateRemoteState
            this.savedCreateButtonState = savedCreateButtonState
        }

        constructor(parcel: Parcel) : this() {
            layoutState = layoutStateFromInt(parcel.readInt()) ?: LayoutState.REMOTES_FAV
            adapterBaseID = parcel.readLong()
            isListeningForSaveRemoteConfirmation = parcel.readByte() != 0.toByte()
            savedCreateRemoteState = RemoteCreator.readStateFromParcel(parcel)
            savedCreateButtonState = ButtonCreator.readStateFromParcel(parcel)
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(layoutState.value)
            parcel.writeLong(adapterBaseID)
            parcel.writeByte(if (isListeningForSaveRemoteConfirmation) 1 else 0)
            RemoteCreator.writeToParcel(parcel, savedCreateRemoteState)
            ButtonCreator.writeToParcel(parcel, savedCreateButtonState)
        }

        fun getNavPosition() =
            when (layoutState) {
                LayoutState.REMOTES_FAV, LayoutState.REMOTES_FAV_EDITING, LayoutState.REMOTES_ALL  -> FP_MY_REMOTES
                LayoutState.DEVICES_HUBS, LayoutState.DEVICES_ALL -> FP_MY_DEVICES
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

/*
----------------------------------------------
    Extension Functions
----------------------------------------------
*/

//    private lateinit var binding : ActivityMainViewBinding
//    private lateinit var drawer : Drawer
//    private lateinit var state : State
//    private lateinit var pagerAdapter: MainViewAdapter
//
//    private val remoteFragment = RemoteFragment()
//    private lateinit var exitWarningSheet : BottomSheet
//
//    private var createRemoteFromBinding : VCreateRemoteFromBinding? = null
//    private var createRemoteDialog : BottomSheetDialog? = null
//
///*
//-----------------------------------------------
//    Database Listeners
//-----------------------------------------------
//*/
//
///*
//----------------------------------------------
//    Overridden Functions
//----------------------------------------------
//*/
//
//    override fun onBackPressed() {
//        when {
//        // check remoteTemplateSheet state
//            //remoteTemplatesSheet.onBackPressed() -> {}
//        // show exit warning before leaving
//            !exitWarningSheet.isShowing -> { exitWarningSheet.show() }
//        // proceed with normal onBackPressed
//            else -> { super.onBackPressed() }
//        }
//    }
//
//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        outState.putParcelable(STATE, state.apply { adapterBaseID = pagerAdapter.getBaseItemId() })
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_view)
//
//        // setup exit warning sheet
//        exitWarningSheet = BottomSheet(this,
//            this@MainViewActivity.getString(R.string.exit_app_title),
//            this@MainViewActivity.getString(R.string.exit_app_desc),
//            this@MainViewActivity.getString(R.string.leave),
//            this@MainViewActivity.getString(android.R.string.cancel),
//            { finishAndRemoveTask() })
//            .apply {
//                setup()
//            }
//
//        // set up side drawer
//        buildSideDrawer()
//
//        // get state
//        state = savedInstanceState?.get(STATE) as State? ?: State()
//
//        // check to see if we were waiting for "save remote" response. Act on any change that happened during config change or keep listening
//        if (state.isListeningForSaveRemoteConfirmation)
//            if (!AppState.tempData.tempRemoteProfile.inEditMode.get())
//                state.isListeningForSaveRemoteConfirmation = false
//            else if (AppState.errorData.remoteSaveError.get() != null)
//                showRemoteSaveError()
//                    .also { state.isListeningForSaveRemoteConfirmation = false }
//            else {
//                addSaveResponseListeners()
//            }
//
//        // set tempRemote to fav if none is currently showing
//        AppState.userData.remotes.forEach {
//            Log.d("TEST_REMOTES", "\t - $it")
//        }
//        Log.d("TEST", "tempRemoteProfile.uid = ${AppState.tempData.tempRemoteProfile.uid}, " +
//                "inEditMode = ${AppState.tempData.tempRemoteProfile.inEditMode.get()}" +
//                ", contains ${AppState.userData.user.favRemote} = ${AppState.userData.remotes.containsKey(AppState.userData.user.favRemote)}")
//        if (AppState.tempData.tempRemoteProfile.uid.isEmpty()
//            && !AppState.tempData.tempRemoteProfile.inEditMode.get()
//            && AppState.userData.remotes.containsKey(AppState.userData.user.favRemote)
//        ) {
//            AppState.tempData.tempRemoteProfile.copyFrom(AppState.userData.remotes[AppState.userData.user.favRemote])
//        }
//
//        // setup fragment pager adapter
//        pagerAdapter = MainViewAdapter(supportFragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, state.navPosition, state.adapterBaseID)
//            .apply {
//                addFragment(MyCommandsFragment(), MainViewAdapter.Companion.ViewPagerList.COMMANDS)
//                addFragment(MyIrSignalsFragment(), MainViewAdapter.Companion.ViewPagerList.COMMANDS)
//                addFragment(remoteFragment, MainViewAdapter.Companion.ViewPagerList.REMOTES)
//                addFragment(MyRemotesFragment(), MainViewAdapter.Companion.ViewPagerList.REMOTES)
//                addFragment(MyDevicesFragment(), MainViewAdapter.Companion.ViewPagerList.DEVICES)
//                addFragment(MyIRSmartHubsFragment(), MainViewAdapter.Companion.ViewPagerList.DEVICES)
//            }
//
//        // setup bindings
//        binding
//            .apply {
//                frameLayout.adapter = pagerAdapter
//                frameLayout.addOnPageChangeListener(pageChangeCallback)
//
//                navView.layoutParams = CoordinatorLayout.LayoutParams(navView.layoutParams)
//                    .apply {
//                        val tv = TypedValue()
//                        val navBarHeight = findNavBarHeight()
//                        if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true) && navBarHeight > 0) {
//                            height = TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics) + findNavBarHeight()
//                        }
//                        gravity = Gravity.BOTTOM
//                    }
//                navView.setPadding(navView.paddingLeft, navView.paddingTop, navView.paddingRight, findNavBarHeight())
//
//                btnMyRemotes.setOnClickListener { onMyRemotesClicked() }
//                btnMyDevices.setOnClickListener { onMyDevicesClicked() }
//            }
//
//        // set currently shown view
//        when (state.navPosition) {
//            FP_MY_DEVICES -> onMyDevicesClicked(true)
//            FP_MY_REMOTES -> onMyRemotesClicked(true)
//        }
//
//        // check if was showing bottom sheets
//        when {
//            state.isShowingCreateRemoteView -> createRemote(true)
//        }
//    }
//
//    private fun buildSideDrawer() {
//        // build drawer header layout
//        val header = AccountHeaderBuilder()
//            .withActivity(this)
//            .withHeaderBackground(R.drawable.side_nav_bar)
//            .addProfiles(
//                ProfileDrawerItem()
//                    .withName(AppState.userData.user.username.get())
//                    .withEmail(FirebaseAuth.getInstance().currentUser?.email)
//            )
//            .withCompactStyle(true)
//            .build()
//
//        // Account for nav/status bar height
//        val rotation = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
//        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
//            try {
//                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
//                binding.navView.setPadding(0, 0, 0, getNavBarHeight())
//            } catch (e :Exception) {}
//        }
//
//        // setup side drawer
//        drawer = DrawerBuilder()
//            .withActivity(this)
//            .withToolbar(binding.toolbar)
//            .withAccountHeader(header)
//            .addDrawerItems()
//            .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
//                override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*>): Boolean {
//                    onDrawerItemClicked(view, position, drawerItem)
//                    return false
//                }
//            })
//            .build()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        AppState.tempData.tempRemoteProfile.inEditMode.removeOnPropertyChangedCallback(editModeListener)
//        AppState.errorData.remoteSaveError.removeOnPropertyChangedCallback(remoteSaveErrorListener)
//    }
//
///*
// ----------------------------------------------
//    Inner Layout Functions
// ----------------------------------------------
// */
//
//    private fun onMyRemotesClicked(forceUpdate: Boolean = false) {
//        // do nothing if 'my remotes' is already shown
//        if (state.navPosition == FP_MY_REMOTES && !forceUpdate)
//            return
//
//        // update nav position
//        state.navPosition = FP_MY_REMOTES
//
//        // set toolbar title
//        setupToolbar()
//
//        // set nav buttons selected
//        binding.btnMyRemotes.isSelected = true
//        binding.btnMyDevices.isSelected = false
//
//        // set fab label and function
//        setupFab()
//
//        // update pagerAdapter and viewPager
//        pagerAdapter.setNavPosition(state.navPosition)
//        binding.frameLayout.setCurrentItem(state.viewPagerPosition, false)
//    }
//
//    private fun onMyDevicesClicked(forceUpdate: Boolean = false) {
//        // do nothing if 'my devices' is already shown
//        if (state.navPosition == FP_MY_DEVICES && !forceUpdate)
//            return
//
//        // update nav position
//        state.navPosition = FP_MY_DEVICES
//
//        // set toolbar title
//        setupToolbar()
//
//        // set nav buttons selected
//        binding.btnMyRemotes.isSelected = false
//        binding.btnMyDevices.isSelected = true
//
//        // set fab label and function
//        setupFab()
//
//        // update pagerAdapter and viewPager
//        pagerAdapter.setNavPosition(state.navPosition)
//        binding.frameLayout.setCurrentItem(state.viewPagerPosition, false)
//    }
//
//    private val pageChangeCallback = object: ViewPager.OnPageChangeListener {
//        override fun onPageScrollStateChanged(state: Int) {}
//
//        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
//
//        override fun onPageSelected(position: Int) {
//            Log.d("TEST", "POSITION $position SELECTED")
//            state.viewPagerPosition = position
//            setupFab()
//            setupToolbar()
//            showUiElements()
//        }
//    }
//
//    @SuppressLint("LogNotTimber")
//    private fun setupToolbar() {
//        when (state.navPosition) {
//        // 'My Remotes' page is currently shown
//            FP_MY_REMOTES -> {
//                when (state.viewPagerPosition) {
//                    VP_FAV_REMOTE -> {
//                        // todo set large margins
//
//                        if (AppState.tempData.tempRemoteProfile.inEditMode.get()) {
//                            // make title editable
//                            binding.toolbar.setTitleMode(ToolbarCenteredTitle.REMOTE_TITLE_EDITABLE)
//                        } else {
//                            // make title not editable
//                            binding.toolbar.setTitleMode(ToolbarCenteredTitle.REMOTE_TITLE)
//                        }
//
//                        Log.d("Toolbar", "tempRemoteProfile.name = ${AppState.tempData.tempRemoteProfile.name}")
//                        // set title text
//                        if (AppState.tempData.tempRemoteProfile.name == "") {
//                            binding.toolbar.setTitleHint(getString(R.string.name_new_remote))
//                        } else {
//                            binding.toolbar.title = AppState.tempData.tempRemoteProfile.name
//                        }
//                    }
//                    VP_ALL_REMOTES -> {
//                        // make title not editable
//                        binding.toolbar.setTitleMode(ToolbarCenteredTitle.NORMAL_TITLE_CENTERED)
//
//                        // set title text
//                        binding.toolbar.title = getString(R.string.all_remotes)
//                    }
//                    else -> {
//                        Log.e("setupToolbar", "unknown viewpager state: ${state.viewPagerPosition}")
//                    }
//                }
//            }
//        // 'My Devices' page is currently show
//            FP_MY_DEVICES -> {
//                binding.toolbar.setTitleMode(ToolbarCenteredTitle.NORMAL_TITLE_CENTERED)
//
//                when (state.viewPagerPosition) {
//                    VP_DEVICES -> {
//                        binding.toolbar.title = getString(R.string.title_my_devices)
//                    }
//                    VP_IRSMART_DEVICES -> {
//                        binding.toolbar.title = getString(R.string.title_my_ir_hubs)
//                    }
//                    else -> {
//                        Log.e("setupToolbar", "unknown viewpager state: ${state.viewPagerPosition}")
//                    }
//                }
//            }
//        // Unknown nav position
//            else -> {
//                Log.e("setupToolbar", "unknown nav position: ${state.navPosition}")
//            }
//        }
//    }
//
//    private fun setupFab() {
//        Log.d("TEST", "state.navPosition = ${state.navPosition} (VP_FAV_REMOTE = $VP_FAV_REMOTE)")
//        when (state.navPosition) {
//            // My Remotes
//            FP_MY_REMOTES -> {
//                when {
//                    state.viewPagerPosition == VP_FAV_REMOTE -> {
//                        if (AppState.userData.remotes.size == 0 && !AppState.tempData.tempRemoteProfile.inEditMode.get()) {
//                            // Creating first remote
//                            binding.fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_new_remote_icon))
//                            binding.fab.imageTintList = ContextCompat.getColorStateList(this, R.color.black)
//                            binding.fab.setOnClickListener { createRemote() }
//                        } else {
//                            when {
//                                // Waiting for remote saved confirmation
//                                state.isListeningForSaveRemoteConfirmation -> {
//                                    Log.d("TEST", "Waiting for save response...")
//                                    val animatedDrawable = AnimatedVectorDrawableCompat.create(this, R.drawable.edit_to_save)
//                                        ?.apply {
//                                            registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
//                                                override fun onAnimationEnd(drawable: Drawable?) {
//                                                    val savingDrawable = AnimatedVectorDrawableCompat.create(this@MainViewActivity, R.drawable.remote_saving)
//                                                        ?.apply {
//                                                            registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
//                                                                override fun onAnimationEnd(drawable: Drawable?) {
//                                                                    binding.fab.post { start() }
//                                                                }
//                                                            })
//                                                        }
//                                                    if (this@MainViewActivity.state.navPosition == VP_FAV_REMOTE && this@MainViewActivity.state.isListeningForSaveRemoteConfirmation) {
//                                                        binding.fab.setImageDrawable(savingDrawable)
//                                                        binding.fab.imageTintList = ContextCompat.getColorStateList(this@MainViewActivity, R.color.black)
//                                                        savingDrawable?.start()
//                                                    }
//                                                }
//                                            })
//                                        }
//
//                                    binding.fab.setImageDrawable(animatedDrawable)
//                                    animatedDrawable?.start()
//                                }
//
//                                // Editing current remote
//                                AppState.tempData.tempRemoteProfile.inEditMode.get() -> {
//                                    Log.d("TEST", "In Edit Mode")
//                                    binding.fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_done_green_24dp))
//                                    binding.fab.imageTintList = ContextCompat.getColorStateList(this, R.color.md_green_300)
//                                    binding.fab.setOnClickListener { saveRemoteEdits() }
//                                }
//
//                                // Not editing current remote
//                                else -> {
//                                    Log.d("TEST", "Not In Edit Mode")
//                                    binding.fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mode_edit_black_24dp))
//                                    binding.fab.imageTintList = ContextCompat.getColorStateList(this, R.color.black)
//                                    binding.fab.setOnClickListener { editRemote() }
//                                }
//                            }
//                        }
//                    }
//                    state.viewPagerPosition == VP_ALL_REMOTES -> {
//                        binding.fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_new_remote_icon))
//                        binding.fab.imageTintList = ContextCompat.getColorStateList(this, R.color.black)
//                        binding.fab.setOnClickListener { createRemote() }
//                    }
//                }
//            }
//            // My Devices
//            FP_MY_DEVICES -> {
//                when (state.viewPagerPosition) {
//                // Add Predefined Devices
//                    VP_DEVICES -> {
//                        // todo set fab icon
//                        binding.fab.setOnClickListener { addDevice() }
//                    }
//                // Set Up IRSmartHub
//                    VP_IRSMART_DEVICES -> {
//                        // todo set fab icon
//                        binding.fab.setOnClickListener { setupNewHub() }
//                    }
//                }
//            }
//        }
//    }
//
//    fun hideUiElements() {
//        if (!binding.fab.isOrWillBeHidden) {
//            val tv = TypedValue()
//            val navBarDist = if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true))
//                TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
//            else
//                Utils.dpToPx(this, 56f)
//
//
//            val interpolator = AccelerateInterpolator()
//            binding.fab.hide()
//            binding.fab.animate()
//                .translationY(200f)
//                .setDuration(300)
//                .setInterpolator(interpolator)
//                .start()
//            binding.toolbar.animate()
//                .alpha(0f)
//                .translationY(-100f)
//                .setDuration(300)
//                .setInterpolator(interpolator)
//                .start()
//            binding.navView.animate()
//                .translationY(navBarDist.toFloat())
//                .setDuration(300)
//                .setInterpolator(interpolator)
//                .start()
//            binding.btnMyRemotes.animate()
//                .alpha(0f)
//                .setDuration(300)
//                .setInterpolator(interpolator)
//                .start()
//            binding.btnMyDevices.animate()
//                .alpha(0f)
//                .setDuration(300)
//                .setInterpolator(interpolator)
//                .start()
//        }
//    }
//
//    fun showUiElements() {
//        if (binding.fab.isOrWillBeHidden) {
//            val interpolator = DecelerateInterpolator()
//            binding.fab.show()
//            binding.toolbar.animate()
//                .alpha(1f)
//                .translationY(0f)
//                .setDuration(300)
//                .setInterpolator(interpolator)
//                .start()
//            binding.fab.animate()
//                .translationY(0f)
//                .setDuration(300)
//                .setInterpolator(interpolator)
//                .start()
//            binding.navView.animate()
//                .translationY(0f)
//                .setDuration(300)
//                .setInterpolator(interpolator)
//                .start()
//            binding.btnMyRemotes.animate()
//                .alpha(1f)
//                .setDuration(300)
//                .setInterpolator(interpolator)
//                .start()
//            binding.btnMyDevices.animate()
//                .alpha(1f)
//                .setDuration(300)
//                .setInterpolator(interpolator)
//                .start()
//        }
//    }
//
//    private fun showRemoteSaveError() {
//        getGenericErrorFlashbar(true)
//            .message(getString(R.string.err_unknown_save_remote))
//            .build()
//            .show()
//            .also {
//                Log.e("SaveRemote", "$it")
//                AppState.errorData.remoteSaveError.removeOnPropertyChangedCallback(remoteSaveErrorListener)
//                AppState.errorData.remoteSaveError.set(null)
//            }
//    }
//
///*
// ----------------------------------------------
//    Navigation Functions
// ----------------------------------------------
// */
//
//    private fun onDrawerItemClicked(view: View?, position: Int, drawerItem: IDrawerItem<*>) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
///*
//----------------------------------------------
//    OnClick Functions
//----------------------------------------------
//*/
//
//    private fun editRemote() {
//        AppState.tempData.tempRemoteProfile.inEditMode.set(true)
//        setupToolbar()
//        setupFab()
//    }
//
//    private val editModeListener = object : Observable.OnPropertyChangedCallback() {
//        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
//            Log.d("TEST", "Changed!")
//            state.isListeningForSaveRemoteConfirmation = false
//            removeSaveResponseListener()
//            setupToolbar()
//            setupFab()
//        }
//    }
//
//    private val remoteSaveErrorListener = object : Observable.OnPropertyChangedCallback() {
//        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
//            AppState.errorData.remoteSaveError.get()?.let {
//                state.isListeningForSaveRemoteConfirmation = false
//                removeSaveResponseListener()
//                showRemoteSaveError()
//                setupFab()
//            }
//        }
//    }
//
//    private fun removeSaveResponseListener() {
//        AppState.tempData.tempRemoteProfile.inEditMode.removeOnPropertyChangedCallback(editModeListener)
//        AppState.errorData.remoteSaveError.removeOnPropertyChangedCallback(remoteSaveErrorListener)
//    }
//
//    private fun addSaveResponseListeners() {
//        AppState.tempData.tempRemoteProfile.inEditMode.addOnPropertyChangedCallback(editModeListener)
//        AppState.errorData.remoteSaveError.addOnPropertyChangedCallback(remoteSaveErrorListener)
//    }
//
//    private fun saveRemoteEdits() {
//        // Check remote for valid name
//
//        if (AppState.tempData.tempRemoteProfile.saveRemote(this)) {
//            // set fab to loading animation
//            state.isListeningForSaveRemoteConfirmation = true
//            setupFab()
//
//            // listen for success via change to remote.isInEditMode
//            AppState.tempData.tempRemoteProfile.inEditMode.addOnPropertyChangedCallback(editModeListener)
//
//            // listen for failure via change to AppState.saveRemoteError
//            AppState.errorData.remoteSaveError.addOnPropertyChangedCallback(remoteSaveErrorListener)
//        }
//    }
//
////    private val remoteTemplatesSheet = RemoteTemplatesSheet().apply {
////       templateSheetCallback = object : RemoteTemplatesSheet.RemoteTemplateSheetCallback{
////            @SuppressLint("LogNotTimber")
////            override fun onTemplateSelected(uid: String) {
////                Log.d("onTemplateSelected", "Template selected: $uid")
////                AppState.tempData.tempRemoteProfile = AppState.userData.remotes[uid] ?: RemoteProfile()
////                AppState.tempData.tempRemoteProfile.inEditMode.set(true)
////                state.viewPagerPosition = VP_FAV_REMOTE
////                onMyRemotesClicked()
////            }
////        }
////    }
//
//    private fun createRemote(forceShow : Boolean = false) {
//        FirestoreActions.getRemoteTemplates()
//        if (!state.isShowingCreateRemoteView || forceShow) {
//
//            // set up bottom sheet dialog
//            val createRemoteView = layoutInflater.inflate(R.layout.v_create_remote_from, null)
//            createRemoteDialog = BottomSheetDialog(this)
//            createRemoteFromBinding = DataBindingUtil.bind(createRemoteView)
//            createRemoteDialog?.setContentView(createRemoteView)
//            createRemoteDialog?.setOnDismissListener { state.isShowingCreateRemoteView = false }
//
//            // set up onClick listeners (device template, existing remote, blank layout)
//            createRemoteFromBinding?.tvFromScratch?.setOnClickListener { createBlankRemote() }
//            createRemoteFromBinding?.tvFromDeviceTemplate?.setOnClickListener { createFromDeviceTemplate() }
//            createRemoteFromBinding?.tvFromExistingRemote?.setOnClickListener { createFromExistingRemote() }
//
//            // Hide "From Existing Remote" if user doesn't have any
//            if (AppState.userData.remotes.size == 0)
//                createRemoteFromBinding?.tvFromExistingRemote?.visibility = View.GONE
//
//            createRemoteDialog?.show()
//            state.isShowingCreateRemoteView = true
//        }
//
//        //remoteTemplatesSheet.show(supportFragmentManager, "RemoteTemplateSheet")
//    }
//
//    /* -------- Create Remote Functions -------- */
//
//    private fun createFromExistingRemote() {
//        createRemoteDialog?.dismiss()
//
//        //createRemoteDialog = BottomSheetDialog(this)
//
//
//        debug_showComingSoonFlashbar()
//    }
//
//    private fun createFromDeviceTemplate() {
//        createRemoteDialog?.let {
//            Log.d("Test", "still have dialog link")
//            it.dismiss()
//        }
//        //createRemoteDialog?.dismiss()
//        debug_showComingSoonFlashbar()
//    }
//
//    private fun createBlankRemote() {
//        // dismiss "create from" dialog
//        createRemoteDialog?.dismiss()
//
//        // create blank remote in tempData
//        AppState.resetTempRemote()
//
//        // set remote to edit mode
//        AppState.tempData.tempRemoteProfile.inEditMode.set(true)
//
//        // Trigger update to fragment
//        onMyRemotesClicked(true)
//
//        // show keyboard
//        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
//
//        // select toolbar title
//        binding.toolbar.selectTitleText()
//    }
//
//
//
//    private fun createCommand() {
//        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    private fun createIrSignal() {
//        startActivityForResult(Intent(this, LSWalkThroughActivity::class.java), REQ_NEW_IR_SIG)
//    }
//
//    private fun setupNewHub() {
//        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    private fun addDevice() {
//        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    /* -------- DEBUG FUNCTIONS -------- */
//
//    fun debug_showComingSoonFlashbar() {
//        Flashbar.Builder(this)
//            .gravity(Flashbar.Gravity.BOTTOM)
//            .message("Feature coming soon!")
//            .showOverlay()
//            .enableSwipeToDismiss()
//            .dismissOnTapOutside()
//            .duration(Flashbar.DURATION_LONG)
//            .build()
//            .show()
//    }


//    companion object {
//        const val REQ_NEW_IR_SIG = 2
//        const val STATE = "MAIN_STATE"
//
//        // Viewpager Positions
//        const val VP_COMMANDS           = 0
//        const val VP_IR_SIGNALS         = 1
//        const val VP_DEVICES            = 0
//        const val VP_IRSMART_DEVICES    = 1
//        const val VP_FAV_REMOTE         = 0
//        const val VP_ALL_REMOTES        = 1
//
//        // Fragment Positions
//        const val FP_MY_REMOTES = 0
//        const val FP_MY_DEVICES = 1
//
//        enum class LayoutState(val value: Int) {
//            REMOTES_FAV(1),
//            REMOTES_FAV_EDITING(2),
//            REMOTES_ALL(3),
//            DEVICES_HUBS(4),
//            DEVICES_ALL(5)
//        }
//        fun layoutStateFromInt(stateAsInt: Int) = LayoutState.values().associateBy(LayoutState::value)[stateAsInt]
//    }
//
//    /*
//    ----------------------------------------------
//        Activity State
//    ----------------------------------------------
//    */
//
//    class State() : Parcelable {
//        var navPosition = FP_MY_REMOTES
//        var viewPagerPosition = 0
//        var adapterBaseID: Long = 0
//        var isShowingCreateRemoteView = false
//        var isListeningForSaveRemoteConfirmation = false
//
//        constructor(parcel: Parcel) : this() {
//            navPosition = parcel.readInt()
//            viewPagerPosition = parcel.readInt()
//            adapterBaseID = parcel.readLong()
//            isShowingCreateRemoteView = parcel.readByte() != 0.toByte()
//            isListeningForSaveRemoteConfirmation = parcel.readByte() != 0.toByte()
//        }
//
//        override fun writeToParcel(parcel: Parcel, flags: Int) {
//            parcel.writeInt(navPosition)
//            parcel.writeInt(viewPagerPosition)
//            parcel.writeLong(adapterBaseID)
//            parcel.writeByte(if (isShowingCreateRemoteView) 1 else 0)
//            parcel.writeByte(if (isListeningForSaveRemoteConfirmation) 1 else 0)
//        }
//
//        override fun describeContents(): Int {
//            return 0
//        }
//
//        companion object CREATOR : Parcelable.Creator<State> {
//            override fun createFromParcel(parcel: Parcel): State {
//                return State(parcel)
//            }
//
//            override fun newArray(size: Int): Array<State?> {
//                return arrayOfNulls(size)
//            }
//        }
//
//    }
}


