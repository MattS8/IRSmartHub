package com.ms8.irsmarthub.main_menu

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerAdapter
import com.ms8.irsmarthub.main_menu.fragments.MainFragment

class MainMenuAdapter(
    fm: FragmentManager,
    behavior: Int,
    state: State = State()
) : FragmentPagerAdapter(fm, behavior)
{
    private var remotesFragList: MutableList<MainFragment> = ArrayList()
    private var devicesFragList: MutableList<MainFragment> = ArrayList()

    private var baseItemId: Long = state.adapterBaseID
    private var layoutState = state.layoutState

    fun getLayoutState() = layoutState

    fun setLayoutState(layoutState: LayoutState, fromButtonClick: Boolean = false) {
        this.layoutState = layoutState
        if (fromButtonClick) {
            Log.d("TEST", "here")
            baseItemId += when (navPositionFromLayoutState()) {
                FP_MY_DEVICES -> devicesFragList.size
                FP_MY_REMOTES -> remotesFragList.size
                else -> maxOf(devicesFragList.size, remotesFragList.size)
            }
            notifyDataSetChanged()
        }
    }

    private fun navPositionFromLayoutState(): Int {
        return when (layoutState) {
            LayoutState.DEVICES_ALL, LayoutState.DEVICES_HUBS ->
                FP_MY_DEVICES
            LayoutState.REMOTES_ALL, LayoutState.REMOTES_FAV, LayoutState.REMOTES_FAV_EDITING ->
                FP_MY_REMOTES
        }
    }

/*
----------------------------------------------
    Overridden Functions
----------------------------------------------
*/
    override fun getItem(position: Int): Fragment {
        return when (navPositionFromLayoutState()) {
            FP_MY_DEVICES -> devicesFragList[position].newInstance()
            FP_MY_REMOTES -> remotesFragList[position].newInstance()
            else -> {
                Log.e(TAG, "(getItem) - Unknown navPosition: ${navPositionFromLayoutState()}")
                remotesFragList[0].newInstance()
            }
        }
    }

    override fun getCount() : Int {
        return when (navPositionFromLayoutState()) {
            FP_MY_DEVICES -> { devicesFragList.size }
            FP_MY_REMOTES -> { remotesFragList.size }
            else -> {
                Log.e(TAG, "(getCount) - Unknown navigation position: ${navPositionFromLayoutState()}")
                0
            }
        }
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun getItemId(position: Int): Long {
        return baseItemId + position
    }

/*
----------------------------------------------
    Public Accessors
----------------------------------------------
*/
    fun addFragment(fragment: MainFragment, list : ViewPagerList) {
        when (list) {
            Companion.ViewPagerList.REMOTES -> remotesFragList.add(fragment)
            Companion.ViewPagerList.DEVICES -> devicesFragList.add(fragment)
        }
    }

    fun getState() = State(
        layoutState,
        baseItemId
    )

/*
----------------------------------------------
    Companion Objects
----------------------------------------------
*/
    companion object
{
        const val FP_MY_DEVICES = 1
        const val FP_MY_REMOTES = 0

        const val TAG = "MainMenuAdapter"
        enum class ViewPagerList {REMOTES, DEVICES}

        enum class LayoutState {
            REMOTES_FAV,
            REMOTES_FAV_EDITING,
            REMOTES_ALL,
            DEVICES_HUBS,
            DEVICES_ALL
        }
        fun layoutStateFromInt(stateAsInt: Int) = LayoutState.values().associateBy(LayoutState::ordinal)[stateAsInt]

        class State(): Parcelable {
            var layoutState: LayoutState = LayoutState.REMOTES_FAV
                private set
            var adapterBaseID: Long = 0
                private set

            constructor(
                layoutState: LayoutState,
                adapterBaseID: Long
            ) : this() {
                this.layoutState = layoutState
                this.adapterBaseID = adapterBaseID
            }

            // ---- Parcelable Implementation ---- //
            constructor(parcel: Parcel) : this() {
                layoutState = layoutStateFromInt(parcel.readInt()) ?: LayoutState.REMOTES_FAV
                adapterBaseID = parcel.readLong()
            }

            override fun writeToParcel(parcel: Parcel, flags: Int) {
                parcel.writeInt(layoutState.ordinal)
                parcel.writeLong(adapterBaseID)
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
}