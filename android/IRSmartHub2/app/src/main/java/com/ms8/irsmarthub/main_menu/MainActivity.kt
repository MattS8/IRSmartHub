package com.ms8.irsmarthub.main_menu

import android.graphics.Paint
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.fragment.app.FragmentPagerAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.ms8.irsmarthub.R
import com.ms8.irsmarthub.database.AppState.tempData
import com.ms8.irsmarthub.databinding.AMainViewBinding
import com.ms8.irsmarthub.main_menu.MainMenuAdapter.Companion.LayoutState
import com.ms8.irsmarthub.main_menu.fragments.MyDevicesFragment
import com.ms8.irsmarthub.main_menu.fragments.MyRemotesFragment
import com.ms8.irsmarthub.main_menu.fragments.RemoteFragment
import com.ms8.irsmarthub.remote_control.remote.views.asymmetric_gridview.Utils
import com.ms8.irsmarthub.utils.findNavBarHeight

class MainActivity : AppCompatActivity(), ViewPager.OnPageChangeListener, View.OnClickListener {
/*
----------------------------------------------
    View Binding
----------------------------------------------
*/
    private lateinit var binding: AMainViewBinding
    private fun setupBinding() {
        val inEditMode = tempData.tempRemote.inEditMode.get() ?: false

        binding.frameLayout.apply {
            adapter = pagerAdapter
            addOnPageChangeListener(this@MainActivity)
        }
        binding.navView.apply {
            layoutParams = CoordinatorLayout.LayoutParams(layoutParams)
                .apply {
                    val tv = TypedValue()
                    val navBarHeight = findNavBarHeight()
                    if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true) && navBarHeight > 0) {
                        height = TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics) + findNavBarHeight()
                    }
                    gravity = Gravity.BOTTOM
                }
            setPadding(paddingLeft, paddingTop, paddingRight, findNavBarHeight())
        }
        binding.btnMyRemotes.apply {
            setOnClickListener(this@MainActivity)
            paintFlags = when (pagerAdapter.getLayoutState()) {
                LayoutState.REMOTES_FAV_EDITING,
                LayoutState.REMOTES_FAV,
                LayoutState.REMOTES_ALL -> paintFlags or Paint.UNDERLINE_TEXT_FLAG
                else -> 0
            }
        }
        binding.btnMyDevices.apply {
            setOnClickListener(this@MainActivity)
            paintFlags = when (pagerAdapter.getLayoutState()) {
                LayoutState.DEVICES_HUBS,
                LayoutState.DEVICES_ALL -> Paint.UNDERLINE_TEXT_FLAG
                else -> 0
            }
        }
        binding.toolbar.layoutState = pagerAdapter.getLayoutState()
        binding.toolbar.layoutState = pagerAdapter.getLayoutState()
        binding.fab.layoutState = pagerAdapter.getLayoutState()
    }

    /*
    ----------------------------------------------
        Pager Adapter
    ----------------------------------------------
    */
    private val remoteFragment = RemoteFragment()
    private lateinit var pagerAdapter: MainMenuAdapter
    private fun setupPagerAdapter(pagerState: MainMenuAdapter.Companion.State) {
        pagerAdapter = MainMenuAdapter(
            supportFragmentManager,
            FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,
            pagerState)
            .apply {
                addFragment(remoteFragment, MainMenuAdapter.Companion.ViewPagerList.REMOTES)
                addFragment(MyRemotesFragment(), MainMenuAdapter.Companion.ViewPagerList.REMOTES)
                addFragment(MyDevicesFragment(), MainMenuAdapter.Companion.ViewPagerList.DEVICES)
            }
    }

    override fun onPageScrollStateChanged(state: Int) {}

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {
        val newLayoutState = getPagerLayoutState(pagerAdapter.getLayoutState(), position)

        pagerAdapter.setLayoutState(newLayoutState, false)

        binding.toolbar.layoutState = newLayoutState
        binding.fab.layoutState = newLayoutState
        showUiElements(ElementParts.BOTH)
    }

    private fun getPagerLayoutState(layoutState: LayoutState, position: Int): LayoutState {
        val inEditMode = tempData.tempRemote.inEditMode.get() ?: false
        return when (layoutState) {
            LayoutState.REMOTES_FAV,
            LayoutState.REMOTES_FAV_EDITING,
            LayoutState.REMOTES_ALL ->
                when {
                    position == 1 -> LayoutState.REMOTES_ALL
                    inEditMode -> LayoutState.REMOTES_FAV_EDITING
                    else -> LayoutState.REMOTES_FAV
                }
            LayoutState.DEVICES_HUBS,
            LayoutState.DEVICES_ALL ->
                if (position == 1) LayoutState.DEVICES_HUBS
                else LayoutState.DEVICES_ALL
        }
    }

