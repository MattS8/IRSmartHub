package com.ms8.smartirhub.android.create_remote

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.andrefrsousa.superbottomsheet.SuperBottomSheetFragment
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.data.RemoteProfile
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.databinding.ACreateButtonWalkthroughBinding

class CBWalkthroughActivity : AppCompatActivity() {
    lateinit var binding: ACreateButtonWalkthroughBinding
    var buttonPos = 0
    val warningSheet: BackWarningSheet = BackWarningSheet()

    override fun onBackPressed() {
        when {
            warningSheet.bWantsToLeave -> {
                Log.d("TEST", " I WANT TO LEAVE")
                super.onBackPressed()
            }
            !warningSheet.bIsShowing -> {
                Log.d("TEST", "HELLOOO")
                warningSheet.show(supportFragmentManager, "WarningBottomSheet")
            }
            else -> {
                Log.d("TEST", "HMMM")
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

        buttonPos = intent.getIntExtra(EXTRA_BUTTON_POS, 0)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = getString(R.string.add_button)

        // Figure out which step we're on
        when {
            TempData.tempButton == null || TempData.tempButton!!.name == "" -> {
                TempData.tempButton = RemoteProfile.Button(buttonPos)
                binding.prog1.bOnThisStep = true
                binding.prog2.bOnThisStep = false
                binding.prog3.bOnThisStep = false
                binding.btnNextStep.text = getString(R.string.pick_name)
                binding.btnNextStep.setOnClickListener { getNameActivity() }
                binding.prog1.setOnClickListener { getNameActivity() }
            }
            TempData.tempRemoteProfile!!.buttons.size == 0 -> {
                binding.prog1.bOnThisStep = true
                binding.prog2.bOnThisStep = true
                binding.prog3.bOnThisStep = false
                binding.btnNextStep.text = getString(R.string.choose_action)
                binding.btnNextStep.setOnClickListener { getSignalOrAction() }
                binding.prog1.setOnClickListener { getSignalOrAction() }
            }
            else -> {
                binding.prog1.bOnThisStep = true
                binding.prog2.bOnThisStep = true
                binding.prog3.bOnThisStep = true
                binding.btnNextStep.text = getString(R.string.pick_style)
                binding.btnNextStep.setOnClickListener { getButtonStyle() }
                binding.prog1.setOnClickListener { getButtonStyle() }
            }
        }
    }

    fun getSignalOrAction() {
        startActivityForResult(Intent(this, CBSigActionActivity::class.java), REQ_SIG_ACTION)
    }

    fun getButtonStyle() {
        startActivityForResult(Intent(this, CBStyleActivity::class.java), REQ_STYLE)
    }

    fun getNameActivity() {
        startActivityForResult(Intent(this, CBNameActivity::class.java), REQ_NAME)
    }

    class BackWarningSheet : SuperBottomSheetFragment() {
        lateinit var binding: com.ms8.smartirhub.android.databinding.DBottomSheetBinding
        var bWantsToLeave = false
        var bIsShowing = false

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            super.onCreateView(inflater, container, savedInstanceState)
            binding = DataBindingUtil.inflate(inflater, R.layout.d_bottom_sheet, container, false)
            binding.tvTitle.text = context!!.getString(R.string.are_you_sure)
            binding.tvDescription.text = context!!.getString(R.string.wrn_create_button_desc)
            binding.btnNeg.text = context!!.getString(R.string.stay)
            binding.btnNeg.setOnClickListener { dismissWarning() }
            binding.btnPos.setOnClickListener { leave() }
            binding.btnPos.text = context!!.getString(R.string.go_back)

            return binding.root
        }

        override fun getBackgroundColor(): Int {
            return ContextCompat.getColor(context!!, R.color.colorPrimaryDark)
        }

        private fun dismissWarning() {
            bIsShowing = false
            dismiss()
        }

        private fun leave() {
            bWantsToLeave = true
            activity?.onBackPressed()
        }
    }

    companion object {
        const val REQ_NAME = 2
        const val REQ_SIG_ACTION = 3
        const val REQ_STYLE = 4
        const val EXTRA_BUTTON_POS = "EXTRA_BUTTON_POS"
    }
}
