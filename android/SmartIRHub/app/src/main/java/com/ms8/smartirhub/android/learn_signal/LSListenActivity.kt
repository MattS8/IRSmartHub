package com.ms8.smartirhub.android.learn_signal

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.custom_views.BottomErrorSheet
import com.ms8.smartirhub.android.data.HubResult
import com.ms8.smartirhub.android.data.IrSignal
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.databinding.ALearnSigListenBinding
import com.ms8.smartirhub.android.firebase.FirebaseConstants
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.firebase.RealtimeDatabaseFunctions
import com.ms8.smartirhub.android.learn_signal.LSWalkthroughActivity.Companion.LISTENING_HUB
import java.lang.Exception

class LSListenActivity : AppCompatActivity() {
    lateinit var binding: ALearnSigListenBinding
    lateinit var hubUID: String
    val bottomErrorSheet = BottomErrorSheet()
    var isListeningForResult = false
    var isListeningForRawData = false

    /**
     * This listener is set whenever a hubResult is successfully parsed and a
     * resultCode of IR_RES_RECEIVED_SIG is returned. It connects and reports
     * changes to the rawData backend tree. Whenever all the rawData is read,
     * the listener stops listening and sets the temporary IrSignal's rawData
     * accordingly.
     *
     * This listener concludes the entire listening process.
     */
    private val rawDataListener = object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError) {
            binding.btnStartListening.revertAnimation()
            isListeningForResult = false
        }

        override fun onDataChange(dataSnapshot: DataSnapshot) {
            RealtimeDatabaseFunctions.getHubRef(hubUID).removeEventListener(this)

            val rawData = RealtimeDatabaseFunctions.parseRawData(dataSnapshot.value) ?: return
            val numChunks = RealtimeDatabaseFunctions.calculateNumChunks(TempData.tempSignal?.rawLength ?: 0)

            if (rawData.size == numChunks) {
                // Stop listening for rawData changes
                RealtimeDatabaseFunctions.getRawData(hubUID).removeEventListener(this)

                // Set data array for tempSignal
                TempData.tempSignal?.rawData = rawData

                // Stop loading button and clean up listener data
                binding.btnStartListening.revertAnimation()
                isListeningForResult = false

                // Remove rawData from hub's endpoint
                RealtimeDatabaseFunctions.getRawData(hubUID).removeValue()

                // Show recorded signal info
                showLearnedLayout(true)
            } else {
                Log.w("rawDataListener", "Mismatch in chunk list size: expected $numChunks but actually ${rawData.size}")
            }
        }

    }

    /**
     * This listener is set whenever the user clicks on "start listening".
     * It connects to the Realtime database and reports a result from the
     * IR hub. This listener must only be added AFTER it is deemed safe
     * to listen to the IR hub. (see [sendListenAction])
     */
    private val resultListener = object : ValueEventListener {
        override fun onCancelled(dbError: DatabaseError) {
            binding.btnStartListening.revertAnimation()
            isListeningForResult = false
            Log.e("LSListenActivity", "result listener error: $dbError")
        }

        override fun onDataChange(dataSnapshot: DataSnapshot) {
            Log.d("LSListenActivity", "datasnapshot = $dataSnapshot")
            // Could be null of first call
            val hubResult : HubResult = RealtimeDatabaseFunctions.parseHubResult(dataSnapshot.value) ?: return

            // Display proper response
            when (hubResult.resultCode) {
            // Overflow Error
                FirebaseConstants.IR_RES_OVERFLOW_ERR -> {
                    Log.e("LSListenActivity", "result listener result: Overflow")
                    showOverflowError()
                }
            // Timeout Error
                FirebaseConstants.IR_RES_TIMEOUT_ERR -> {
                    Log.e("LSListenActivity", "result listener result: Timeout")
                    showTimeoutError()
                }
            // Unknown Error
                FirebaseConstants.IR_RES_UNKNOWN_ERR -> {
                    Log.e("LSListenActivity", "result listener result: Unknown Error")
                    showUnknownError(null)
                }
            // Received an IR Signal
                FirebaseConstants.IR_RES_RECEIVED_SIG -> {
                    Log.d("LSListenActivity", "Received signal")
                    TempData.tempSignal = IrSignal()
                        .apply {
                            rawLength = hubResult.rawLen
                            encodingType = hubResult.encoding
                            code = hubResult.code
                            repeat = false //TODO determine if this is needed at all
                         }
                    RealtimeDatabaseFunctions.getHubResults(hubUID).removeEventListener(this)
                    listenForRawData()
                }
            // Unexpected IR result
                else -> {
                    Log.e("LSListenActivity", "result listener result: unexpected result (${hubResult.resultCode})")
                    showUnknownError(null)
                }
            }

            RealtimeDatabaseFunctions.getHubResults(hubUID).removeValue()
        }
    }

