package com.ms8.smartirhub.android

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableMap
import com.google.firebase.auth.FirebaseAuth
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.ms8.smartirhub.android.data.RemoteProfile
import com.ms8.smartirhub.android.database.LocalData
import com.ms8.smartirhub.android.databinding.ActivityMainViewBinding
import it.sephiroth.android.library.bottomnavigation.BottomNavigation
import kotlinx.android.synthetic.main.activity_main_view.*
import java.io.Serializable

class MainViewActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainViewBinding
    private lateinit var drawer : Drawer
    private lateinit var state : State

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
        outState.putSerializable(STATE, state)
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_view)
        binding.navView.menuItemSelectionListener = navListener
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

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

        binding.navView.setSelectedIndex(state.navPosition, true)

        /* --- Set Fab Layout --- */
        setupFab()

        /* --- Set Inner View Layout --- */
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

    private fun setupInnerView() {
        when (state.navPosition) {
        // My Remotes
            NAV_REMOTES -> {
                //TODO Implement remotes page
                /*
                    This page contains two panels:
                        1. The user's favorite remote
                        2. A list of all other remotes the user has access to
                 */
                when (state.viewPagerPosition) {
                    VP_FAV_REMOTE -> {
                        binding.toolbar.title = getString(R.string.favorite_remote) //todo replace this with actual name of remote
                    }
                    VP_ALL_REMOTES -> {
                        binding.toolbar.title = getString(R.string.title_remotes)
                    }
                }
            }
        // My Commands
            NAV_COMMANDS -> {
                //TODO Implement commands page
                /*
                    This page contains two panels:
                        1. A list of user-defined commands (with favorite commands stickied to the
                            top).
                        2. A list of user-defined IR signals.
                 */
                when (state.viewPagerPosition) {
                    VP_COMMANDS -> {
                        binding.toolbar.title = getString(R.string.title_my_commands)
                    }
                    VP_SIGNALS -> {
                        binding.toolbar.title = getString(R.string.title_programmed_signals)
                    }
                }
            }
        // My Devices
            NAV_DEVICES -> {
                //TODO Implement devices page
                /*
                    This page contains multiple things:
                        1. A list of IR devices that the user has added. These are pre-defined devices
                            with a preset remote profile to accompany them.
                        2. A list of IRSmartHub devices. From here, users can change the name of
                            devices, set up new ones, etc.
                 */
                when (state.viewPagerPosition) {
                    VP_DEVICES -> {
                        binding.toolbar.title = getString(R.string.title_my_devices)
                    }
                    VP_IRSMART_DEVICES -> {
                        binding.toolbar.title = getString(R.string.title_my_ir_hubs)
                    }
                }
            }
        }
    }

    private fun setupFab() {
        when (state.navPosition) {
            // My Remotes
            NAV_REMOTES -> {
                binding.fabText.text = getString(R.string.create_remote)
                binding.fab.setOnClickListener { createRemote() }
            }
            // My Commands
            NAV_COMMANDS -> {
                when (state.viewPagerPosition) {
                // Create New Command
                    VP_COMMANDS -> {
                        binding.fabText.text = getString(R.string.create_command)
                        binding.fab.setOnClickListener { createCommand() }
                    }
                // Create New IR Signal
                    VP_SIGNALS -> {
                        binding.fabText.text = getString(R.string.create_ir_signal)
                        binding.fab.setOnClickListener { createIrSignal() }
                    }
                }
            }
            // My Devices
            NAV_DEVICES -> {
                when (state.viewPagerPosition) {
                // Add Predefined Devices
                    VP_DEVICES -> {
                        binding.fabText.text = getString(R.string.add_ir_device)
                        binding.fab.setOnClickListener { addDevice() }
                    }
                // Set Up IRSmartHub
                    VP_IRSMART_DEVICES -> {
                        binding.fabText.text = getString(R.string.setup_new_hub)
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


    private val navListener = object : BottomNavigation.OnMenuItemSelectionListener {
        override fun onMenuItemReselect(itemId: Int, position: Int, fromUser: Boolean) {}

        override fun onMenuItemSelect(itemId: Int, position: Int, fromUser: Boolean) {
            // Update state
            state.navPosition = position

            // Reset viewpager position
            state.viewPagerPosition = 0

            // Update FAB based on selected item
            setupFab()

            // Show proper views based on selected item
            setupInnerView()
        }
    }

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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun setupNewHub() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun addDevice() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        const val REQ_NEW_IR_SIG = 2
        const val STATE = "MAIN_STATE"

        // Nav Item Positions
        const val NAV_REMOTES   = 0
        const val NAV_COMMANDS  = 1
        const val NAV_DEVICES   = 2

        // Viewpager Positions
        const val VP_COMMANDS           = 0
        const val VP_SIGNALS            = 1
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

    inner class State : Serializable {
        var navPosition = 0
        var viewPagerPosition = 0

        override fun toString(): String {
            return "navPosition = $navPosition, viewPagerPosition = $viewPagerPosition"
        }
    }


}
//startActivityForResult(Intent(this, LSWalkthroughActivity::class.java), REQ_NEW_IR_SIG)