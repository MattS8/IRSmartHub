package com.ms8.smartirhub.android.main_view.fragments

import androidx.fragment.app.Fragment

abstract class MainFragment: Fragment() {
    abstract fun newInstance() : MainFragment
}