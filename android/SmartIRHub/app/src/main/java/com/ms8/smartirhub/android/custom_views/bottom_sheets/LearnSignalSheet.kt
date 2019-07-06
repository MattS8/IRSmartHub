package com.ms8.smartirhub.android.custom_views.bottom_sheets

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.databinding.DataBindingUtil
import com.andrefrsousa.superbottomsheet.SuperBottomSheetFragment
import com.andrognito.flashbar.Flashbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.data.HubResult
import com.ms8.smartirhub.android.data.IrSignal
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.databinding.VLearnSigSheetBinding
import com.ms8.smartirhub.android.firebase.FirebaseConstants
import com.ms8.smartirhub.android.firebase.RealtimeDatabaseFunctions
import com.ms8.smartirhub.android.learn_signal.*
import java.lang.Exception

class LearnSignalSheet : SuperBottomSheetFragment() {
    var binding : VLearnSigSheetBinding? = null


    /* State Variables */
    private var hubUID: String = ""
    private var isListeningForResult = false
    private var isListeningForRawData = false

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
            binding?.btnStartListening?.revertAnimation()
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
                binding?.btnStartListening?.revertAnimation()
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
        @SuppressLint("LogNotTimber")
        override fun onCancelled(dbError: DatabaseError) {
            binding?.btnStartListening?.revertAnimation()
            isListeningForResult = false
            Log.e("LSListenActivity", "result listener error: $dbError")
        }

        @SuppressLint("LogNotTimber")
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

/*
    ----------------------------------------------
        Overridden Functions
    ----------------------------------------------
*/

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBooleanArray(IS_LISTENING, BooleanArray(2).apply { set(0, isListeningForResult); set(1, isListeningForRawData) })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = DataBindingUtil.inflate(inflater, R.layout.v_learn_sig_sheet, container, false)

        binding!!.let { b ->
            b.btnTestSignal.setOnClickListener { testSignal() }
            b.btnRetry.setOnClickListener { retry() }
        }

        hideErrorLayout()

        val len = TempData.tempSignal?.rawData?.size ?: -1
        if (len > 0)
            showLearnedLayout(false)
        else
            hideLearnedLayout(false)

        return binding!!.root
    }

    override fun onPause() {
        super.onPause()

        if (isListeningForResult) {
            RealtimeDatabaseFunctions.getHubResults(hubUID).removeEventListener(resultListener)
            binding?.btnStartListening?.revertAnimation()
        }

        if (isListeningForRawData) {
            RealtimeDatabaseFunctions.getRawData(hubUID).removeEventListener(rawDataListener)
            binding?.btnStartListening?.revertAnimation()
        }
    }


/*
    ----------------------------------------------
        Layout Functions
    ----------------------------------------------
*/
    private fun showLearnedLayout(animate: Boolean) {
        // Set data text

        binding?.let {
            TempData.tempSignal?.let { irSignal ->
                it.tvSigType.text = irSignal.encodingType.toString()
                it.tvSigCode.text = irSignal.code
            }
            // Set Listening Button to Save
            it.btnStartListening.text = (getString(R.string.save))
            it.btnStartListening.setOnClickListener { saveRecordedSignal() }
            // Enable Advanced Info button
            it.btnShowAdvancedInfo.isEnabled = true
            // Enable and show Retry button
            it.btnRetry.isEnabled = true
            it.btnRetry.visibility = View.VISIBLE
            // Enable Test Signal button
            it.btnTestSignal.isEnabled = true

            // Show Learned Info Layout, Advanced Info button, and Test Signal button
            if (animate) {
                ObjectAnimator.ofFloat(it.learnedSignalLayout, "alpha", 1f).apply {
                    duration = LSListenActivity.ANIM_DURATION.toLong()
                    interpolator = DecelerateInterpolator()
                }.start()
                ObjectAnimator.ofFloat(it.btnShowAdvancedInfo, "alpha", 1f).apply {
                    duration = LSListenActivity.ANIM_DURATION.toLong()
                    interpolator = DecelerateInterpolator()
                }.start()
                ObjectAnimator.ofFloat(it.btnTestSignal, "alpha", 1f).apply {
                    duration = LSListenActivity.ANIM_DURATION.toLong()
                    interpolator = DecelerateInterpolator()
                }.start()
            } else {
                it.learnedSignalLayout.alpha = 1f
                it.btnShowAdvancedInfo.alpha = 1f
                it.btnTestSignal.alpha = 1f
            }
        }
    }

    private fun hideLearnedLayout(animate: Boolean) {
        binding?.let {
            // Disabled Advanced Info button
            it.btnShowAdvancedInfo.isEnabled = false
            // Disable and hide Retry button
            it.btnRetry.isEnabled = false
            it.btnRetry.visibility = View.GONE
            //Enable Test Signal button
            it.btnTestSignal.isEnabled = false
            // Set Listening Button to Start Listening
            it.btnStartListening.text = (getString(R.string.start_listening))
            it.btnStartListening.setOnClickListener { beginListeningProcess() }

            // Hide Learned Info Layout, Advanced Info button, and Test Signal button
            if (animate) {
                ObjectAnimator.ofFloat(it.learnedSignalLayout, "alpha", 0f).apply {
                    duration = ANIM_DURATION.toLong()
                    interpolator = DecelerateInterpolator()
                }.start()
                ObjectAnimator.ofFloat(it.btnShowAdvancedInfo, "alpha", 0f).apply {
                    duration = ANIM_DURATION.toLong()
                    interpolator = DecelerateInterpolator()
                }.start()
                ObjectAnimator.ofFloat(it.btnTestSignal, "alpha", 0f).apply {
                    duration = ANIM_DURATION.toLong()
                    interpolator = DecelerateInterpolator()
                }.start()
            } else {
                it.learnedSignalLayout.alpha = 0f
                it.btnShowAdvancedInfo.alpha = 0f
                it.btnTestSignal.alpha = 0f
            }
        }
    }

    private fun showErrorLayout(errorTitle: String, errorDesc: String) {
        binding?.let { b ->
            b.btnTestSignal.revertAnimation()
            b.btnTestSignal.revertAnimation()
            b.tvErrorTitle.text = errorTitle
            b.tvErrorDesc.text = errorDesc
            b.learnedSignalLayout.animate().alpha(1f).setDuration(350).setInterpolator(AccelerateDecelerateInterpolator()).start()
        }
    }

    private fun hideErrorLayout() {
        binding?.learnedSignalLayout?.animate()?.alpha(0f)?.setDuration(350)?.setInterpolator(AccelerateDecelerateInterpolator())?.start()
    }