/*
----------------------------------------------
    Overridden Functions
----------------------------------------------
*/
    override fun onCreate(savedInstanceState: Bundle?)
{
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.a_main_view)

        val state = savedInstanceState?.get(STATE) as State? ?: State()

        setupPagerAdapter(state.pagerState)
        setupBinding()
    }

    override fun onResume() {
        super.onResume()
        tempData.tempRemote.inEditMode.addOnPropertyChangedCallback(editModeChangedListener)
        tempData.tempRemote.uid.addOnPropertyChangedCallback(remoteChangeListener)
        tempData.tempRemote.name.addOnPropertyChangedCallback(remoteChangeListener)
    }

    override fun onPause() {
        super.onPause()
        tempData.tempRemote.inEditMode.removeOnPropertyChangedCallback(editModeChangedListener)
        tempData.tempRemote.uid.removeOnPropertyChangedCallback(remoteChangeListener)
        tempData.tempRemote.name.removeOnPropertyChangedCallback(remoteChangeListener)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE, State(
            pagerAdapter.getState()
        ))
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnMyDevices -> onMyDevicesClicked()
            R.id.btnMyRemotes -> onMyRemotesClicked()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("TEST", "item ${item.itemId} was selected")
        return when (item.itemId) {

            else -> super.onOptionsItemSelected(item)
        }
    }

