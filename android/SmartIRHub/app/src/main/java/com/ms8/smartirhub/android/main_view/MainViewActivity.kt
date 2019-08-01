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
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableMap
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.firebase.auth.FirebaseAuth
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.ms8.smartirhub.android.custom_views.bottom_sheets.BackWarningSheet
import com.ms8.smartirhub.android.custom_views.bottom_sheets.RemoteTemplatesSheet
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import com.ms8.smartirhub.android.databinding.ActivityMainViewBinding
import com.ms8.smartirhub.android.utils.extensions.getNavBarHeight
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.learn_signal.LSWalkThroughActivity
import com.ms8.smartirhub.android.main_view.fragments.*
import com.ms8.smartirhub.android.remote_control.RemoteFragment
import com.ms8.smartirhub.android.utils.extensions.findNavBarHeight
import android.util.TypedValue
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.remote_control.views.asymmetric_gridview.Utils


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
        // check remoteTemplateSheet state
            remoteTemplatesSheet.onBackPressed() -> {}
        // show exit warning before leaving
            !exitWarningSheet.isVisible -> { exitWarningSheet.show(supportFragmentManager, "ExitWarningSheet") }
        // proceed with normal onBackPressed
            else -> { super.onBackPressed() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE, state.apply { adapterBaseID = pagerAdapter.getBaseItemId() })
    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        Log.d("TEST", "Selected item ID = ${item.itemId}" +
//                "\n\tR.id.navigation_remote = ${R.id.navigation_main_remote}" +
//                "\n\tR.id.navigation_commands = ${R.id.navigation_commands}" +
//                "\n\tR.id.navigation_devices = ${R.id.navigation_devices}")
//
//        // Update state
//        state.navPosition = item.itemId
//
//        // Reset viewpager position
//        state.viewPagerPosition = 0
//
//        // Update FAB based on selected item
//        setupFab()
//
//        // Show proper views based on selected item
//        setupInnerView()
//
//        return true
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_view)

        // setup exit warning sheet
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

        // build drawer header layout
        val header = AccountHeaderBuilder()
            .withActivity(this)
            .withHeaderBackground(R.drawable.side_nav_bar)
            .addProfiles(
                ProfileDrawerItem()
                    .withName(AppState.userData.user.username.get())
                    .withEmail(FirebaseAuth.getInstance().currentUser?.email)
            )
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

        // get state
        state = savedInstanceState?.get(STATE) as State? ?: State()

        // setup fab
        setupFab()

        // setup fragment pager adapter
        pagerAdapter = MainViewAdapter(supportFragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, state.navPosition, state.adapterBaseID)
            .apply {
                addFragment(MyCommandsFragment(), MainViewAdapter.Companion.ViewPagerList.COMMANDS)
                addFragment(MyIrSignalsFragment(), MainViewAdapter.Companion.ViewPagerList.COMMANDS)
                addFragment(remoteFragment, MainViewAdapter.Companion.ViewPagerList.REMOTES)
                addFragment(MyRemotesFragment(), MainViewAdapter.Companion.ViewPagerList.REMOTES)
                addFragment(MyDevicesFragment(), MainViewAdapter.Companion.ViewPagerList.DEVICES)
                addFragment(MyIRSmartHubsFragment(), MainViewAdapter.Companion.ViewPagerList.DEVICES)
            }

        // setup bindings
        binding
            .apply {
                frameLayout.adapter = pagerAdapter
                frameLayout.addOnPageChangeListener(pageChangeCallback)

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

        // set currently shown view
        when (state.navPosition) {
            FP_MY_DEVICES -> onMyDevicesClicked(true)
            FP_MY_REMOTES -> onMyRemotesClicked(true)
        }

        createMockData()
    }

    private fun createMockData() {
        AppState.tempData.tempRemoteProfile.buttons
            .apply {
                for (i in 0 until 50) {
                    add(
                        RemoteProfile.Button()
                            .apply {
                                name = "B $i"
                                when (i) {
                                    0,1  -> {
                                        columnSpan = 2
                                    }
                                    2 -> {
                                        rowSpan = 2
                                        style = RemoteProfile.Button.STYLE_BTN_INCREMENTER_VERTICAL
                                        properties[0].bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_ROUND_RECT_TOP
                                        properties[0].marginTop = 16
                                        properties[0].marginStart = 16
                                        properties[0].marginEnd = 16
                                        properties[0].marginBottom = 0
                                        properties[0].image = RemoteProfile.Button.IMG_ADD

                                        properties.add(
                                            RemoteProfile.Button.Properties()
                                                .apply {
                                                    bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_ROUND_RECT_BOTTOM
                                                    marginTop = 0
                                                    marginStart = 16
                                                    marginBottom = 16
                                                    marginEnd = 16
                                                    image = RemoteProfile.Button.IMG_SUBTRACT
                                                })

                                        name = "VOL"
                                    }
                                    4 -> {
                                        rowSpan = 2
                                        style = RemoteProfile.Button.STYLE_BTN_INCREMENTER_VERTICAL
                                        properties[0].bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_ROUND_RECT_TOP
                                        properties[0].marginTop = 16
                                        properties[0].marginStart = 16
                                        properties[0].marginEnd = 16
                                        properties[0].marginBottom = 0
                                        properties[0].image = RemoteProfile.Button.IMG_ADD

                                        properties.add(
                                            RemoteProfile.Button.Properties()
                                                .apply {
                                                    bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_ROUND_RECT_BOTTOM
                                                    marginTop = 0
                                                    marginStart = 16
                                                    marginBottom = 16
                                                    marginEnd = 16
                                                    image = RemoteProfile.Button.IMG_SUBTRACT
                                                })

                                        name = "CH"
                                    }
                                    3 -> {
                                        rowSpan = 2
                                        columnSpan = 2
                                        style = RemoteProfile.Button.STYLE_BTN_RADIAL_W_CENTER

                                        // add topButton Properties
                                        properties[0].bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_NONE
                                        properties[0].marginTop = 16
                                        properties[0].marginStart = 0
                                        properties[0].marginEnd = 0
                                        properties[0].marginBottom = 0
                                        properties[0].image = RemoteProfile.Button.IMG_RADIAL_UP

                                        // add endButton Properties
                                        properties.add(
                                            RemoteProfile.Button.Properties()
                                                .apply {
                                                    bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_NONE
                                                    marginTop = 0
                                                    marginStart = 0
                                                    marginEnd = 16
                                                    marginBottom = 0
                                                    image = RemoteProfile.Button.IMG_RADIAL_RIGHT
                                                })
                                        // add bottomButton Properties
                                        properties.add(
                                            RemoteProfile.Button.Properties()
                                                .apply {
                                                    bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_NONE
                                                    marginTop = 0
                                                    marginStart = 0
                                                    marginEnd = 0
                                                    marginBottom = 16
                                                    image = RemoteProfile.Button.IMG_RADIAL_DOWN
                                                })
                                        // add startButton Properties
                                        properties.add(
                                            RemoteProfile.Button.Properties()
                                                .apply {
                                                    bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_NONE
                                                    marginTop = 0
                                                    marginStart = 16
                                                    marginEnd = 0
                                                    marginBottom = 0
                                                    image = RemoteProfile.Button.IMG_RADIAL_LEFT
                                                })
                                        // add centerButton Properties
                                        properties.add(
                                            RemoteProfile.Button.Properties()
                                                .apply {
                                                    bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_CIRCLE
                                                    marginTop = 0
                                                    marginStart = 0
                                                    marginEnd = 0
                                                    marginBottom = 0
                                                })

                                        name = "OK"
                                    }
                                }
                            })
                }
            }
    }

    override fun onPause() {
        super.onPause()
        AppState.userData.remotes.removeOnMapChangedCallback(remoteProfilesListener)
    }

    override fun onResume() {
        super.onResume()
        AppState.userData.remotes.addOnMapChangedCallback(remoteProfilesListener)
    }


