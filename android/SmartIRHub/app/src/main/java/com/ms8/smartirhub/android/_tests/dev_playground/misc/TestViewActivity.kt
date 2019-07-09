package com.ms8.smartirhub.android._tests.dev_playground.misc

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
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
