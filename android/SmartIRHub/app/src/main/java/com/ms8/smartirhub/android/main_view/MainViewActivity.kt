package com.ms8.smartirhub.android.main_view

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableMap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.data.RemoteProfile
import com.ms8.smartirhub.android.database.LocalData
import com.ms8.smartirhub.android.databinding.ActivityMainViewBinding
import com.ms8.smartirhub.android.learn_signal.LSWalkthroughActivity
import com.ms8.smartirhub.android.main_view.fragments.*
import it.sephiroth.android.library.bottomnavigation.BottomNavigation
import kotlinx.android.synthetic.main.activity_main_view.*

class MainViewActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainViewBinding
    private lateinit var drawer : Drawer
    private lateinit var state : State
    private lateinit var pagerAdapter: MainViewAdapter

    private val commandsFragments: MutableList<Fragment> = arrayListOf(MyCommandsFragment(), MyIrSignalsFragment())
    private val devicesFragments: MutableList<Fragment> = arrayListOf(MyDevicesFragment(), MyIRSmartHubsFragment())
    private val remotesFragments: MutableList<Fragment> = arrayListOf(RemoteFragment(), MyRemotesFragment())

    /*
    -----------------------------------------------
        Database Listeners
    -----------------------------------------------
    */

    private val remoteProfilesListener: ObservableMap.OnMapChangedCallback<out ObservableMap<String, RemoteProfile>, String, RemoteProfile>? = object :
        ObservableMap.OnMapChangedCallback<ObservableMap<String, RemoteProfile>, String, RemoteProfile>() {
        override fun onMapChanged(sender: ObservableMap<String, RemoteProfile>?, key: String?) {

        }
    }

    /*
    ----------------------------------------------
        Overridden Functions
    ----------------------------------------------
    */

    override fun onSaveInstanceState(outState: Bundle) {
        state.adapterBaseID = pagerAdapter.getBaseItemId()
        outState.putParcelable(STATE, state)
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_view)
        //binding.navView.menuItemSelectionListener = navListener

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
        pagerAdapter.addFragment(MyCommandsFragment(), MainViewAdapter.Companion.ViewPagerList.COMMANDS)
        pagerAdapter.addFragment(MyIrSignalsFragment(), MainViewAdapter.Companion.ViewPagerList.COMMANDS)
        pagerAdapter.addFragment(RemoteFragment(), MainViewAdapter.Companion.ViewPagerList.REMOTES)
        pagerAdapter.addFragment(MyRemotesFragment(), MainViewAdapter.Companion.ViewPagerList.REMOTES)
        pagerAdapter.addFragment(MyDevicesFragment(), MainViewAdapter.Companion.ViewPagerList.DEVICES)
        pagerAdapter.addFragment(MyIRSmartHubsFragment(), MainViewAdapter.Companion.ViewPagerList.DEVICES)

        binding.frameLayout.adapter = pagerAdapter
        binding.frameLayout.addOnPageChangeListener(pageChangeCallback)

        binding.navView.setOnNavigationItemSelectedListener { item -> onItemSelected(item) }
        binding.navView.setOnNavigationItemReselectedListener { run {  } }
        binding.navView.selectedItemId = state.navPosition
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
            Log.d("MainViewActivity", "page changed to $position")
            state.viewPagerPosition = position
            setupFab()
        }
    }

    private fun setupInnerView() {
        Log.d("MainViewActivity", "Setting up with ${state.navPosition}")
        when (state.navPosition) {
        // My Remotes
            R.id.navigation_main_remote -> {
                Log.d("MainViewActivity", "Here! viewPagerPosition = ${state.viewPagerPosition}")
                //TODO Implement remotes page
                /*
                    This page contains two panels:
                        1. The user's favorite remote
                        2. A list of all other remotes the user has access to
                 */
                //pagerAdapter.setFragments(remotesFragments)
                when (state.viewPagerPosition) {
                    VP_FAV_REMOTE -> {
                        Log.d("MainViewActivity", "Here!!!!!!! viewPagerPosition = ${state.viewPagerPosition}")
                        binding.toolbar.title = getString(R.string.favorite_remote) //todo replace this with actual name of remote
                    }
                    VP_ALL_REMOTES -> {
                        Log.d("MainViewActivity", "Here11111111 viewPagerPosition = ${state.viewPagerPosition}")
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
            else -> {
                Log.e("MainViewActivity", "Unknown nav id: ${state.navPosition}")
            }
        }
        //pagerAdapter.updateNavPosition(state.navPosition)
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

//    private val navListener = object : BottomNavigation.OnMenuItemSelectionListener {
//        override fun onMenuItemReselect(itemId: Int, position: Int, fromUser: Boolean) {}
//
//        override fun onMenuItemSelect(itemId: Int, position: Int, fromUser: Boolean) {
//            // Update state
//            state.navPosition = position
//
//            // Reset viewpager position
//            state.viewPagerPosition = 0
//
//            // Update FAB based on selected item
//            setupFab()
//
//            // Show proper views based on selected item
//            setupInnerView()
//        }
//    }

    /*
    ----------------------------------------------
        OnClick Functions
    ----------------------------------------------
    */

    private fun createRemote() {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun createCommand() {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun createIrSignal() {
        startActivityForResult(Intent(this, LSWalkthroughActivity::class.java), REQ_NEW_IR_SIG)
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

}

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
