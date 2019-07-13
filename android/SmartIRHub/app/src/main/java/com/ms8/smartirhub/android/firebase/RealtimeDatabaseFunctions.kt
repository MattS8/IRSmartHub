package com.ms8.smartirhub.android.firebase

import android.annotation.SuppressLint
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ms8.smartirhub.android.models.firestore.RemoteProfile.Command
import com.ms8.smartirhub.android.models.realtimedatabase.HubAction
import com.ms8.smartirhub.android.models.realtimedatabase.HubResult
import com.ms8.smartirhub.android.models.firestore.IrSignal
import com.ms8.smartirhub.android.database.LocalData
import com.ms8.smartirhub.android.firebase.FirebaseConstants.IR_ACTION_LISTEN
import com.ms8.smartirhub.android.firebase.FirebaseConstants.IR_ACTION_NONE
import com.ms8.smartirhub.android.firebase.FirebaseConstants.IR_ACTION_SEND
import com.ms8.smartirhub.android.models.firestore.Hub.Companion.DEFAULT_HUB
import org.jetbrains.anko.doAsync
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object RealtimeDatabaseFunctions {

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
        Log.d("sendCommandToHub", "defaultHub = ${LocalData.user?.defaultHub}")
        doAsync {
            when (command) {
                null -> { Log.w("sendCommandToHub", "call with null command") }
                else -> {
                    val actions = ArrayList(command.actions)
                    sendNextSignalToHub(actions, LocalData.user?.defaultHub ?: "")
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
            "" -> { Log.e("sendCommandToHub", "empty default hubUID for action ${LocalData.signals[action.irSignal]?.name}") }
            else -> {
                when (val signal = LocalData.signals[action.irSignal]) {
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
                                LocalData.signals[sig.uid] = sig
                                Log.d("sendNextSignalToHub", "Send sig: $sig")
                                sendSignalToHub(destHub, sig)
                                    .addOnFailureListener {  Log.e("sendCommandToHub", "Failed to send ${LocalData.signals[action.irSignal]?.name} to hub $destHub")}
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
                            .addOnFailureListener {  Log.e("sendCommandToHub", "Failed to send ${LocalData.signals[action.irSignal]?.name} to hub $destHub")}
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
            return ArrayList<String>()
                .apply {
                    for (i in 0 until tempMap.size)
                        add(tempMap[i] as String)
                }
        } catch (e : Exception) { Log.e("parseRawData", "$e")}

        try {
            return value as ArrayList<String>
        }
        catch (e : Exception) { Log.e("parseRawData", "$e")}

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
        } catch (e : Exception) { Log.e("parseHubResult", "$e")}

        return null
    }

    fun calculateNumChunks(rawLength: Int): Int {
        val numChunks : Int = (rawLength / CHUNK_SIZE)
        return if (numChunks * CHUNK_SIZE < rawLength) numChunks+1 else numChunks
    }

    private const val CHUNK_SIZE = 50
}