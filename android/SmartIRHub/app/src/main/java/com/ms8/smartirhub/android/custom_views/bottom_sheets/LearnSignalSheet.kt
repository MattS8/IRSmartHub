package com.ms8.smartirhub.android.custom_views.bottom_sheets

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.databinding.DataBindingUtil
import com.andrefrsousa.superbottomsheet.SuperBottomSheetFragment
import com.andrognito.flashbar.Flashbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
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

    var callback: Callback? = null

    /* State Variables */
    var hubUID: String = ""
    private var isListeningForResult = false
    private var isListeningForRawData = false
    private var isListeningForTestSignal = false

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
                isListeningForRawData = false

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
            Log.d("LSheet", "datasnapshot = $dataSnapshot")
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

                    isListeningForResult = false
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
        outState.putBooleanArray(KEY_LSS_BOOLS, BooleanArray(4).apply {
            set(0, isListeningForResult)
            set(1, isListeningForRawData)
            set(2, isListeningForTestSignal)
        })
        outState.putString(KEY_HUB_UID, hubUID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = DataBindingUtil.inflate(inflater, R.layout.v_learn_sig_sheet, container, false)

        /* -- Restore State --*/
        savedInstanceState?.getString(KEY_HUB_UID)?.let { hubUID = it }

        binding!!.let { b ->
            b.btnTestSignal.setOnClickListener { testSignal() }
            b.btnRetry.setOnClickListener { retry() }
            b.btnShowAdvancedInfo.setOnClickListener { showAdvancedLayout() }
        }


        val len = TempData.tempSignal?.rawData?.size ?: -1
        if (len > 0) {
            Log.d("TEST", "Showing learned layout")
            showLearnedLayout(false)
        }
        else {
            Log.d("TEST", "Hiding learned layout")
            hideLearnedLayout(false)
        }

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

    fun onBackPressed() : Boolean {
        Log.d("TEST", "onBackPressed")
        return when {
            isListeningForResult or isListeningForRawData -> {
                isListeningForResult = false
                isListeningForRawData = false
                binding?.btnStartListening?.revertAnimation()
                true
            }
            isListeningForTestSignal -> {
                isListeningForTestSignal = false
                binding?.btnTestSignal?.revertAnimation()
                true
            }
            else -> false
        }
    }

    private fun showAdvancedLayout() {
        startActivity(Intent(activity, AdvancedSignalInfoActivity::class.java))
    }

    private fun showLearnedLayout(animate: Boolean) {
        Log.d("TEST", "Showing learned layout")
        binding?.let {
            TempData.tempSignal?.let { irSignal ->
                it.tvSigType.text = irSignal.encodingType.toString()
                it.tvSigCode.text = irSignal.code
            }
            // Set Listening Button to Save
            it.btnStartListening.text = getString(R.string.save)
            it.btnStartListening.setOnClickListener { saveRecordedSignal() }
            // Enable Advanced Info button
            it.btnShowAdvancedInfo.isEnabled = true
            // Enable and show Retry button
            it.btnRetry.isEnabled = true
            it.btnRetry.visibility = View.VISIBLE
            // Enable Test Signal button
            it.btnTestSignal.isEnabled = true

            // Show Learned Info Layout, Advanced Info button, and Test Signal button
            it.learnedSignalLayout.visibility = View.VISIBLE
            if (animate) {
                val interpolator = AccelerateDecelerateInterpolator()
                it.learnedSignalLayout.animate().alpha(1f).setDuration(ANIM_DURATION).setInterpolator(interpolator)
                    .setListener(object : Animator.AnimatorListener {
                        override fun onAnimationRepeat(p0: Animator?) {}

                        override fun onAnimationEnd(p0: Animator?) { binding?.infoLayout?.invalidate() }

                        override fun onAnimationCancel(p0: Animator?) {}

                        override fun onAnimationStart(p0: Animator?) {}

                    })
                    .start()
                it.btnShowAdvancedInfo.animate().alpha(1f).setDuration(ANIM_DURATION).setInterpolator(interpolator).start()
                it.btnTestSignal.animate().alpha(1f).setDuration(ANIM_DURATION).setInterpolator(interpolator).start()
            } else {
                it.learnedSignalLayout.alpha = 1f
                it.btnShowAdvancedInfo.alpha = 1f
                it.btnTestSignal.alpha = 1f
            }
        }
    }

    private fun hideLearnedLayout(animate: Boolean) {
        Log.d("TEST", "hideLearnedLayout  animate = $animate")
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
                val interpolator = AccelerateDecelerateInterpolator()
                it.learnedSignalLayout.animate().alpha(0f).setDuration(ANIM_DURATION).setInterpolator(interpolator)
                    .setListener(object : Animator.AnimatorListener {
                        override fun onAnimationRepeat(p0: Animator?) {}

                        override fun onAnimationEnd(p0: Animator?) { binding?.learnedSignalLayout?.visibility = View.GONE }

                        override fun onAnimationCancel(p0: Animator?) {}

                        override fun onAnimationStart(p0: Animator?) {}

                    })
                    .start()
                it.btnShowAdvancedInfo.animate().alpha(0f).setDuration(ANIM_DURATION).setInterpolator(interpolator).start()
                it.btnTestSignal.animate().alpha(0f).setDuration(ANIM_DURATION).setInterpolator(interpolator).start()
            } else {
                it.learnedSignalLayout.alpha = 0f
                it.learnedSignalLayout.visibility = View.GONE
                it.btnShowAdvancedInfo.alpha = 0f
                it.btnTestSignal.alpha = 0f
            }
        }
    }

    private fun showErrorLayout(errorTitle: String, errorDesc: String) {
        Log.d("TEST", "showing Error Layout")
        binding?.let { b ->
            b.btnTestSignal.revertAnimation()
            b.btnStartListening.revertAnimation()
            b.tvErrorTitle.text = errorTitle
            b.tvErrorDesc.text = errorDesc
            b.errorLayout.visibility = View.VISIBLE
            b.infoLayout.invalidate()
            b.errorLayout.animate().alpha(1f).setDuration(ANIM_DURATION).setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(p0: Animator?) {

                    }

                    override fun onAnimationEnd(p0: Animator?) {
                        binding?.infoLayout?.invalidate()
                    }

                    override fun onAnimationCancel(p0: Animator?) {
                        binding?.infoLayout?.invalidate()
                    }

                    override fun onAnimationStart(p0: Animator?) {
                    }

                })
               .start()
        }
    }

    private fun hideErrorLayout() {
        Log.d("TEST", "hiding Error Layout")
        binding?.errorLayout?.animate()?.alpha(0f)?.setDuration(ANIM_DURATION)?.setInterpolator(AccelerateDecelerateInterpolator())
            ?.setListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(p0: Animator?) {}

                override fun onAnimationEnd(p0: Animator?) { binding?.errorLayout?.visibility = View.GONE }

                override fun onAnimationCancel(p0: Animator?) {}

                override fun onAnimationStart(p0: Animator?) {}

            })
            ?.start()
    }

