package com.ms8.smartirhub.android.learn_signal

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.custom_views.BackWarningSheet
import com.ms8.smartirhub.android.custom_views.PickNameSheet
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.databinding.ACreateButtonWalkthroughBinding

class LSWalkthroughActivity : AppCompatActivity() {
    lateinit var binding : ACreateButtonWalkthroughBinding

    private val warningSheet: BackWarningSheet = BackWarningSheet()
    private val nameSheet: PickNameSheet = PickNameSheet().apply {
            nameDesc = getString(R.string.need_help_name_ls_desc)
            tipsTitle = getString(R.string.tips_title)
            tipsDesc1 = getString(R.string.tips_learn_desc_1)
            tipsDesc2 = getString(R.string.tips_learn_desc_2)
            tipsExampleTitle = getString(R.string.for_example)
            nameInputHint = getString(R.string.signal_name_hint)
            callback = object : PickNameSheet.Callback {
                override fun onDismiss() {
                    determineWalkThroughState()
                }
            }
        }

    private var listeningHub = ""
    private var clicked = false

    override fun onBackPressed() {
        when {
            binding.prog1.bOnThisStep -> {
                when {
                    warningSheet.bWantsToLeave -> super.onBackPressed()
                    !warningSheet.bIsShowing -> warningSheet.show(supportFragmentManager, "WarningBottomSheet")
                }
            }
            binding.prog2.bOnThisStep -> {
                listeningHub = ""
                determineWalkThroughState()
            }
            binding.prog3.bOnThisStep -> {
                TempData.tempSignal = null
                determineWalkThroughState()
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

        listeningHub = savedInstanceState?.getString(LISTENING_HUB, "") ?: ""
        binding = DataBindingUtil.setContentView(this, R.layout.a_create_button_walkthrough)

        binding.toolbar.title = getString(R.string.learn_ir_title)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.prog1.description = getString(R.string.select_ir_hub_desc)
        binding.prog2.description = getString(R.string.send_ir_signal_desc)
        binding.prog3.description = getString(R.string.name_learned_signal_desc)
    }

    override fun onResume() {
        super.onResume()
        clicked = false

        determineWalkThroughState()
    }

    private fun determineWalkThroughState() {
        // Figure out which step we're on
        when {
            listeningHub == "" -> {
                Log.d("###TEST", "Starting on prog 1 (height = ${binding.prog1.measuredHeightAndState}")
                binding.prog1.bOnThisStep = true
                binding.prog2.bOnThisStep = false
                binding.prog3.bOnThisStep = false
                binding.btnNextStep.text = getString(R.string.select_ir_hub)
                binding.btnNextStep.setOnClickListener { getHubActivity() }
                binding.prog1.setOnClickListener { getHubActivity() }
            }
            TempData.tempSignal == null || TempData.tempSignal?.rawData?.size == 0 -> {
                Log.d("###TEST", "Starting on prog 2")
                binding.prog1.bOnThisStep = false
                binding.prog2.bOnThisStep = true
                binding.prog3.bOnThisStep = false
                binding.btnNextStep.text = getString(R.string.send_ir_signal)
                binding.btnNextStep.setOnClickListener { getSignalActivity() }
                binding.prog2.setOnClickListener { getSignalActivity() }
            }
            else -> {
                Log.d("###TEST", "Starting on prog 3")
                binding.prog1.bOnThisStep = false
                binding.prog2.bOnThisStep = false
                binding.prog3.bOnThisStep = true
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
                    setResult(Activity.RESULT_OK, Intent().putExtra(NEW_IR_SIGNAL_UID, data?.getStringExtra(NEW_IR_SIGNAL_UID)))
                    finish()
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



    private fun getSignalNameActivity() {
        if (!clicked) {
            startActivityForResult(Intent(this, LSNameSignalActivity::class.java), REQ_NAME)
            clicked = true
        }
    }

    private fun getSignalActivity() {
        if (!clicked) {
            startActivityForResult(Intent(this, LSListenActivity::class.java).putExtra(LISTENING_HUB, listeningHub), REQ_SIG)
            clicked = true
        }
    }

    private fun getHubActivity() {
        if (!clicked){
//            val center = binding.prog1.progressBar.getCenter()
//            val options = ActivityOptionsCompat.makeClipRevealAnimation(binding.prog1, center.first.toInt(), center.second.toInt(), 0, 0)
            val intent = Intent(this, LSSelectHubActivity::class.java)
//            ViewAnimationUtils.createCircularReveal(binding.root, center.first.toInt(), center.second.toInt(), 0f, 100f).apply {
//                duration = 1000
//                interpolator = DecelerateInterpolator()
//            }.start()

            ActivityCompat.startActivityForResult(this, intent, REQ_HUB, null)
            clicked = true
        }
    }



    /**
     * Computes a [Rect] defining the location of this [View] in terms of screen coordinates
     *
     * Note: The view must be laid out before calling this else the returned [Rect] might not be valid
     */
    private fun View.computeScreenBounds(): Rect {
        val viewLocation = IntArray(2).apply { getLocationOnScreen(this) }
        val contentX = viewLocation[0]
        val contentY = viewLocation[1]
        return Rect(
            contentX,
            contentY,
            contentX + width,
            contentY + height
        )
    }

    companion object {
        const val REQ_NEW_IR_SIGNAL = 9
        const val REQ_HUB = 2
        const val REQ_SIG = 3
        const val REQ_NAME = 4
        const val LISTENING_HUB = "LISTENEING_HUB"
        const val NEW_IR_SIGNAL_UID = "NEW_SIGNAL_UID"
        const val EXTRA_REVEAL = "EXTRA_REVEAL"
    }
}

fun View.getCenter() : Pair<Float, Float> {
    return Pair(x + width/2, y + height/2)
}