/* ------------------------------------------- Layout Transition Functions ------------------------------------------ */

    private fun showLearnedLayout(animate: Boolean) {
        // Set data text
        TempData.tempSignal?.let { irSignal ->
            binding.tvSigType.text = irSignal.encodingType.toString()
            binding.tvSigCode.text = irSignal.code
        }
        // Set Listening Button to Save
        binding.btnStartListening.text = (getString(R.string.save))
        binding.btnStartListening.setOnClickListener { saveRecordedSignal() }
        // Enable Advanced Info button
        binding.btnShowAdvancedInfo.isEnabled = true
        // Enable and show Retry button
        binding.btnRetry.isEnabled = true
        binding.btnRetry.visibility = View.VISIBLE
        // Enable Test Signal button
        binding.btnTestSignal.isEnabled = true

        // Show Learned Info Layout, Advanced Info button, and Test Signal button
        if (animate) {
            ObjectAnimator.ofFloat(binding.learnedSignalLayout, "alpha", 1f).apply {
                duration = ANIM_DURATION.toLong()
                interpolator = DecelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(binding.btnShowAdvancedInfo, "alpha", 1f).apply {
                duration = ANIM_DURATION.toLong()
                interpolator = DecelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(binding.btnTestSignal, "alpha", 1f).apply {
                duration = ANIM_DURATION.toLong()
                interpolator = DecelerateInterpolator()
            }.start()
        } else {
            binding.learnedSignalLayout.alpha = 1f
            binding.btnShowAdvancedInfo.alpha = 1f
            binding.btnTestSignal.alpha = 1f
        }
    }

    private fun hideLearnedLayout(animate: Boolean) {
        // Disabled Advanced Info button
        binding.btnShowAdvancedInfo.isEnabled = false
        // Disable and hide Retry button
        binding.btnRetry.isEnabled = false
        binding.btnRetry.visibility = View.GONE
        //Enable Test Signal button
        binding.btnTestSignal.isEnabled = false

        // Hide Learned Info Layout, Advanced Info button, and Test Signal button
        if (animate) {
            ObjectAnimator.ofFloat(binding.learnedSignalLayout, "alpha", 0f).apply {
                duration = ANIM_DURATION.toLong()
                interpolator = DecelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(binding.btnShowAdvancedInfo, "alpha", 0f).apply {
                duration = ANIM_DURATION.toLong()
                interpolator = DecelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(binding.btnTestSignal, "alpha", 0f).apply {
                duration = ANIM_DURATION.toLong()
                interpolator = DecelerateInterpolator()
            }.start()
        } else {
            binding.learnedSignalLayout.alpha = 0f
            binding.btnShowAdvancedInfo.alpha = 0f
            binding.btnTestSignal.alpha = 0f
        }
    }

/*
    ----------------------------------------------
        Overridden Functions
    ----------------------------------------------
*/


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isListeningForResult = savedInstanceState?.getBoolean(IS_LISTENING) ?: false

        hubUID = intent.getStringExtra(LISTENING_HUB) ?: ""

        bottomErrorSheet.sheetTitle = getString(R.string.err_hub_busy_title)
        bottomErrorSheet.description = getString(R.string.err_hub_busy_desc)

        binding = DataBindingUtil.setContentView(this, R.layout.a_learn_sig_listen)
        binding.btnShowAdvancedInfo.setOnClickListener { showAdvancedInfo() }
        binding.btnRetry.setOnClickListener { retry() }
        binding.btnTestSignal.setOnClickListener { testSignal() }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val len = TempData.tempSignal?.rawLength ?: 0
        Log.d("LEN", "$len")
        when {
        // Have already learned signal
            len > 0 -> {
                binding.btnStartListening.setOnClickListener { saveRecordedSignal() }
                binding.btnStartListening.text = (getString(R.string.save))
                showLearnedLayout(false)
            }
        // No signal learned yet
            else -> {
                binding.btnStartListening.setOnClickListener { beginListeningProcess() }
                binding.btnStartListening.text = getString(R.string.start_listening)
                hideLearnedLayout(false)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putBoolean(IS_LISTENING, isListeningForResult)
    }

    override fun onPause() {
        super.onPause()
        if (isListeningForResult) {
            isListeningForResult = false
            RealtimeDatabaseFunctions.getHubResults(hubUID).removeEventListener(resultListener)
            binding.btnStartListening.revertAnimation()
        }

        if (isListeningForRawData) {
            isListeningForRawData = false
            RealtimeDatabaseFunctions.getRawData(hubUID).removeEventListener(rawDataListener)
            binding.btnStartListening.revertAnimation()
        }
    }

    override fun onNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /* ------------------------------------------- Listening Process Functions ------------------------------------------- */

    private fun beginListeningProcess() {
        binding.btnStartListening.startAnimation()
        FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("action").child("sender")
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    Log.w("LSListenActivity", "Lock Check cancelled $p0")
                    binding.btnStartListening.revertAnimation()
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val sender = dataSnapshot.getValue(String::class.java)
                    Log.d("onDataChange", "Sender: $sender")
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (sender != "_none_" && sender != uid) {
                        binding.btnStartListening.revertAnimation()
                        if (!bottomErrorSheet.bIsShowing) { showHubBusyError() }
                    } else {
                        sendListenAction()
                    }
                }
            })
    }

    /**
     * Attempts to send a "Listen for IR Signal" action to the
     * IR hub. If successful, the app will wait for a result
     * response from the hub (or timeout).
     */
    @SuppressLint("SimpleDateFormat")
    private fun sendListenAction() {
        // Clear result before listening
        RealtimeDatabaseFunctions.clearResult(hubUID)
            .addOnFailureListener {e -> Log.w("LSListenActivity", "Failure while clearing results: $e")}
            .addOnSuccessListener {
                // Clear Action before sending listen action
                RealtimeDatabaseFunctions.sendNoneAction(hubUID)
                    .addOnFailureListener {e -> showUnknownError(e) }
                    .addOnSuccessListener {
                        // Send listen action
                        RealtimeDatabaseFunctions.sendListenAction(hubUID)
                            .addOnFailureListener { Log.e("SendListenAction", "$it") }
                            .addOnSuccessListener { listenForResult() }
                    }
            }
    }

    private fun listenForResult() {
        isListeningForResult = true
        RealtimeDatabaseFunctions.getHubResults(hubUID)
            .addValueEventListener(resultListener)
        Handler().postDelayed({timedOut()}, TIMEOUT_DURATION.toLong())
    }

    private fun listenForRawData() {
        isListeningForRawData = true
        RealtimeDatabaseFunctions.getRawData(hubUID)
            .addValueEventListener(rawDataListener)
    }

/* ------------------------------------------------ OnClick Functions ------------------------------------------------ */

    private fun retry() {
        TempData.tempSignal?.rawData = HashMap()
        TempData.tempSignal?.rawLength = 0

        beginListeningProcess()
    }

    private fun timedOut() {
        if (isListeningForResult) {
            Log.e("LSListenActivity", "Never heard back from the Hub...")
            showNoResponseError()
        }
    }

    private fun testSignal() {
        TempData.tempSignal?.let { irSignal ->
            binding.btnTestSignal.startAnimation()
            RealtimeDatabaseFunctions.sendNoneAction(hubUID)
                .addOnFailureListener {e -> showUnknownError(e) }
                .addOnSuccessListener {
                    RealtimeDatabaseFunctions.sendSignalToHub(hubUID, irSignal)
                        .addOnSuccessListener { binding.btnTestSignal.revertAnimation() }
                        .addOnFailureListener {e -> showUnknownError(e) }
                }
        }
    }

    private fun saveRecordedSignal() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun showAdvancedInfo() {
        startActivity(Intent(this, AdvancedSignalInfoActivity::class.java))
    }

    companion object {
        const val ANIM_DURATION = 750
        const val TIMEOUT_DURATION = 15000
        const val IS_LISTENING = "IS_LISTENING"
    }
}

/*
    ---------------------------------------------
    Display Error Functions
    ---------------------------------------------
*/
private fun LSListenActivity.showUnknownError(e: Exception?) {
    binding.btnTestSignal.revertAnimation()
    bottomErrorSheet.sheetTitle = getString(R.string.err_unknown_title)
    bottomErrorSheet.description = getString(R.string.err_unknown_desc)
    bottomErrorSheet.show(supportFragmentManager, "Bottom_sheet_error_timeout")

    e?.let { Log.e("LSListenActivity", "Unknown Error: $it") }

}

private fun LSListenActivity.showNoResponseError() {
    binding.btnStartListening.revertAnimation()
    bottomErrorSheet.sheetTitle = getString(R.string.err_no_response_title)
    bottomErrorSheet.description = getString(R.string.err_no_response_desc)
    bottomErrorSheet.show(supportFragmentManager, "Bottom_sheet_error_timeout")
}
private fun LSListenActivity.showHubBusyError() {
    binding.btnStartListening.revertAnimation()
    bottomErrorSheet.sheetTitle = getString(R.string.err_hub_busy_title)
    bottomErrorSheet.description = getString(R.string.err_hub_busy_desc)
    bottomErrorSheet.show(supportFragmentManager, "Bottom_error_frag")
}
private fun LSListenActivity.showOverflowError() {
    binding.btnStartListening.revertAnimation()
    bottomErrorSheet.sheetTitle = getString(R.string.err_overflow_title)
    bottomErrorSheet.description = getString(R.string.err_overflow_desc)
    bottomErrorSheet.show(supportFragmentManager, "Bottom_sheet_error_overflow")
}
private fun LSListenActivity.showTimeoutError() {
    binding.btnStartListening.revertAnimation()
    bottomErrorSheet.sheetTitle = getString(R.string.err_timeout_title)
    bottomErrorSheet.description = getString(R.string.err_timeout_desc)
    bottomErrorSheet.show(supportFragmentManager, "Bottom_sheet_error_timeout")
}