/*
    ---------------------------------------------
        Display Error Functions
    ---------------------------------------------
*/

    @SuppressLint("LogNotTimber")
    private fun showUnknownError(e: Exception?) {
        showErrorLayout(getString(R.string.err_unknown_title), getString(R.string.err_unknown_desc))

        e?.let { Log.e("LSListenActivity", "Unknown Error: $it") }
    }

    private fun showNoResponseError() {
        showErrorLayout(getString(R.string.err_no_response_title), getString(R.string.err_no_response_desc))
    }
    private fun showHubBusyError() {
        showErrorLayout(getString(R.string.err_hub_busy_title), getString(R.string.err_hub_busy_desc))
    }
    private fun showOverflowError() {
        showErrorLayout(getString(R.string.err_overflow_title), getString(R.string.err_overflow_desc))
    }
    private fun showTimeoutError() {
        showErrorLayout(getString(R.string.err_timeout_title), getString(R.string.err_timeout_desc))
    }

    private fun timedOut() {
        if (isListeningForResult) {
            showNoResponseError()
        }
    }

/*
    ----------------------------------------------
        OnClick Functions
    ----------------------------------------------
*/

    private fun retry() {
        TempData.tempSignal?.resetData()

        beginListeningProcess()
    }

    private fun saveRecordedSignal() {
        dismiss()
    }

    private fun beginListeningProcess() {
        binding?.btnStartListening?.startAnimation()
        hideLearnedLayout(true)
        hideErrorLayout()
        RealtimeDatabaseFunctions.getHubAvailability(hubUID).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                binding?.btnStartListening?.revertAnimation()
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val sender = if (dataSnapshot.exists()) dataSnapshot.getValue(String::class.java) else "_none_"
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (sender != "_none_" && sender != uid) {
                    binding?.btnStartListening?.revertAnimation()
                    showHubBusyError()
                } else {
                    sendListenAction()
                }
            }
        })
    }

    private fun testSignal() {
        TempData.tempSignal?.let { irSignal ->
            binding?.btnTestSignal?.startAnimation()
            RealtimeDatabaseFunctions.sendNoneAction(hubUID)
                .addOnFailureListener {e -> showUnknownError(e) }
                .addOnSuccessListener {
                    RealtimeDatabaseFunctions.sendSignalToHub(hubUID, irSignal)
                        .addOnSuccessListener {
                            binding?.btnTestSignal?.revertAnimation()
                            this@LearnSignalSheet.activity?.let { a ->
                                Flashbar.Builder(a).message(R.string.signal_sent)
                            }
                        }
                        .addOnFailureListener {e -> showUnknownError(e) }
                }
        }
    }

/*
    ----------------------------------------------
        IR CommunicationFunctions Functions
    ----------------------------------------------
*/

    /**
     * Attempts to send a "Listen for IR Signal" action to the
     * IR hub. If successful, the app will wait for a result
     * response from the hub (or timeout).
     */
    @SuppressLint("SimpleDateFormat")
    private fun sendListenAction() {
        // Clear result before listening
        RealtimeDatabaseFunctions.clearResult(hubUID)
            .addOnFailureListener {e -> showUnknownError(e)}
            .addOnSuccessListener {
                // Clear Action before sending listen action
                RealtimeDatabaseFunctions.sendNoneAction(hubUID)
                    .addOnFailureListener {e -> showUnknownError(e) }
                    .addOnSuccessListener {
                        // Send listen action
                        RealtimeDatabaseFunctions.sendListenAction(hubUID)
                            .addOnFailureListener {e -> showUnknownError(e) }
                            .addOnSuccessListener { listenForResult() }
                    }
            }
    }

    private fun listenForResult() {
        isListeningForResult = true
        RealtimeDatabaseFunctions.getHubResults(hubUID)
            .addValueEventListener(resultListener)
        Handler().postDelayed({timedOut()}, LSListenActivity.TIMEOUT_DURATION.toLong())
    }

    private fun listenForRawData() {
        isListeningForRawData = true
        RealtimeDatabaseFunctions.getRawData(hubUID)
            .addValueEventListener(rawDataListener)
    }

    companion object {
        const val ANIM_DURATION = 750
        const val TIMEOUT_DURATION = 15000
        const val IS_LISTENING = "IS_LISTENING"
    }

}