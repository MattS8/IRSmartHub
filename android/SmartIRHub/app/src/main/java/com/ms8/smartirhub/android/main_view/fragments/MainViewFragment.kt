package com.ms8.smartirhub.android.main_view.fragments

import androidx.fragment.app.Fragment

abstract class MainViewFragment: Fragment() {
    abstract fun newInstance() : MainViewFragment
}