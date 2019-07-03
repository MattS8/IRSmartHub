package com.ms8.smartirhub.android.create_button

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.create_command.CC_ChooseActionsActivity
import com.ms8.smartirhub.android.custom_views.BackWarningSheet
import com.ms8.smartirhub.android.data.RemoteProfile
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.databinding.ACreateButtonWalkthroughBinding

class CBWalkthroughActivity : AppCompatActivity() {
    lateinit var binding: ACreateButtonWalkthroughBinding

    val warningSheet: BackWarningSheet = BackWarningSheet()

/*
    ----------------------------------------------
        Overridden Functions
    ----------------------------------------------
*/

    override fun onBackPressed() {
        when {
            warningSheet.bWantsToLeave -> {
                //Note: TempData is not cleared on exit. It is up to the activity that started the Create Button process to clear TempData
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.a_create_button_walkthrough)

        if (TempData.tempButton == null)
            TempData.tempButton = RemoteProfile.Button()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = getString(R.string.how_to_add_button)

        // Set progress texts
        binding.prog1.description = getString(R.string.prog_button_name)
        binding.prog2.description = getString(R.string.prog_get_actions)
        binding.prog3.description = getString(R.string.prog_button_style)

        // Figure out which step we're on
        determineWalkThroughState()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_SIG_ACTION -> {
                determineWalkThroughState()
            }
            REQ_STYLE -> {
                if (resultCode == Activity.RESULT_OK) {
                    TempData.tempButton?.command?.let {
                        TempData.tempRemoteProfile.addButton(TempData.tempButton!!, intent.getIntExtra(EXTRA_BUTTON_POS, -1))
                        TempData.tempButton = null
                        // Only finish if TempData has a valid button
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
            }
            REQ_NAME -> {
                determineWalkThroughState()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            TempData.tempButton = null
        }
    }

/*
    ----------------------------------------------
        Layout Functions
    ----------------------------------------------
*/

    private fun determineWalkThroughState() {
        when {
            TempData.tempButton == null || TempData.tempButton?.name == "" -> {
                TempData.tempButton = RemoteProfile.Button()
                binding.prog1.bOnThisStep = true
                binding.prog2.bOnThisStep = false
                binding.prog3.bOnThisStep = false
                binding.btnNextStep.text = getString(R.string.pick_a_button_name)
                binding.btnNextStep.setOnClickListener { getNameActivity() }
                binding.prog1.setOnClickListener { getNameActivity() }
            }
            TempData.tempButton?.command?.actions?.size ?: 0 == 0 -> {
                binding.prog1.bOnThisStep = true
                binding.prog2.bOnThisStep = true
                binding.prog3.bOnThisStep = false
                binding.btnNextStep.text = getString(R.string.choose_a_button_action)
                binding.btnNextStep.setOnClickListener { getSignalOrAction() }
                binding.prog1.setOnClickListener { getSignalOrAction() }
            }
            else -> {
                binding.prog1.bOnThisStep = true
                binding.prog2.bOnThisStep = true
                binding.prog3.bOnThisStep = true
                binding.btnNextStep.text = getString(R.string.pick_a_button_style)
                binding.btnNextStep.setOnClickListener { getButtonStyle() }
                binding.prog1.setOnClickListener { getButtonStyle() }
            }
        }
    }

/*
    ----------------------------------------------
        OnClick Functions
    ----------------------------------------------
*/

    private fun getSignalOrAction() {
        startActivityForResult(Intent(this, CC_ChooseActionsActivity::class.java), REQ_SIG_ACTION)
    }

    private fun getButtonStyle() {
        startActivityForResult(Intent(this, CBStyleActivity::class.java), REQ_STYLE)
    }

    private fun getNameActivity() {
        startActivityForResult(Intent(this, CBNameActivity::class.java), REQ_NAME)
    }

    companion object {
        const val REQ_NEW_BUTTON = 9
        const val REQ_NAME = 2
        const val REQ_SIG_ACTION = 3
        const val REQ_STYLE = 4
        const val EXTRA_BUTTON_POS = "EXTRA_BUTTON_POS"
    }
}
