package com.ms8.irsmarthub.main_menu.fragments

import androidx.fragment.app.Fragment

abstract class MainFragment: Fragment() {
    abstract fun newInstance() : MainFragment
}