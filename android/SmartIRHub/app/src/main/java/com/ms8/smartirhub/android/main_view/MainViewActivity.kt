package com.ms8.smartirhub.android.main_view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableMap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.firebase.auth.FirebaseAuth
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.custom_views.bottom_sheets.BackWarningSheet
import com.ms8.smartirhub.android.custom_views.bottom_sheets.RemoteTemplatesSheet
import com.ms8.smartirhub.android.models.firestore.RemoteProfile
import com.ms8.smartirhub.android.database.LocalData
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.databinding.ActivityMainViewBinding
import com.ms8.smartirhub.android.utils.exts.getNavBarHeight
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.learn_signal.LSWalkThroughActivity
import com.ms8.smartirhub.android.main_view.fragments.*

class MainViewActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainViewBinding
    private lateinit var drawer : Drawer
    private lateinit var state : State
    private lateinit var pagerAdapter: MainViewAdapter

    private val remoteFragment = RemoteFragment()
    private val exitWarningSheet = BackWarningSheet()

/*
-----------------------------------------------
    Database Listeners
-----------------------------------------------
*/

    private val remoteProfilesListener: ObservableMap.OnMapChangedCallback<out ObservableMap<String, RemoteProfile>, String, RemoteProfile>? = object :
        ObservableMap.OnMapChangedCallback<ObservableMap<String, RemoteProfile>, String, RemoteProfile>() {
        override fun onMapChanged(sender: ObservableMap<String, RemoteProfile>?, key: String?) {
            //todo ?
        }
    }

/*
----------------------------------------------
    Overridden Functions
----------------------------------------------
*/

    override fun onBackPressed() {
        when {
        // Check remoteTemplateSheet state
            remoteTemplatesSheet.onBackPressed() -> {}
        // Show exit warning before leaving
            !exitWarningSheet.isVisible -> { exitWarningSheet.show(supportFragmentManager, "ExitWarningSheet") }
        // Proceed with normal onBackPressed
            else -> { super.onBackPressed() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE, state.apply { adapterBaseID = pagerAdapter.getBaseItemId() })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_view)
        //binding.navView.menuItemSelectionListener = navListener

        /* --- Set up exit sheet -- */
        exitWarningSheet
            .apply {
                titleStr = this@MainViewActivity.getString(R.string.exit_app_title)
                descStr = this@MainViewActivity.getString(R.string.exit_app_desc)
                btnNegStr = this@MainViewActivity.getString(android.R.string.cancel)
                btnPosStr = this@MainViewActivity.getString(R.string.leave)
                callback = object : BackWarningSheet.BackWaringSheetCallback {
                    override fun btnNegAction() {}

                    override fun btnPosAction() { finishAndRemoveTask() }
                }
            }

        /* --- Build drawer layout --- */
        val header = AccountHeaderBuilder()
            .withActivity(this)
            .withHeaderBackground(R.drawable.side_nav_bar)
            .addProfiles(
                ProfileDrawerItem()
                    .withName(LocalData.user?.username)
                    .withEmail(FirebaseAuth.getInstance().currentUser?.email)
            )
            .build()

        /* -- Account for nav/status bar height --*/
        val rotation = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            try {
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                binding.navView.setPadding(0, 0, 0, getNavBarHeight())
            } catch (e :Exception) {}
        }

        /* --- Build Side Drawer --- */
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

        /* --- Set Up State ---*/
        state = savedInstanceState?.get(STATE) as State? ?: State()

        //binding.navView.setSelectedIndex(state.navPosition, true)

        /* --- Set Fab Layout --- */
        setupFab()

        /* --- Set Inner View Layout --- */
        pagerAdapter = MainViewAdapter(supportFragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, state.navPosition, state.adapterBaseID)
            .apply {
                addFragment(MyCommandsFragment(), MainViewAdapter.Companion.ViewPagerList.COMMANDS)
                addFragment(MyIrSignalsFragment(), MainViewAdapter.Companion.ViewPagerList.COMMANDS)
                addFragment(remoteFragment, MainViewAdapter.Companion.ViewPagerList.REMOTES)
                addFragment(MyRemotesFragment(), MainViewAdapter.Companion.ViewPagerList.REMOTES)
                addFragment(MyDevicesFragment(), MainViewAdapter.Companion.ViewPagerList.DEVICES)
                addFragment(MyIRSmartHubsFragment(), MainViewAdapter.Companion.ViewPagerList.DEVICES)
            }


        /* -- Set bindings -- */
        binding
            .apply {
                frameLayout.adapter = pagerAdapter
                frameLayout.addOnPageChangeListener(pageChangeCallback)

                navView.setOnNavigationItemSelectedListener { item -> onItemSelected(item) }
                navView.setOnNavigationItemReselectedListener { run {  } }
                navView.selectedItemId = state.navPosition
            }

        setupInnerView()
    }

    override fun onPause() {
        super.onPause()
        LocalData.remoteProfiles.removeOnMapChangedCallback(remoteProfilesListener)
    }

    override fun onResume() {
        super.onResume()
        LocalData.remoteProfiles.addOnMapChangedCallback(remoteProfilesListener)
    }


