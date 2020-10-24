package com.ms8.irsmarthub.firebase

import android.annotation.SuppressLint
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.ms8.irsmarthub.database.AppState
import com.ms8.irsmarthub.firebase.FirebaseConstants.IR_ACTION_LISTEN
import com.ms8.irsmarthub.firebase.FirebaseConstants.IR_ACTION_NONE
import com.ms8.irsmarthub.models.HubAction
import com.ms8.irsmarthub.models.HubResult
import com.ms8.irsmarthub.models.IrSignal
import java.text.SimpleDateFormat
import java.util.*

object RealtimeDatabaseFunctions {
    enum class Command {
        TOGGLE_TV, TURN_ON, TURN_OFF, TOGGLE_MUTE, VOL_UP, VOL_DOWN
    }

    fun sendCommand(command: Command) {
        //TODO

    }

    fun saveCommand(command: Command, signal: IrSignal) {
        FirebaseFirestore.getInstance()
                .collection("lite_commands")
                .document(command.name)
                .set(signal.toFirebaseObject("_admin_"))
    }

    fun listenForCommand() {
        clearResult()
            .addOnFailureListener { e -> AppState.errorData.pairSignalError.set(HubUnknownException(e.message)) }
            .addOnSuccessListener {
                sendNoneAction()
                    .addOnFailureListener { e -> AppState.errorData.pairSignalError.set(HubUnknownException(e.message)) }
                    .addOnSuccessListener {
                        sendListenAction()
                            .addOnFailureListener { e -> AppState.errorData.pairSignalError.set(HubUnknownException(e.message)) }
                            .addOnSuccessListener { hubResultEndpoint().addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(p0: DataSnapshot) {
                                    handleHubResultData(this, p0)
                                }

                                override fun onCancelled(p0: DatabaseError) {
                                    AppState.errorData.pairSignalError
                                        .set(HubUnknownException(p0.message))
                                }
                            }) }
                    }
            }
    }

    /**
     * Parses and sets the proper state variables to denote a hubResult was processed.
     */
    private fun handleHubResultData(eventListener: ValueEventListener, dataSnapshot: DataSnapshot) {
        val hubResult = parseHubResult(dataSnapshot.value)

        // Return if no result was received
        if (hubResult == null) {
            Log.w("RealtimeDatabase", "handleHubResultData - hubResult was null!")
            return
        }

        // Remove the one-time listener
        hubResultEndpoint().removeEventListener(eventListener)

        // Check the result code and act accordingly
        when (hubResult.resultCode) {
            // Overflow Error
            FirebaseConstants.IR_RES_OVERFLOW_ERR -> {
                AppState.errorData.pairSignalError.set(HubOverflowException())
            }
            // Timeout Error
            FirebaseConstants.IR_RES_TIMEOUT_ERR -> {
                AppState.errorData.pairSignalError.set(HubTimeoutException())
            }
            // Unknown Error
            FirebaseConstants.IR_RES_UNKNOWN_ERR -> {
                AppState.errorData.pairSignalError.set(HubUnknownException())
            }
            // Received an IR Signal
            FirebaseConstants.IR_RES_RECEIVED_SIG -> {
                Log.d("RealtimeDatabase", "handleHubResultData - Received signal")
                AppState.pairedSignal.set(IrSignal()
                    .apply {
                        rawLength = hubResult.rawLen
                        encodingType = hubResult.encoding
                        code = hubResult.code
                        repeat = false //TODO determine if this is needed at all
                    })

                rawDataEndpoint().addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        handleRawData(this, dataSnapshot)
                    }

                    override fun onCancelled(p0: DatabaseError) {
                        AppState.errorData.pairSignalError.set(HubUnknownException())
                    }
                })
            }
            // Unexpected IR result
            else -> {
                Log.e("RealtimeDatabase", "unexpected result code (${hubResult.resultCode})")
                AppState.errorData.pairSignalError.set(HubUnknownException())
            }
        }
    }

    /**
     * Parses and sets the proper state variables to denote rawData for some IR Signal was
     * read.
     */
    private fun handleRawData(eventListener: ValueEventListener, dataSnapshot: DataSnapshot) {
        Log.d("RealtimeDatabase", "handleRawData - data changed!")
        val rawData = parseRawData(dataSnapshot.value) ?: return
        val numChunks = calculateNumChunks(AppState.pairedSignal.get()?.rawLength ?: 0)

        Log.d("RealtimeDatabase", "handleRawData - rawData size = ${rawData.size}")

        if (rawData.size == numChunks) {
            // Stop listening for rawData changes
            rawDataEndpoint().removeEventListener(eventListener)

            // Set data array for tempSignal
            val completedTempSignal = IrSignal.copyFrom(AppState.pairedSignal.get())
            completedTempSignal.rawData = rawData
            AppState.pairedSignal.set(completedTempSignal)

            // Remove rawData from hub's endpoint
            rawDataEndpoint().removeValue()
        } else {
            Log.w("RealtimeDatabase", "handleRawData - Mismatch in chunk list size: " +
                    "expected $numChunks but actually ${rawData.size}")
        }
    }

    /**
     * Sends an action that causes the hub to listen for an IR signal. This signal can be read from
     * the hubResult endpoint.
     */
    @SuppressLint("SimpleDateFormat")
    private fun sendListenAction() : Task<Void> {
        // TODO - ensure this can be hardcoded for now...
        val uid = "_admin_"

        return actionEndpoint().setValue(HubAction().apply {
            type = IR_ACTION_LISTEN
            sender = uid
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().time)
        })
    }

    /**
     * Sends a blank action. This is used to ensure that that action endpoint is cleared and
     * ready for a new action.
     */
    @SuppressLint("SimpleDateFormat")
    private fun sendNoneAction() : Task<Void> {
        // TODO - ensure this can be hardcoded for now...
        val uid = "_admin_"

        return actionEndpoint().setValue(HubAction().apply {
            type = IR_ACTION_NONE
            sender = uid
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().time)
        })
    }

    /**
     * Removes any data from the result endpoint
     */
    private fun clearResult(): Task<Void> {
        return deviceEndpoint().child("result").removeValue()
    }

    /**
     * Attempts to massage raw IR data into a nicer list that can be worked on more easily.
     */
    @SuppressLint("UseSparseArrays", "LogNotTimber")
    @Suppress("UNCHECKED_CAST")
    private fun parseRawData(value: Any?): ArrayList<String>? {
        try {
            val tempMap = value as HashMap<Any, Any>
            tempMap.remove("numChunks")
            Log.d("RealtimeDatabase", "parseRawData - tempMap size = ${tempMap.size}  | ${tempMap[0]}")
            return ArrayList<String>()
                .apply {
                    for (i in 0 until tempMap.size)
                        add(tempMap[i] as String)
                }
        } catch (e : Exception) { Log.e("RealtimeDatabase", " parseRawData - $e")}

        try {
            return value as ArrayList<String>
        }
        catch (e : Exception) { Log.e("RealtimeDatabase", "parseRawData - $e")}

        return null
    }

    /**
     * Attempts to massage data into a HubResult.
     */
    private fun parseHubResult(value: Any?): HubResult? {
        try {
            val mapVal = value as Map<String, Any?>?
            val resultCode = mapVal?.get("resultCode") as Number? ?: return null
            val timestamp = mapVal?.get("timestamp") as String? ?: ""
            val encoding = mapVal?.get("encoding") as Number? ?: 0
            val rawLen = mapVal?.get("rawLen") as Number? ?: 0
            val code = mapVal?.get("code") as String? ?: ""
            //val repeat = value?.get("repeat") as Boolean? ?: false

            return HubResult().apply {
                this.resultCode = resultCode.toInt()
                this.timestamp = timestamp
                this.encoding = encoding.toInt()
                this.rawLen = rawLen.toInt()
                this.code = code
            }
        } catch (e : Exception) { Log.e("RealtimeDatabase", "parseHubResult - $e")}

        return null
    }

    /**
     * Determines how many chunks of data to expect given the total length of the IR signal's
     * raw data.
     */
    fun calculateNumChunks(rawLength: Int): Int {
        val numChunks : Int = (rawLength / CHUNK_SIZE)
        return if (numChunks * CHUNK_SIZE < rawLength) numChunks+1 else numChunks
    }

    private const val CHUNK_SIZE = 50

    /** Endpoints **/

    private fun deviceEndpoint() = FirebaseDatabase.getInstance().reference
            .child("devices")
            .child(AppState.hubUID)

    private fun actionEndpoint() = deviceEndpoint().child("action")

    private fun rawDataEndpoint() = deviceEndpoint()
            .child("rawData")

    private fun hubResultEndpoint() = deviceEndpoint()
            .child("raw")
}