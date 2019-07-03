package com.ms8.smartirhub.android.learn_signal

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.custom_views.BottomErrorSheet
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.databinding.ALearnSigNameBinding
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.learn_signal.LSWalkthroughActivity.Companion.NEW_IR_SIGNAL_UID
import com.ms8.smartirhub.android.utils.MyValidators.SignalNameValidator

class LSNameSignalActivity : AppCompatActivity() {
    lateinit var binding : ALearnSigNameBinding
    val errorSaveSheet = BottomErrorSheet()
    val errorNameSheet = BottomErrorSheet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        errorSaveSheet.sheetTitle = getString(R.string.err_uploading_signal_title)
        errorSaveSheet.description = getString(R.string.err_uploading_signal_desc)
        errorSaveSheet.setPositiveButton(getString(R.string.retry)) { uploadIrSignal() }
        errorNameSheet.sheetTitle = getString(R.string.err_invalid_sig_name_title)
        errorNameSheet.description = getString(R.string.err_invalid_sig_name_desc)

        binding = DataBindingUtil.setContentView(this, R.layout.a_learn_sig_name)
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
        val isValidName = SignalNameValidator(binding.txtSignalName.editText!!.text.toString())
            .addErrorCallback { errorNameSheet.show(supportFragmentManager, "bottom_sheet_error_invalid_name") }
            .check()
        if (isValidName) {
            TempData.tempSignal!!.name = binding.txtSignalName.editText!!.text.toString()
        }
    }

    /*
    ----------------------------------------------
        Firestore Functions
    ----------------------------------------------
 */

    @SuppressLint("LogNotTimber")
    private fun uploadIrSignal() {
        binding.btnPickName.startAnimation()

        FirestoreActions.addIrSignal()
            .addOnCompleteListener {
                binding.btnPickName.revertAnimation()
            }
            .addOnFailureListener {
                Log.e("LSNameSignalActivity", "AddIrSignal listener error: $it")
                errorSaveSheet.show(supportFragmentManager, "bottom_error_sheet_ir_upload")
            }
            .addOnSuccessListener {
                setResult(Activity.RESULT_OK, Intent().putExtra(NEW_IR_SIGNAL_UID, TempData.tempSignal?.uid))
                TempData.tempSignal = null
                finish()
            }
    }
}
