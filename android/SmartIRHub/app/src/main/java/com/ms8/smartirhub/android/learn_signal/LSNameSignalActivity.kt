package com.ms8.smartirhub.android.learn_signal

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.custom_views.bottom_sheets.BottomErrorSheet
import com.ms8.smartirhub.android.custom_views.bottom_sheets.BottomSheet
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.ALearnSigNameBinding
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.learn_signal.LSWalkThroughActivity.Companion.NEW_IR_SIGNAL_UID
import com.ms8.smartirhub.android.utils.MyValidators.SignalNameValidator

class LSNameSignalActivity : AppCompatActivity() {
    lateinit var binding : ALearnSigNameBinding
    lateinit var errorSaveSheet : BottomErrorSheet
    lateinit var errorNameSheet : BottomErrorSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set up saving error BottomErrorSheet
        errorSaveSheet = BottomErrorSheet(this,
            getString(R.string.err_uploading_signal_title),
            getString(R.string.err_uploading_signal_desc),
            getString(R.string.retry))
            .apply { posListener = { uploadIrSignal() } }
        errorSaveSheet.setup()

        // set up naming error BottomErrorSheet
        errorNameSheet = BottomErrorSheet(this,
            getString(R.string.err_invalid_sig_name_title),
            getString(R.string.err_invalid_sig_name_desc))
        errorNameSheet.setup()

        binding = DataBindingUtil.setContentView(this, R.layout.a_learn_sig_name)
        binding.btnPickName.setOnClickListener { checkName() }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun hideErrorSheets() {
        if (errorSaveSheet.isShowing)
            errorSaveSheet.dismiss()
        if (errorNameSheet.isShowing)
            errorNameSheet.dismiss()
    }

    /*
        ----------------------------------------------
            OnClick Functions
        ----------------------------------------------
     */
    private fun checkName() {
        val isValidName = binding.txtSignalName.editText!!.text.toString().SignalNameValidator()
            .addErrorCallback {
                hideErrorSheets()
                errorNameSheet.show()
            }
            .check()
        if (isValidName) {
            AppState.tempData.tempSignal.get()?.name = binding.txtSignalName.editText!!.text.toString()
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
                hideErrorSheets()
                errorSaveSheet.show()
            }
            .addOnSuccessListener {
                setResult(Activity.RESULT_OK, Intent().putExtra(NEW_IR_SIGNAL_UID, AppState.tempData.tempSignal.get()?.uid))
                AppState.tempData.tempSignal.set(null)
                finish()
            }
    }
}
