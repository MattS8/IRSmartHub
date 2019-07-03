package com.ms8.smartirhub.android.firebase

import android.annotation.SuppressLint
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ms8.smartirhub.android.data.Command
import com.ms8.smartirhub.android.data.Command.Companion.DEFAULT_HUB
import com.ms8.smartirhub.android.data.HubAction
import com.ms8.smartirhub.android.data.HubResult
import com.ms8.smartirhub.android.data.IrSignal
import com.ms8.smartirhub.android.database.LocalData
import com.ms8.smartirhub.android.firebase.FirebaseConstants.IR_ACTION_NONE
import com.ms8.smartirhub.android.firebase.FirebaseConstants.IR_ACTION_SEND
import org.jetbrains.anko.doAsync
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.schedule

object RealtimeDatabaseFunctions {

    @SuppressLint("SimpleDateFormat")
    fun sendSignalToHub(hubUID: String, irSignal: IrSignal): Task<Void> {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val tempMap = HashMap<String, String>()
        irSignal.rawData.keys.forEach { key ->
            tempMap[key.toString()] = irSignal.rawData[key] ?: ""
        }
        FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("rawData")
            .setValue(tempMap)
        return FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("action")
            .setValue(HubAction(IR_ACTION_SEND, uid,
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().time))
                .apply {
                    rawLen = irSignal.rawLength
                })
    }

    @SuppressLint("SimpleDateFormat")
    fun sendNoneAction(hubUID: String): Task<Void> {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        return FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("action")
            .setValue(HubAction(IR_ACTION_NONE, uid,
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().time)))
    }

    @SuppressLint("SimpleDateFormat")
    fun sendListenAction(hubUID: String): Task<Void> {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        return FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("action")
            .setValue(HubAction(FirebaseConstants.IR_ACTION_LISTEN, uid, SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().time)))
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
        when (val destHub : String = if (action.hubUID == DEFAULT_HUB) defaultHubUID else action.hubUID) {
            "" -> { Log.e("sendCommandToHub", "empty default hubUID for action ${LocalData.irSignals[action.irSignal]?.name}") }
            else -> {
                when (val signal = LocalData.irSignals[action.irSignal]) {
                    null -> Log.e("sendCommandToHub", "Null irSignal passed for action ${action.irSignal}")
                    else -> {
                        sendSignalToHub(destHub, signal)
                            .addOnFailureListener {  Log.e("sendCommandToHub", "Failed to send ${LocalData.irSignals[action.irSignal]?.name} to hub $destHub")}
                            .addOnSuccessListener {
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

    @SuppressLint("LogNotTimber")
    fun sendCommandToHub(command: Command?, defaultHub: String?) {
        doAsync {
            when (command) {
                null -> { Log.w("sendCommandToHub", "call with null command") }
                else -> {
                    val actions = ArrayList(command.actions)
                    sendNextSignalToHub(actions, defaultHub ?: "")
                }
            }
        }
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
    fun parseRawData(value: Any?): HashMap<Int, String>? {
        try {
            val tempMap = value as HashMap<Any, Any>
            tempMap.remove("numChunks")
            return tempMap as HashMap<Int, String>?
        } catch (e : Exception) { Log.e("parseRawData", "$e")}

        try {
            val arrayList = value as ArrayList<String>
            val map = HashMap<Int, String>()
            for (i in 0 until arrayList.size) {
                map[i] = arrayList[i]
            }

            return map
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

            val hubResult = HubResult(resultCode.toInt(), timestamp).apply {
                this.encoding = encoding.toInt()
                this.rawLen = rawLen.toInt()
                this.code = code
            }
//            Log.d("parseHubResult", "Gson got: $hubResult")
            return hubResult
        } catch (e : Exception) { Log.e("parseHubResult", "$e")}

        return null
    }

    fun calculateNumChunks(rawLength: Int): Int {
        val numChunks : Int = (rawLength / CHUNK_SIZE)
        //Log.d("calculateNumChunks", "rawLen = $rawLength, CHUNK_SIZE = $CHUNK_SIZE, numChunks = $numChunks")
        return if (numChunks * CHUNK_SIZE < rawLength) numChunks+1 else numChunks
    }

    const val CHUNK_SIZE = 50
}