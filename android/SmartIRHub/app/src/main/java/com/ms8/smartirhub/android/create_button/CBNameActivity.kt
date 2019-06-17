package com.ms8.smartirhub.android.create_button

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.databinding.ACreateButtonNameBinding

class CBNameActivity : AppCompatActivity() {
    lateinit var binding: ACreateButtonNameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.a_create_button_name)
    }
}
