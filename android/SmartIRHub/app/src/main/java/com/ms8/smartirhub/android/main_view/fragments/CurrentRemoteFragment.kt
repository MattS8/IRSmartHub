package com.ms8.smartirhub.android.main_view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.databinding.FRemoteCurrentBinding

class CurrentRemoteFragment : MainFragment() {
    lateinit var binding: FRemoteCurrentBinding

    override fun newInstance(): MainFragment { return CurrentRemoteFragment() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.f_remote_current, container, false)


        return binding.root
    }

}