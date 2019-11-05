package com.ms8.smartirhub.android.firebase

import android.annotation.SuppressLint
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Command
import com.ms8.smartirhub.android.models.realtimedatabase.HubAction
import com.ms8.smartirhub.android.models.realtimedatabase.HubResult
import com.ms8.smartirhub.android.models.firestore.IrSignal
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.firebase.FirebaseConstants.IR_ACTION_LISTEN
import com.ms8.smartirhub.android.firebase.FirebaseConstants.IR_ACTION_NONE
import com.ms8.smartirhub.android.firebase.FirebaseConstants.IR_ACTION_SEND
import com.ms8.smartirhub.android.models.firestore.Hub
import com.ms8.smartirhub.android.models.firestore.Hub.Companion.DEFAULT_HUB
import org.jetbrains.anko.doAsync
import java.text.SimpleDateFormat
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import java.util.*

object RealtimeDatabaseFunctions {

    @SuppressLint("LogNotTimber")
    fun sendListenAction2(hubUID: String) {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid

        Log.d("RDBF", "sendListenAction2 - Begging listening process on hub $hubUID from user $uid")
        clearResult(hubUID)
            .addOnFailureListener {  e -> AppState.errorData.pairSignalError.set(HubUnknownException(e.message)) }
            .addOnSuccessListener {
                sendNoneAction(hubUID)
                    .addOnFailureListener { e -> AppState.errorData.pairSignalError.set(HubUnknownException(e.message)) }
                    .addOnSuccessListener {
                        FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("action")
                            .setValue(
                                HubAction().apply {
                                    type = IR_ACTION_LISTEN
                                    sender = uid
                                    timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Calendar.getInstance().time)
                                }
                            )
                            .addOnFailureListener { e -> AppState.errorData.pairSignalError.set(HubUnknownException(e.message)) }
                            .addOnSuccessListener {
                                getHubResults(hubUID).addValueEventListener(object : ValueEventListener {
                                    override fun onCancelled(p0: DatabaseError) { AppState.errorData.pairSignalError.set(HubUnknownException(p0.toException().message)) }


                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        val hubResult = parseHubResult(dataSnapshot.value)

                                        if (hubResult == null) {
                                            Log.w("RealtimeDatabase", "sendListenAction2 - hubResult was null!")
                                            return
                                        }

                                        getHubResults(hubUID).removeEventListener(this)

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
                                                Log.d("RealtimeDatabase", "sendListenAction2 - Received signal")
                                                AppState.tempData.tempSignal.set(IrSignal()
                                                    .apply {
                                                        rawLength = hubResult.rawLen
                                                        encodingType = hubResult.encoding
                                                        code = hubResult.code
                                                        repeat = false //TODO determine if this is needed at all
                                                    })

                                                getRawData(hubUID).addValueEventListener(object : ValueEventListener {
                                                    override fun onCancelled(p0: DatabaseError) { AppState.errorData.pairSignalError.set(HubUnknownException()) }

                                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                        Log.d("RealtimeDatabase", "getRawData - data changed!")
                                                        val rawData = parseRawData(dataSnapshot.value) ?: return
                                                        val numChunks = calculateNumChunks(AppState.tempData.tempSignal.get()?.rawLength ?: 0)

                                                        Log.d("RealtimeDatabase", "getRawData - rawData size = ${rawData.size}")

                                                        if (rawData.size == numChunks) {
                                                            // Stop listening for rawData changes
                                                            getRawData(hubUID).removeEventListener(this)

                                                            // Set data array for tempSignal
                                                            val completedTempSignal = IrSignal.copyFrom(AppState.tempData.tempSignal.get())
                                                            completedTempSignal.rawData = rawData
                                                            AppState.tempData.tempSignal.set(completedTempSignal)

                                                            // Remove rawData from hub's endpoint
                                                            getRawData(hubUID).removeValue()
                                                        } else {
                                                            Log.w("RealtimeDatabase", "sendListenAction2 - Mismatch in chunk list size: " +
                                                                    "expected $numChunks but actually ${rawData.size}")
                                                        }
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
                                })
                            }
                    }
            }

    }

    @SuppressLint("SimpleDateFormat")
    fun sendSignalToHub(hubUID: String, irSignal: IrSignal): Task<Void> {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid

        Log.d("TEST", "Sending Signal to $hubUID")
        FirebaseDatabase.getInstance().reference
            .child("devices")
            .child(hubUID)
            .child("rawData")
            .setValue(ArrayList<String>()
                .apply {
                    for (i in 0 until irSignal.rawData.size) {
                        add(irSignal.rawData[i])
                    }
                 }
            )

        return FirebaseDatabase.getInstance().reference
            .child("devices")
            .child(hubUID)
            .child("action")
            .setValue(
                HubAction().apply {
                    type = IR_ACTION_SEND
                    sender = uid
                    timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().time)
                    rawLen = irSignal.rawLength
                })
    }

