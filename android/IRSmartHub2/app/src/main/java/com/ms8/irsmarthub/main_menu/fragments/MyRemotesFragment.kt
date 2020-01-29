package com.ms8.irsmarthub.main_menu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ms8.irsmarthub.databinding.FRemotesAllBinding

class MyRemotesFragment: MainFragment() {
    override fun newInstance(): MainFragment { return MyRemotesFragment() }


    lateinit var binding: FRemotesAllBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FRemotesAllBinding.inflate(inflater, null, false)

        //todo set up remote list

        return binding.root
    }

    companion object {
        const val recyclerViewTag = "AllRemotesRV"
    }
}