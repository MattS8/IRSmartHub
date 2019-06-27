package com.ms8.smartirhub.android.main_view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.databinding.FCommandsBinding


class MyCommandsFragment : MainViewFragment() {
    override fun newInstance(): MainViewFragment {
        return MyCommandsFragment()
    }

    lateinit var binding : FCommandsBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.f_commands, container, false)
        return binding.root
    }

    override fun toString(): String {
        return "My Commands Fragment"
    }
}
