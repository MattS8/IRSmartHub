package com.ms8.smartirhub.android.main_view.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.databinding.FRemoteAllBinding

class MyRemotesFragment : MainFragment() {
    override fun newInstance(): MainFragment {
        return MyRemotesFragment()
    }

    lateinit var binding: FRemoteAllBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.f_remote_all, container, false)
        return binding.root
    }

    override fun toString(): String {
        return "My Remotes Fragment"
    }
}
