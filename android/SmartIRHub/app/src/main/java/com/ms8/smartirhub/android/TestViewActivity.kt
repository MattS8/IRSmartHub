package com.ms8.smartirhub.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.databinding.ObservableArrayMap
import androidx.databinding.ObservableMap
import com.ms8.smartirhub.android.data.RemoteProfile
import com.ms8.smartirhub.android.databinding.ACreateButtonWalkthroughBinding

class TestViewActivity : AppCompatActivity() {
    lateinit var binding: ACreateButtonWalkthroughBinding
    var step = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.test__activity)

        binding.btnNextStep.setOnClickListener { nextStep() }


    }

    private fun nextStep() {
        when (++step) {
            2 -> {
                binding.prog2.bOnThisStep = true
            }
            3 -> {
                binding.prog3.bOnThisStep = true
            }
            else -> {
                binding.prog1.bOnThisStep = true
                step = 1
            }
        }
    }
}