/*
 ----------------------------------------------
    Inner Layout Functions
 ----------------------------------------------
 */

    private val pageChangeCallback = object: ViewPager.OnPageChangeListener {
        override fun onPageScrollStateChanged(state: Int) {}

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

        override fun onPageSelected(position: Int) {
            state.viewPagerPosition = position
            setupFab()
        }
    }

    @SuppressLint("LogNotTimber")
    private fun setupInnerView() {
        when (state.navPosition) {
        // My Remotes
            R.id.navigation_main_remote -> {
                //TODO Implement remotes page
                /*
                    This page contains two panels:
                        1. The user's favorite remote
                        2. A list of all other remotes the user has access to
                 */
                //pagerAdapter.setFragments(remotesFragments)
                when (state.viewPagerPosition) {
                    VP_FAV_REMOTE -> {
                        val title = if (TempData.tempRemoteProfile.name == "") getString(R.string.new_remote) else TempData.tempRemoteProfile.name
                        binding.toolbar.title = title
                    }
                    VP_ALL_REMOTES -> {
                        binding.toolbar.title = getString(R.string.title_remotes)
                    }
                }
            }
        // My Commands
            R.id.navigation_commands -> {
                //TODO Implement commands page
                /*
                    This page contains two panels:
                        1. A list of user-defined commands (with favorite commands stickied to the
                            top).
                        2. A list of user-programed IR signals.
                 */
                //pagerAdapter.setFragments(commandsFragments)
                when (state.viewPagerPosition) {
                    VP_COMMANDS -> {
                        binding.toolbar.title = getString(R.string.title_my_commands)
                    }
                    VP_IR_SIGNALS -> {
                        binding.toolbar.title = getString(R.string.title_programmed_signals)
                    }
                }
            }
        // My Devices
            R.id.navigation_devices -> {
                //TODO Implement devices page
                /*
                    This page contains multiple things:
                        1. A list of IR devices that the user has added. These are pre-defined devices
                            with a preset remote profile to accompany them.
                        2. A list of IRSmartHub devices. From here, users can change the name of
                            devices, set up new ones, etc.
                 */
                //pagerAdapter.setFragments(devicesFragments)
                when (state.viewPagerPosition) {
                    VP_DEVICES -> {
                        binding.toolbar.title = getString(R.string.title_my_devices)
                    }
                    VP_IRSMART_DEVICES -> {
                        binding.toolbar.title = getString(R.string.title_my_ir_hubs)
                    }
                }
            }
            else -> { Log.e("MainViewActivity", "Unknown nav id: ${state.navPosition}") }
        }
        pagerAdapter.setNavPosition(state.navPosition)
        binding.frameLayout.setCurrentItem(state.viewPagerPosition, false)
    }

    private fun setupFab() {
        when (state.navPosition) {
            // My Remotes
            R.id.navigation_main_remote -> {
                binding.fab.animateNewText(getString(R.string.create_remote))
                binding.fab.setOnClickListener { createRemote() }
            }
            // My Commands
            R.id.navigation_commands -> {
                when (state.viewPagerPosition) {
                // Create New Command
                    VP_COMMANDS -> {
                        binding.fab.animateNewText(getString(R.string.create_command))
                        binding.fab.setOnClickListener { createCommand() }
                    }
                    VP_IR_SIGNALS -> {
                        binding.fab.text = getString(R.string.create_ir_signal)
                        binding.fab.setOnClickListener { createIrSignal() }
                    }
                }
            }
            // My Devices
            R.id.navigation_devices -> {
                when (state.viewPagerPosition) {
                // Add Predefined Devices
                    VP_DEVICES -> {
                        binding.fab.animateNewText(getString(R.string.add_ir_device))
                        binding.fab.setOnClickListener { addDevice() }
                    }
                // Set Up IRSmartHub
                    VP_IRSMART_DEVICES -> {
                        binding.fab.animateNewText(getString(R.string.setup_new_hub))
                        binding.fab.setOnClickListener { setupNewHub() }
                    }
                }
            }
        }
    }