/*
 ----------------------------------------------
    Inner Layout Functions
 ----------------------------------------------
 */

    private fun onMyRemotesClicked(forceUpdate: Boolean = false) {
        // do nothing if 'my remotes' is already shown
        if (state.navPosition == FP_MY_REMOTES && !forceUpdate)
            return

        // update nav position
        state.navPosition = FP_MY_REMOTES

        // set toolbar title
        when (state.viewPagerPosition) {
            VP_FAV_REMOTE -> {
                val title = if (AppState.tempData.tempRemoteProfile.name == "") getString(R.string.new_remote) else AppState.tempData.tempRemoteProfile.name
                binding.toolbar.title = title
            }
            VP_ALL_REMOTES -> {
                binding.toolbar.title = getString(R.string.title_remotes)
            }
        }

        // set nav buttons selected
        binding.btnMyRemotes.isSelected = true
        binding.btnMyDevices.isSelected = false

        // set fab label and function
        setupFab()

        // update pagerAdapter and viewPager
        pagerAdapter.setNavPosition(state.navPosition)
        binding.frameLayout.setCurrentItem(state.viewPagerPosition, false)
    }

    private fun onMyDevicesClicked(forceUpdate: Boolean = false) {
        // do nothing if 'my devices' is already shown
        if (state.navPosition == FP_MY_DEVICES && !forceUpdate)
            return

        // update nav position
        state.navPosition = FP_MY_DEVICES

        // set toolbar title
        when (state.viewPagerPosition) {
            VP_DEVICES -> {
                binding.toolbar.title = getString(R.string.title_my_devices)
            }
            VP_IRSMART_DEVICES -> {
                binding.toolbar.title = getString(R.string.title_my_ir_hubs)
            }
        }

        // set nav buttons selected
        binding.btnMyRemotes.isSelected = false
        binding.btnMyDevices.isSelected = true

        // set fab label and function
        setupFab()

        // update pagerAdapter and viewPager
        pagerAdapter.setNavPosition(state.navPosition)
        binding.frameLayout.setCurrentItem(state.viewPagerPosition, false)
    }

    private val pageChangeCallback = object: ViewPager.OnPageChangeListener {
        override fun onPageScrollStateChanged(state: Int) {}

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

        override fun onPageSelected(position: Int) {
            state.viewPagerPosition = position
            setupFab()
        }
    }

    private fun setupFab() {
        when (state.navPosition) {
            // My Remotes
            FP_MY_REMOTES -> {
                //binding.fab.labelText = getString(R.string.create_remote)
                binding.fab.setOnClickListener { createRemote() }
            }
            // My Devices
            FP_MY_DEVICES -> {
                when (state.viewPagerPosition) {
                // Add Predefined Devices
                    VP_DEVICES -> {
                        //binding.fab.labelText = getString(R.string.add_ir_device)
                        binding.fab.setOnClickListener { addDevice() }
                    }
                // Set Up IRSmartHub
                    VP_IRSMART_DEVICES -> {
                        //binding.fab.labelText = getString(R.string.setup_new_hub)
                        binding.fab.setOnClickListener { setupNewHub() }
                    }
                }
            }
        }
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
    Navigation Functions
 ----------------------------------------------
 */

    private fun onDrawerItemClicked(view: View?, position: Int, drawerItem: IDrawerItem<*>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
                AppState.tempData.tempRemoteProfile = AppState.userData.remotes[uid] ?: RemoteProfile()
                AppState.tempData.tempRemoteProfile.inEditMode.set(true)
                state.viewPagerPosition = VP_FAV_REMOTE
                onMyRemotesClicked()

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

        // Fragment Positions
        const val FP_MY_REMOTES = 0
        const val FP_MY_DEVICES = 1
    }

    /*
    ----------------------------------------------
        Activity State
    ----------------------------------------------
    */

    class State() : Parcelable {
        var navPosition = FP_MY_REMOTES
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


