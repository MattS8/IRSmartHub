package com.ms8.smartirhub.android.create_button

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.databinding.ACreateButtonSigActionBinding


class CBSigActionActivity : AppCompatActivity() {
    lateinit var binding: ACreateButtonSigActionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.a_create_button_sig_action)

    }
}
