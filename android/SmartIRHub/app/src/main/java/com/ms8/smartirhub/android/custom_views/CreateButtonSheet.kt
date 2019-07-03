package com.ms8.smartirhub.android.custom_views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.andrefrsousa.superbottomsheet.SuperBottomSheetFragment
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.databinding.VCreateButtonActionBinding

class CreateButtonSheet : SuperBottomSheetFragment() {
    lateinit var binding: VCreateButtonActionBinding
    //val hubAdapter = HubCardListAdapter(WeakReference(activity))

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.v_bottom_sheet, container, false)
        context?.let {c ->
            binding.tvTitle.text = c.getString(R.string.choose_button_action_title)
            binding.tvDescription.text = c.getString(R.string.choose_button_action_desc)
            binding.btnPos.text = c.getString(R.string.pair)
            binding.btnNeg.text = c.getString(android.R.string.cancel)
            binding.btnPos.setTextColor(ContextCompat.getColor(c, R.color.colorControlNormalWhite))
            binding.btnNeg.setTextColor(ContextCompat.getColor(c, R.color.colorControlNormalWhite))
            binding.btnPos.setOnClickListener {  }


        }

        return binding.root
    }

    /**
     * This listener is set whenever the user clicks on "start listening".
     * It connects to the Realtime database and reports a result from the
     * IR hub. This listener must only be added AFTER it is deemed safe
     * to listen to the IR hub. (see [sendListenAction])
     */
//    private val resultListener = object : ValueEventListener {
//        override fun onCancelled(dbError: DatabaseError) {
//            binding.btnPos.revertAnimation()
//            isListeningForResult = false
//            Log.e("LSListenActivity", "result listener error: $dbError")
//        }
//
//        override fun onDataChange(dataSnapshot: DataSnapshot) {
//            Log.d("LSListenActivity", "datasnapshot = $dataSnapshot")
//            // Could be null of first call
//            val hubResult : HubResult = RealtimeDatabaseFunctions.parseHubResult(dataSnapshot.value) ?: return
//
//            // Display proper response
//            when (hubResult.resultCode) {
//                // Overflow Error
//                FirebaseConstants.IR_RES_OVERFLOW_ERR -> {
//                    Log.e("LSListenActivity", "result listener result: Overflow")
//                    showOverflowError()
//                }
//                // Timeout Error
//                FirebaseConstants.IR_RES_TIMEOUT_ERR -> {
//                    Log.e("LSListenActivity", "result listener result: Timeout")
//                    showTimeoutError()
//                }
//                // Unknown Error
//                FirebaseConstants.IR_RES_UNKNOWN_ERR -> {
//                    Log.e("LSListenActivity", "result listener result: Unknown Error")
//                    showUnknownError(null)
//                }
//                // Received an IR Signal
//                FirebaseConstants.IR_RES_RECEIVED_SIG -> {
//                    Log.d("LSListenActivity", "Received signal")
//                    TempData.tempSignal = IrSignal()
//                        .apply {
//                            rawLength = hubResult.rawLen
//                            encodingType = hubResult.encoding
//                            code = hubResult.code
//                            repeat = false //TODO determine if this is needed at all
//                        }
//                    RealtimeDatabaseFunctions.getHubResults(hubUID).removeEventListener(this)
//                    listenForRawData()
//                }
//                // Unexpected IR result
//                else -> {
//                    Log.e("LSListenActivity", "result listener result: unexpected result (${hubResult.resultCode})")
//                    showUnknownError(null)
//                }
//            }
//
//            RealtimeDatabaseFunctions.getHubResults(hubUID).removeValue()
//        }
//    }
}