/*
 ----------------------------------------------
    Navigation Functions
 ----------------------------------------------
 */

    private fun onDrawerItemClicked(view: View?, position: Int, drawerItem: IDrawerItem<*>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    private fun onItemSelected(menuItem: MenuItem) : Boolean {
        // Update state
        state.navPosition = menuItem.itemId

        // Reset viewpager position
        state.viewPagerPosition = 0

        // Update FAB based on selected item
        setupFab()

        // Show proper views based on selected item
        setupInnerView()

        return true
    }

/*
----------------------------------------------
    OnClick Functions
----------------------------------------------
*/

    private val remoteTemplatesSheet = RemoteTemplatesSheet().apply {
       templateSheetCallback = object : RemoteTemplatesSheet.RemoteTemplateSheetCallback{
            @SuppressLint("LogNotTimber")
            override fun onTemplateSelected(uid: String) {
                Log.d("onTemplateSelected", "Template selected: $uid")
                TempData.tempRemoteProfile = LocalData.remoteProfiles[uid] ?: RemoteProfile()
                TempData.tempRemoteProfile.inEditMode.set(true)
                setupInnerView()
            }
        }
    }

    private fun createRemote() {
        FirestoreActions.getRemoteTemplates()
        remoteTemplatesSheet.show(supportFragmentManager, "RemoteTemplateSheet")
    }

    private fun createCommand() {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun createIrSignal() {
        startActivityForResult(Intent(this, LSWalkThroughActivity::class.java), REQ_NEW_IR_SIG)
    }

    private fun setupNewHub() {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun addDevice() {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        const val REQ_NEW_IR_SIG = 2
        const val STATE = "MAIN_STATE"

        // Viewpager Positions
        const val VP_COMMANDS           = 0
        const val VP_IR_SIGNALS         = 1
        const val VP_DEVICES            = 0
        const val VP_IRSMART_DEVICES    = 1
        const val VP_FAV_REMOTE         = 0
        const val VP_ALL_REMOTES        = 1
    }

    /*
    ----------------------------------------------
        Activity State
    ----------------------------------------------
    */

    class State() : Parcelable {
        var navPosition = R.id.navigation_main_remote
        var viewPagerPosition = 0
        var adapterBaseID: Long = 0

        constructor(parcel: Parcel) : this() {
            navPosition = parcel.readInt()
            viewPagerPosition = parcel.readInt()
            adapterBaseID = parcel.readLong()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(navPosition)
            parcel.writeInt(viewPagerPosition)
            parcel.writeLong(adapterBaseID)
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