/*
----------------------------------------------
    UI Functions
----------------------------------------------
*/
    // Private
    private fun onMyDevicesClicked()
    {
        // set inner page to default
        binding.frameLayout.currentItem = 0

        // set underline to show selection
        binding.btnMyDevices.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        binding.btnMyRemotes.paintFlags = 0

        // tell pager adapter to move to devices page
        changeLayoutState(LayoutState.DEVICES_ALL, true)
    }

    private fun onMyRemotesClicked()
    {
        // set underline to show selection
        binding.btnMyRemotes.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        binding.btnMyDevices.paintFlags = 0

        // set inner page to default
        binding.frameLayout.currentItem = 0

        // tell pager adapter to move to remotes page
        val inEditMode = tempData.tempRemote.inEditMode.get() ?: false
        val newLayoutState = if (inEditMode) LayoutState.REMOTES_FAV_EDITING else LayoutState.REMOTES_FAV
        changeLayoutState(newLayoutState, true)
    }

    // Public

    val showHideUIElementsScrollListener = object : RecyclerView.OnScrollListener()
    {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val elementParts = when(recyclerView.tag) {
                MyRemotesFragment.recyclerViewTag -> ElementParts.BOTTOM
                RemoteFragment.recyclerViewTag -> ElementParts.BOTH
                else -> ElementParts.BOTH
            }
            if (dy > 0) {
                hideUiElements(elementParts)
            } else if (dy < 0) {
                showUiElements(elementParts)
            }
        }
    }

    fun hideUiElements(hiddenParts : ElementParts) {
        if (binding.fab.isOrWillBeHidden)
            return

        val tv = TypedValue()
        val navBarDist = if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true))
            TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        else
            Utils.dpToPx(this, 56f)
        val interpolator = AccelerateInterpolator()

        if (hiddenParts == ElementParts.BOTTOM || hiddenParts == ElementParts.BOTH) {
            binding.fab.hide()
            binding.fab.animate()
                .translationY(200f)
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

        if (hiddenParts == ElementParts.TOP || hiddenParts == ElementParts.BOTH) {
            binding.toolbar.animate()
                .alpha(0f)
                .translationY(-100f)
                .setDuration(300)
                .setInterpolator(interpolator)
                .start()
        }
    }

    private fun showUiElements(shownParts: ElementParts) {
        if (!binding.fab.isOrWillBeHidden)
            return
        val interpolator = DecelerateInterpolator()

        // Check bottom parts (FAB, navView)
        if (shownParts == ElementParts.BOTTOM || shownParts == ElementParts.BOTH) {
            binding.fab.show()
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

        // Check top parts (Toolbar)
        if (shownParts == ElementParts.TOP || shownParts == ElementParts.BOTH) {
            binding.toolbar.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(interpolator)
                .start()
        }
    }

    fun switchInnerPage(position : Int, smoothScroll : Boolean = true) {
        binding.frameLayout.setCurrentItem(position, smoothScroll)
    }

/*
----------------------------------------------
    Activity Functionality
----------------------------------------------
*/
    fun createRemote()
    {
        tempData.tempRemote.copyFrom(null, true)
    }

    fun editRemote() {
        tempData.tempRemote.inEditMode.set(true)
    }

    fun saveRemote() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

/*
----------------------------------------------
    Activity State Functionality
----------------------------------------------
*/
    private fun changeLayoutState(layoutState: LayoutState, fromButtonClick: Boolean = false)
    {
        binding.frameLayout.currentItem = when (layoutState)
        {
            LayoutState.REMOTES_ALL, LayoutState.DEVICES_ALL -> 1
            else -> 0
        }
        pagerAdapter.setLayoutState(layoutState, fromButtonClick)
        binding.fab.layoutState = pagerAdapter.getLayoutState()
        binding.toolbar.layoutState = pagerAdapter.getLayoutState()
    }

    private val editModeChangedListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            val inEditMode = tempData.tempRemote.inEditMode.get() ?: false
            remoteFragment.updateRemoteLayout()
            if (pagerAdapter.getLayoutState() == LayoutState.REMOTES_FAV && inEditMode) {
                changeLayoutState(LayoutState.REMOTES_FAV_EDITING, true)
            } else if (pagerAdapter.getLayoutState() == LayoutState.REMOTES_FAV_EDITING && !inEditMode) {
                changeLayoutState(LayoutState.REMOTES_FAV, true)
            }
        }
    }

    private val remoteChangeListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            when (pagerAdapter.getLayoutState()) {
                LayoutState.REMOTES_FAV, LayoutState.REMOTES_FAV_EDITING ->
                {
                    binding.toolbar.applyLayoutState()
                    binding.fab.applyLayoutState()
                }
                else -> {}
            }
        }
    }

/*
----------------------------------------------
    Companion Objects
----------------------------------------------
*/
    companion object
{
        const val STATE = "com.ms8.irsmarthub.MainMenuActivity.CUSTOM_STATE"

        enum class ElementParts {TOP, BOTTOM, BOTH}

        internal class State() : Parcelable {
            var pagerState: MainMenuAdapter.Companion.State = MainMenuAdapter.Companion.State()
                private set

            constructor(adapterState: MainMenuAdapter.Companion.State) : this()
            {
                this.pagerState = adapterState
            }

            // ---- Parcelable Implementation ---- //
            constructor(parcel: Parcel) : this() {
                pagerState = MainMenuAdapter.Companion.State(parcel)
            }

            override fun writeToParcel(parcel: Parcel, flags: Int) {
                pagerState.writeToParcel(parcel, flags)
            }

            override fun describeContents() = 0

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

/*
----------------------------------------------
    Menu Functions
----------------------------------------------
*/

    fun menuViewAllRemotes(item: MenuItem) { changeLayoutState(LayoutState.REMOTES_ALL, true) }
    fun menuViewCurrentRemote(item: MenuItem) {
        changeLayoutState(
            if (tempData.tempRemote.inEditMode.get() == true) LayoutState.REMOTES_FAV_EDITING else LayoutState.REMOTES_FAV,
            true
        )
    }
    fun menuCreateNewRemote(item: MenuItem) {
        tempData.tempRemote.copyFrom(null, true)
        changeLayoutState(LayoutState.REMOTES_FAV_EDITING, true)
    }
}
