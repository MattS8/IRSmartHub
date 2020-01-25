package com.ms8.irsmarthub.main_menu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ms8.irsmarthub.R
import com.ms8.irsmarthub.databinding.FMyDevicesBinding

class MyDevicesFragment : MainFragment() {
    override fun newInstance(): MainFragment {
        return MyDevicesFragment()
    }

    lateinit var binding: FMyDevicesBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.f_my_devices, container, false)
        return binding.root
    }

    override fun toString(): String {
        return "My Devices Fragment"
    }
}