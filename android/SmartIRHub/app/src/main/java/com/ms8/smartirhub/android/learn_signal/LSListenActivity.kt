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
import com.ms8.smartirhub.android.data.HubAction
import com.ms8.smartirhub.android.data.HubResult
import com.ms8.smartirhub.android.data.IrSignal
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.databinding.ALearnSigListenBinding
import com.ms8.smartirhub.android.firebase.FirebaseConstants
import com.ms8.smartirhub.android.firebase.FirebaseConstants.IR_ACTION_LISTEN
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.firebase.RealtimeDatabaseFunctions
import com.ms8.smartirhub.android.learn_signal.LSWalkthroughActivity.Companion.LISTENING_HUB
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class LSListenActivity : AppCompatActivity() {
    lateinit var binding: ALearnSigListenBinding
    lateinit var hubUID: String
    val bottomErrorSheet = BottomErrorSheet()
    var isListening = false

    /**
     * This listener is set whenever the user clicks on "start listening".
     * It connects to the Realtime database and reports a result from the
     * IR hub. This listener must only be added AFTER it is deemed safe
     * to listen to the IR hub. (see [sendListenAction])
     */
    val resultListener = object : ValueEventListener {
        override fun onCancelled(dbError: DatabaseError) {
            binding.btnStartListening.revertAnimation()
            isListening = false
            Log.e("LSListenActivity", "result listener error: $dbError")
        }

        override fun onDataChange(dataSnapshot: DataSnapshot) {
            binding.btnStartListening.revertAnimation()
            isListening = false
            FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("result").removeEventListener(this)

            Log.d("LSListenActivity", "datasnapshot = $dataSnapshot")
            var hubResult : HubResult? = null
            try {
                @Suppress("UNCHECKED_CAST")
                hubResult = FirestoreActions.parseHubResult(dataSnapshot.value as Map<String, Any>?)
            } catch (e : Exception) { Log.e("LSListenActivity", "$e") }

            if (hubResult == null) {
                Log.e("LSListenActivity", "result listener hub result was NULL... ${dataSnapshot.value}")
                bottomErrorSheet.sheetTitle = getString(R.string.err_unknown_title)
                bottomErrorSheet.description = getString(R.string.err_unknown_desc)
                bottomErrorSheet.show(supportFragmentManager, "Bottom_sheet_error_unknown")
                return
            }
            when (hubResult.code) {
            // Overflow Error
                FirebaseConstants.IR_RES_OVERFLOW_ERR -> {
                    Log.e("LSListenActivity", "result listener result: Overflow")
                    bottomErrorSheet.sheetTitle = getString(R.string.err_overflow_title)
                    bottomErrorSheet.description = getString(R.string.err_overflow_desc)
                    bottomErrorSheet.show(supportFragmentManager, "Bottom_sheet_error_overflow")
                }
            // Timeout Error
                FirebaseConstants.IR_RES_TIMEOUT_ERR -> {
                    Log.e("LSListenActivity", "result listener result: Timeout")
                    bottomErrorSheet.sheetTitle = getString(R.string.err_timeout_title)
                    bottomErrorSheet.description =  getString(R.string.err_timeout_desc)
                    bottomErrorSheet.show(supportFragmentManager, "Bottom_sheet_error_timeout")
                }
            // Unknown Error
                FirebaseConstants.IR_RES_UNKNOWN_ERR -> {
                    Log.e("LSListenActivity", "result listener result: Unknown Error")
                    bottomErrorSheet.sheetTitle = getString(R.string.err_unknown_title)
                    bottomErrorSheet.description =  getString(R.string.err_unknown_desc)
                    bottomErrorSheet.show(supportFragmentManager, "Bottom_sheet_error_unknown")
                }
            // Received an IR Signal
                FirebaseConstants.IR_RES_RECEIVED_SIG -> {
                    Log.d("LSListenActivity", "Received signal")
                    TempData.tempSignal = IrSignal().apply {
                        rawData = hubResult.rawData
                        rawLength = hubResult.rawLen
                        //signalType = hubResult.protocol
                    }
                    showLearnedLayout(true)
                }
            // Unexpected IR result
                else -> {
                    Log.e("LSListenActivity", "result listener result: unexpected result (${hubResult.code})")
                    bottomErrorSheet.sheetTitle = getString(R.string.err_unknown_title)
                    bottomErrorSheet.description = getString(R.string.err_unknown_desc)
                    bottomErrorSheet.show(supportFragmentManager, "Bottom_sheet_error_unknown")
                }
            }
        }
    }

    private fun retry() {
        TempData.tempSignal?.rawData = ""
        TempData.tempSignal?.rawLength = 0

        beginListeningProcess()
    }


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
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (sender != "" && sender != uid) {
                        binding.btnStartListening.revertAnimation()
                        if (!bottomErrorSheet.bIsShowing) {
                            bottomErrorSheet.sheetTitle = getString(R.string.err_hub_busy_title)
                            bottomErrorSheet.description = getString(R.string.err_hub_busy_desc)
                            bottomErrorSheet.show(supportFragmentManager, "Bottom_error_frag")
                        }
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("action")
            .setValue(HubAction(IR_ACTION_LISTEN, uid, SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().time)))
            .addOnFailureListener {
                Log.e("SendListenAction", "$it")
            }
            .addOnSuccessListener {
                listenForResult()
            }
    }

