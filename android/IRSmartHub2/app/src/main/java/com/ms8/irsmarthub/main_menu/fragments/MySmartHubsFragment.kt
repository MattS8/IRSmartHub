package com.ms8.irsmarthub.main_menu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ms8.irsmarthub.R
import com.ms8.irsmarthub.databinding.FMySmartHubsBinding

class MySmartHubsFragment : MainFragment() {
    override fun newInstance(): MainFragment {
        return MySmartHubsFragment()
    }

    lateinit var binding: FMySmartHubsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.f_my_smart_hubs, container, false)
        return binding.root
    }

    override fun toString(): String {
        return "My IRSmartHubs Fragment"
    }
}