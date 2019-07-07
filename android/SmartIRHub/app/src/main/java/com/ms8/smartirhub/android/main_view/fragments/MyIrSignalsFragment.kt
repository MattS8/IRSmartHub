package com.ms8.smartirhub.android.main_view.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.databinding.FIrSignalsBinding

class MyIrSignalsFragment : MainFragment() {
    override fun newInstance(): MainFragment {
        return MyIrSignalsFragment()
    }

    lateinit var binding: FIrSignalsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.f_ir_signals, container, false)
        return binding.root
    }

    override fun toString(): String {
        return "My IR Signals Fragment"
    }
}