/* ------------------------------------------- Layout Transition Functions ------------------------------------------ */

    private fun showLearnedLayout(animate: Boolean) {
        // Set data text
        TempData.tempSignal?.let {irSignal -> binding.tvSigType.text = irSignal.signalType }
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

/* ---------------------------------------------- Overridden Functions ---------------------------------------------- */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isListening = savedInstanceState?.getBoolean(IS_LISTENING) ?: false

        hubUID = intent.getStringExtra(LISTENING_HUB) ?: ""

        bottomErrorSheet.sheetTitle = getString(R.string.err_hub_busy_title)
        bottomErrorSheet.description = getString(R.string.err_hub_busy_desc)

        binding = DataBindingUtil.setContentView(this, R.layout.a_learn_sig_listen)
        binding.btnShowAdvancedInfo.setOnClickListener { showAdvancedInfo() }
        binding.btnRetry.setOnClickListener { retry() }
        binding.btnTestSignal.setOnClickListener { testSignal() }

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

    private fun testSignal() {
        TempData.tempSignal?.let { irSignal ->
            binding.btnTestSignal.startAnimation()
            RealtimeDatabaseFunctions.sendNoneAction(hubUID)
                .addOnSuccessListener {
                    RealtimeDatabaseFunctions.sendSignalToHub(hubUID, irSignal)
                        .addOnSuccessListener {
                            binding.btnTestSignal.revertAnimation()
                        }
                        .addOnFailureListener {
                            binding.btnTestSignal.revertAnimation()
                            bottomErrorSheet.sheetTitle = getString(R.string.err_unknown_title)
                            bottomErrorSheet.description =  getString(R.string.err_unknown_desc)
                            bottomErrorSheet.show(supportFragmentManager, "Bottom_sheet_error_timeout")
                        }
                }
                .addOnFailureListener {
                    binding.btnTestSignal.revertAnimation()
                    bottomErrorSheet.sheetTitle = getString(R.string.err_unknown_title)
                    bottomErrorSheet.description =  getString(R.string.err_unknown_desc)
                    bottomErrorSheet.show(supportFragmentManager, "Bottom_sheet_error_timeout")
                }

        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putBoolean(IS_LISTENING, isListening)
    }

    override fun onPause() {
        super.onPause()
        if (isListening) {
            isListening = false
            FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("result").removeEventListener(resultListener)
            binding.btnStartListening.revertAnimation()
        }
    }



    private fun listenForResult() {
        isListening = true
        FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("result")
            .addValueEventListener(resultListener)
        Handler().postDelayed({timedOut()}, TIMEOUT_DURATION.toLong())
    }

    private fun timedOut() {
        if (isListening) {
            Log.e("LSListenActivity", "Never heard back from the Hub...")
            bottomErrorSheet.sheetTitle = getString(R.string.err_no_response_title)
            bottomErrorSheet.description =  getString(R.string.err_no_response_desc)
            bottomErrorSheet.show(supportFragmentManager, "Bottom_sheet_error_timeout")
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
