package com.ms8.smartirhub.android.learn_signal

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.databinding.ASigAdvancedInfoBinding
import com.ms8.smartirhub.android.firebase.RealtimeDatabaseFunctions

class AdvancedSignalInfoActivity : AppCompatActivity() {
    lateinit var binding: ASigAdvancedInfoBinding

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.a_sig_advanced_info)

        binding.toolbar.title = getString(R.string.signal_information)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        TempData.tempSignal?.let {irSignal ->
            binding.tvCode.text = irSignal.code
            binding.tvEncoding.text = irSignal.encodingType.toString()
            binding.tvRawData.text = irSignal.rawDataToString()
            binding.tvRawLen.text = irSignal.rawLength.toString()
            binding.tvRepeat.text = irSignal.repeat.toString()
            binding.tvRawDataChunksInput.text = RealtimeDatabaseFunctions.calculateNumChunks(irSignal.rawLength).toString()
        }
    }
}