/*
    ---------------------------------------------
        Display Error Functions
    ---------------------------------------------
*/

    @SuppressLint("LogNotTimber")
    private fun showUnknownError(e: Exception?) {
        context?.let {
            showErrorLayout(getString(R.string.err_unknown_sig_title), getString(R.string.err_unknown_sig_desc))
        }

        e?.let { Log.e("LSListenActivity", "Unknown Error: $it") }
    }

    private fun showNoResponseError() {
        context?.let {
            showErrorLayout(getString(R.string.err_no_response_title), getString(R.string.err_no_response_desc))
        }
    }
    private fun showHubBusyError() {
        context?.let {
            showErrorLayout(getString(R.string.err_hub_busy_title), getString(R.string.err_hub_busy_desc))
        }
    }
    private fun showOverflowError() {
        context?.let {
            showErrorLayout(getString(R.string.err_overflow_title), getString(R.string.err_overflow_desc))
        }
    }
    private fun showTimeoutError() {
        context?.let {
            showErrorLayout(getString(R.string.err_timeout_title), getString(R.string.err_timeout_desc))
        }
    }

    private fun timedOut() {
        if (isListeningForResult || isListeningForRawData) {
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
        callback?.onSaveSignal()
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
            isListeningForTestSignal = true
            binding?.btnTestSignal?.startAnimation()
            RealtimeDatabaseFunctions.sendNoneAction(hubUID)
                .addOnFailureListener {e -> showUnknownError(e) }
                .addOnSuccessListener {
                    if (isListeningForTestSignal) {
                        RealtimeDatabaseFunctions.sendSignalToHub(hubUID, irSignal)
                            .addOnSuccessListener {
                                isListeningForTestSignal = false
                                binding?.btnTestSignal?.revertAnimation()
                                this@LearnSignalSheet.activity?.let { a ->
                                    Flashbar.Builder(a).message(R.string.signal_sent).show()
                                }
                            }
                            .addOnFailureListener {e -> showUnknownError(e) }
                    }
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

    interface Callback {
        fun onSaveSignal()
    }

    companion object {
        const val ANIM_DURATION: Long = 750
        const val TIMEOUT_DURATION = 15000
        const val KEY_LSS_BOOLS = "KEY_LSS_BOOLS"
        const val KEY_HUB_UID = "KEY_HUB_UID"
    }

}