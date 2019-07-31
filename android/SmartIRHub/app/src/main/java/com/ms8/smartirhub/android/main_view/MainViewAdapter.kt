package com.ms8.smartirhub.android.main_view

import android.annotation.SuppressLint
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerAdapter
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.main_view.fragments.MainFragment

class MainViewAdapter(fm: FragmentManager, behavior: Int, private var navPosition: Int, private var baseItemId: Long) : FragmentPagerAdapter(fm, behavior) {
    private var commandsFragList: MutableList<MainFragment> = ArrayList()
    private var remotesFragList: MutableList<MainFragment> = ArrayList()
    private var devicesFragList: MutableList<MainFragment> = ArrayList()

    @SuppressLint("LogNotTimber")
    override fun getItem(position: Int): Fragment {
        return when (navPosition) {
            R.id.navigation_commands -> commandsFragList[position].newInstance()
            MainViewActivity.FP_MY_DEVICES -> devicesFragList[position].newInstance()
            MainViewActivity.FP_MY_REMOTES -> remotesFragList[position].newInstance()
            else -> {
                Log.w("MainViewAdapter", "Unknown navigation position: $navPosition")
                commandsFragList[0].newInstance()
            }
        }
    }

    @SuppressLint("LogNotTimber")
    override fun getCount() =
        when (navPosition) {
        R.id.navigation_commands -> commandsFragList.size
        MainViewActivity.FP_MY_DEVICES -> devicesFragList.size
        MainViewActivity.FP_MY_REMOTES -> remotesFragList.size
        else -> {
            Log.w("MainViewAdapter", "Unknown navigation position: $navPosition")
            0
        }
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun getItemId(position: Int): Long {
        return baseItemId + position
    }

    fun addFragment(fragment: MainFragment, list : ViewPagerList) {
        when (list) {
            Companion.ViewPagerList.COMMANDS -> commandsFragList.add(fragment)
            Companion.ViewPagerList.REMOTES -> remotesFragList.add(fragment)
            Companion.ViewPagerList.DEVICES -> devicesFragList.add(fragment)
        }
    }

    fun setNavPosition(navPosition: Int) {
        baseItemId += when (this.navPosition) {
            R.id.navigation_commands -> commandsFragList.size
            MainViewActivity.FP_MY_DEVICES -> devicesFragList.size
            MainViewActivity.FP_MY_REMOTES -> remotesFragList.size
            else -> maxOf(commandsFragList.size, devicesFragList.size, remotesFragList.size)
        }
        this.navPosition = navPosition
        notifyDataSetChanged()
    }

    fun getBaseItemId(): Long = baseItemId

    companion object {
        enum class ViewPagerList {COMMANDS, REMOTES, DEVICES}
    }
}