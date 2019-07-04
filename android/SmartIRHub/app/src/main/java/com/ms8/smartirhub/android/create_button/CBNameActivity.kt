package com.ms8.smartirhub.android.create_button

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.custom_views.BottomErrorSheet
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.databinding.ACreateButtonNameBinding
import com.ms8.smartirhub.android.utils.MyValidators.ButtonNameValidator

class CBNameActivity : AppCompatActivity() {
    lateinit var binding: ACreateButtonNameBinding
    val errorNameSheet = BottomErrorSheet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.a_create_button_name)

        errorNameSheet.sheetTitle = getString(R.string.err_invalid_button_name_title)
        errorNameSheet.description = getString(R.string.err_invalid_button_name_desc)

        binding.btnPickName.setOnClickListener { checkName() }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }


/*
    ----------------------------------------------
        OnClick Functions
    ----------------------------------------------
 */
    private fun checkName() {
        val isValidName = binding.txtButtonName.editText!!.text.toString().ButtonNameValidator()
            .addErrorCallback { errorNameSheet.show(supportFragmentManager, "bottom_sheet_error_invalid_name") }
            .check()
        if (isValidName) {
            TempData.tempButton?.name = binding.txtButtonName.editText!!.text.toString()
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}
