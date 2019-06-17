package com.ms8.smartirhub.android.learn_signal

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.custom_views.BottomErrorSheet
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.databinding.ALearnSigNameBinding
import com.ms8.smartirhub.android.utils.MyValidators.SignalNameValidator

class LSNameSignalActivity : AppCompatActivity() {
    lateinit var binding : ALearnSigNameBinding
    val errorBottomErrorSheet = BottomErrorSheet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        errorBottomErrorSheet.sheetTitle = getString(R.string.err_invalid_sig_name_title)
        errorBottomErrorSheet.description = getString(R.string.err_invalid_sig_name_desc)
        binding = DataBindingUtil.setContentView(this, R.layout.a_learn_sig_name)
        binding.btnPickName.setOnClickListener { checkName() }
    }

    private fun checkName() {
        val isValidName = SignalNameValidator(binding.txtSignalName.editText!!.text.toString())
            .addErrorCallback { errorBottomErrorSheet.show(supportFragmentManager, "bottom_sheet_error_invalid_name") }
            .check()
        if (isValidName) {
            TempData.tempSignal!!.name = binding.txtSignalName.editText!!.text.toString()
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}