    @SuppressLint("SimpleDateFormat")
    fun sendNoneAction(hubUID: String): Task<Void> {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid

        return FirebaseDatabase.getInstance().reference
            .child("devices")
            .child(hubUID)
            .child("action")
            .setValue(
                HubAction().apply {
                    type = IR_ACTION_NONE
                    sender = uid
                    timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().time)
                }
            )
    }

    @SuppressLint("SimpleDateFormat")
    fun sendListenAction(hubUID: String): Task<Void> {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid

        return FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("action")
            .setValue(
                HubAction().apply {
                    type = IR_ACTION_LISTEN
                    sender = uid
                    timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().time)
                }
            )
    }

    @SuppressLint("LogNotTimber")
    fun sendCommandToHub(command: Command?) {
        Log.d("sendCommandToHub", "defaultHub = ${AppState.userData.user.defaultHub}")
        doAsync {
            when (command) {
                null -> { Log.w("sendCommandToHub", "call with null commands") }
                else -> {
                    val actions = ArrayList(command.actions)
                    sendNextSignalToHub(actions, AppState.userData.user.defaultHub ?: "")
                }
            }
        }
    }

    /**
     * Sends a list of commands sequentially by removing the first action from actions (the stack) and
     * recursively calling this function until actions.size = 0
     */
    @SuppressLint("LogNotTimber")
    private fun sendNextSignalToHub(actions: ArrayList<Command.Action>, defaultHubUID: String) {
        if (actions.size == 0)
            return

        val action = actions.removeAt(0)
        val destHub = if (action.hubUID == DEFAULT_HUB) defaultHubUID else action.hubUID
        Log.d("TEST", "DestHub = $destHub (defaultHubUID = $defaultHubUID)")
        when (destHub) {
            "" -> { Log.e("sendCommandToHub", "empty default hubUID for action ${AppState.userData.irSignals[action.irSignal]?.name}") }
            else -> {
                when (val signal = AppState.userData.irSignals[action.irSignal]) {
                    null -> {
                        Log.e("sendCommandToHub", "Null irSignal passed for action ${action.irSignal}")
                        FirestoreActions.getIrSignal(action.irSignal)
                            .addOnFailureListener { Log.e("sendCommandToHub", "$it") }
                            .addOnSuccessListener {
                                if (it.data == null)
                                    Log.d("sendCommandToHub", "data was null (exists = ${it.exists()}")
                                val sig = it.toObject(IrSignal::class.java)
                                    ?.apply {
                                        uid = action.irSignal
                                    } ?: run { Log.d("sendCommandToHub", "Didn't find signal ${it.id}"); return@addOnSuccessListener}
                                Log.d("sendCommandToHub", "Got missing IR Signal: ${sig.uid}")
                                AppState.userData.irSignals[sig.uid] = sig
                                Log.d("sendNextSignalToHub", "Send sig: $sig")
                                sendSignalToHub(destHub, sig)
                                    .addOnFailureListener {  Log.e("sendCommandToHub", "Failed to send ${AppState.userData.irSignals[action.irSignal]?.name} to hub $destHub")}
                                    .addOnSuccessListener {
                                        Log.d("TEST", "Action sent!")
                                        //TODO: Change action backend implementation to better support a list of actions that the Hub reads at its own pace, one at a time
                                        if (actions.size > 0)
                                            Timer().schedule(object : TimerTask() {
                                                override fun run() {
                                                    sendNextSignalToHub(actions, defaultHubUID)
                                                }
                                            }, actions[0].delay.toLong())
                                    }
                            }
                    }
                    else -> {
                        Log.d("sendNextSignalToHub", "Send sig: $signal")
                        sendSignalToHub(destHub, signal)
                            .addOnFailureListener {  Log.e("sendCommandToHub", "Failed to send ${AppState.userData.irSignals[action.irSignal]?.name} to hub $destHub")}
                            .addOnSuccessListener {
                                Log.d("TEST", "Action sent!")
                                //TODO: Change action backend implementation to better support a list of actions that the Hub reads at its own pace, one at a time
                                if (actions.size > 0)
                                    Timer().schedule(object : TimerTask() {
                                        override fun run() {
                                            sendNextSignalToHub(actions, defaultHubUID)
                                        }
                                    }, actions[0].delay.toLong())
                            }
                    }
                }
            }
        }
    }

    fun getHubAvailability(hubUID: String) : DatabaseReference {
        return FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("action").child("sender")
    }

    fun clearResult(hubUID: String): Task<Void> {
        return FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("result")
            .removeValue()
    }

    fun getHubRef(hubUID: String): DatabaseReference {
        return FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("result")
    }

    fun getHubResults(hubUID: String): DatabaseReference {
        return FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("result")
    }

    /**
     * Queries for rawData from designated hub. Uses rawLen to calculate how many chunks of
     * data to query for.
     */
    fun getRawData(hubUID: String): DatabaseReference {
        return FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("rawData")
    }

    @SuppressLint("UseSparseArrays", "LogNotTimber")
    @Suppress("UNCHECKED_CAST")
    fun parseRawData(value: Any?): ArrayList<String>? {
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

    @SuppressLint("LogNotTimber")
    @Suppress("UNCHECKED_CAST")
    fun parseHubResult(value: Any?): HubResult? {
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

    fun calculateNumChunks(rawLength: Int): Int {
        val numChunks : Int = (rawLength / CHUNK_SIZE)
        return if (numChunks * CHUNK_SIZE < rawLength) numChunks+1 else numChunks
    }

    private const val CHUNK_SIZE = 50



/*
----------------------------------------------
  Hub Errors
----------------------------------------------
*/
    open class HubException(val titleID : Int, val messageID : Int, message : String? = "") : java.lang.Exception(message)
    class HubOverflowException(message : String? = "") : HubException(
        com.ms8.smartirhub.android.R.string.err_overflow_title,
        com.ms8.smartirhub.android.R.string.err_overflow_desc,
        message)
    class HubTimeoutException(message : String? = "") : HubException(
        com.ms8.smartirhub.android.R.string.err_timeout_title,
        com.ms8.smartirhub.android.R.string.err_timeout_desc,
        message)
    class HubUnknownException(message : String? = "") : HubException(
        com.ms8.smartirhub.android.R.string.err_title,
        com.ms8.smartirhub.android.R.string.err_unknown_desc,
        message)
}