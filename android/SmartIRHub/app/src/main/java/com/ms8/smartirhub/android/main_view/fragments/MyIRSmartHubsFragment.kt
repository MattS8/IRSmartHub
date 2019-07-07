package com.ms8.smartirhub.android.main_view.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.databinding.FIrsmathubsBinding


class MyIRSmartHubsFragment : MainFragment() {
    override fun newInstance(): MainFragment {
        return MyIRSmartHubsFragment()
    }

    lateinit var binding: FIrsmathubsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
       binding = DataBindingUtil.inflate(inflater, R.layout.f_irsmathubs, container, false)
        return binding.root
    }

    override fun toString(): String {
        return "My IRSmartHubs Fragment"
    }
}
