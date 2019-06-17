package com.ms8.smartirhub.android.learn_signal

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.custom_views.BackWarningSheet
import com.ms8.smartirhub.android.custom_views.BottomErrorSheet
import com.ms8.smartirhub.android.custom_views.CircularProgressView
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.databinding.ACreateButtonWalkthroughBinding
import com.ms8.smartirhub.android.firebase.FirestoreActions

class LSWalkthroughActivity : AppCompatActivity() {
    lateinit var binding : ACreateButtonWalkthroughBinding
    val errorSheet = BottomErrorSheet()
    val warningSheet: BackWarningSheet = BackWarningSheet()
    private var listeningHub = ""

    override fun onBackPressed() {
        when {
            warningSheet.bWantsToLeave -> {
                //Note: TempData is not cleared on exit. It is up to the activity that started the Learn IR Signal process to clear TempData
                super.onBackPressed()
            }
            !warningSheet.bIsShowing -> {
                warningSheet.show(supportFragmentManager, "WarningBottomSheet")
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putString(LISTENING_HUB, listeningHub)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        errorSheet.sheetTitle = getString(R.string.err_uploading_signal_title)
        errorSheet.description = getString(R.string.err_uploading_signal_desc)
        errorSheet.setPositiveButton(getString(R.string.retry)) { uploadIrSignal() }
        listeningHub = savedInstanceState?.getString(LISTENING_HUB, "") ?: ""
        binding = DataBindingUtil.setContentView(this, R.layout.a_create_button_walkthrough)

//        binding.toolbar.title = getString(R.string.learn_ir_title)
//        setSupportActionBar(binding.toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.prog1.description = getString(R.string.select_ir_hub_desc)
        binding.prog2.description = getString(R.string.send_ir_signal_desc)
        binding.prog3.description = getString(R.string.name_learned_signal_desc)
    }

    override fun onResume() {
        super.onResume()

        // Figure out which step we're on
        when {
            listeningHub == "" -> {
                Log.d("###TEST", "Starting on prog 1")
                binding.prog1.bOnThisStep = true
                binding.prog2.bOnThisStep = false
                binding.prog3.bOnThisStep = false
                binding.btnNextStep.text = getString(R.string.select_ir_hub)
                binding.btnNextStep.setOnClickListener { getHubActivity() }
                binding.prog1.setOnClickListener { getHubActivity() }
            }
            TempData.tempSignal == null || TempData.tempSignal?.rawData == "" -> {
                Log.d("###TEST", "Starting on prog 2")
                binding.prog1.bOnThisStep = true
                Handler().postDelayed({
                    binding.prog2.bOnThisStep = true
                }, (CircularProgressView.ANIM_DURATION / 1.25).toLong())
                binding.prog3.bOnThisStep = false
                binding.btnNextStep.text = getString(R.string.send_ir_signal)
                binding.btnNextStep.setOnClickListener { getSignalActivity() }
                binding.prog2.setOnClickListener { getSignalActivity() }
            }
            else -> {
                Log.d("###TEST", "Starting on prog 3")
                binding.prog1.bOnThisStep = true
                Handler().postDelayed({
                    binding.prog2.bOnThisStep = true
                    Handler().postDelayed({
                        binding.prog3.bOnThisStep = true
                    }, (CircularProgressView.ANIM_DURATION / 1.25).toLong())
                }, (CircularProgressView.ANIM_DURATION / 1.25).toLong())
                binding.btnNextStep.setOnClickListener { getSignalNameActivity() }
                binding.prog3.setOnClickListener { getSignalNameActivity() }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_HUB -> {
                if (resultCode == Activity.RESULT_OK) {
                    listeningHub = data?.getStringExtra(LISTENING_HUB) ?: ""
                }
            }
            REQ_NAME -> {
                if (resultCode == Activity.RESULT_OK) {
                    Log.d("###TEST", "We done! Uploading signal...")
                    uploadIrSignal()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            Log.d("LSWalkthroughActivity", "Fishing up!")
            TempData.tempSignal = null
        }
    }

    private fun uploadIrSignal() {
        binding.btnNextStep.isEnabled = false
        binding.prog3.isEnabled = false
        FirestoreActions.addIrSignal()
            .addOnFailureListener {
                Log.e("LSWalkthroughActivity", "AddIrSignal listener error: $it")
                errorSheet.show(supportFragmentManager, "bottom_error_sheet_ir_upload")
            }
            .addOnSuccessListener {
                Log.d("###TEST", "Uploaded!")
                setResult(Activity.RESULT_OK)
                TempData.tempSignal = null
                finish()
            }
    }

    private fun getSignalNameActivity() {
        startActivityForResult(Intent(this, LSNameSignalActivity::class.java), REQ_NAME)
    }

    private fun getSignalActivity() {
        startActivityForResult(Intent(this, LSListenActivity::class.java).putExtra(LISTENING_HUB, listeningHub), REQ_SIG)
    }

    private fun getHubActivity() {
        startActivityForResult(Intent(this, LSSelectHubActivity::class.java), REQ_HUB)
    }


    companion object {
        const val REQ_HUB = 2
        const val REQ_SIG = 3
        const val REQ_NAME = 4
        const val LISTENING_HUB = "LISTENEING_HUB"
    }
}
