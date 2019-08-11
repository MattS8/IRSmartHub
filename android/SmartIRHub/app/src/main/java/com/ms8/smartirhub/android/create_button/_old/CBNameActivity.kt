package com.ms8.smartirhub.android.create_button._old

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.ACreateButtonNameBinding
import com.ms8.smartirhub.android.databinding.VBottomSheetBinding
import com.ms8.smartirhub.android.utils.MyValidators.ButtonNameValidator

class CBNameActivity : AppCompatActivity() {
    lateinit var binding: ACreateButtonNameBinding
    private lateinit var errorNameSheet : BottomSheetDialog
    private var errorNameSheetBinding: VBottomSheetBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.a_create_button_name)

        // set up error BottomSheet
        errorNameSheet = BottomSheetDialog(this)
        val errorNameSheetView = layoutInflater.inflate(R.layout.v_bottom_sheet, null)
        errorNameSheet.setContentView(errorNameSheetView)
        errorNameSheetBinding = DataBindingUtil.bind(errorNameSheetView)
        errorNameSheetBinding?.let { b ->
            b.tvTitle.text =  getString(R.string.err_invalid_button_name_title)
            b.tvDescription.text = getString(R.string.err_invalid_button_name_desc)
        }

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
            .addErrorCallback { errorNameSheet.show() }
            .check()
        if (isValidName) {
            AppState.tempData.tempButton?.name = binding.txtButtonName.editText!!.text.toString()
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}
