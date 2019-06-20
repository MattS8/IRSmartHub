package com.ms8.smartirhub.android.firebase

import android.annotation.SuppressLint
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ms8.smartirhub.android.data.HubAction
import com.ms8.smartirhub.android.data.IrSignal
import com.ms8.smartirhub.android.firebase.FirebaseConstants.IR_ACTION_NONE
import com.ms8.smartirhub.android.firebase.FirebaseConstants.IR_ACTION_SEND
import java.text.SimpleDateFormat
import java.util.*

object RealtimeDatabaseFunctions {

    @SuppressLint("SimpleDateFormat")
    fun sendSignalToHub(hubUID: String, irSignal: IrSignal): Task<Void> {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        return FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("action")
            .setValue(HubAction(IR_ACTION_SEND, uid,
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().time))
                .apply {
                    rawData = irSignal.rawData
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

    fun clearResult(hubUID: String): Task<Void> {
        return FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("result")
            .removeValue()
    }

    fun getHubRef(hubUID: String): DatabaseReference {
        return FirebaseDatabase.getInstance().reference.child("devices").child(hubUID).child("result")
    }
}