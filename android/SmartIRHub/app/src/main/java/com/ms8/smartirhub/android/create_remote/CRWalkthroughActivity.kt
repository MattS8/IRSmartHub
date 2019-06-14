package com.ms8.smartirhub.android.create_remote

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.databinding.ACreateButtonWalkthroughBinding

class CRWalkthroughActivity : AppCompatActivity() {
    lateinit var binding: ACreateButtonWalkthroughBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.a_create_button_walkthrough)
    }
}
