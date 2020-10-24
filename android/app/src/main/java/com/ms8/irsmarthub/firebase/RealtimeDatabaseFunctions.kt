package com.ms8.irsmarthub.firebase

import com.google.android.gms.tasks.Task
import com.google.firebase.database.FirebaseDatabase
import com.ms8.irsmarthub.R
import com.ms8.irsmarthub.database.AppState
import com.ms8.irsmarthub.firebase.FirebaseConstants.IR_ACTION_NONE
import com.ms8.irsmarthub.models.HubAction
import java.text.SimpleDateFormat

object IRFunctions {
    enum class Command {
        TOGGLE_TV, TURN_ON, TURN_OFF, TOGGLE_MUTE, VOL_UP, VOL_DOWN
    }

    fun sendCommand(command: Command) {
        //TODO
        clearResult()
            .addOnFailureListener { e -> AppState.errorData.pairSignalError.set(HubUnknownException(e.message)) }
            .addOnSuccessListener { sendNoneAction() }
    }

    fun listenForCommand() {
        // TODO - ensure this can be hardcoded for now...
        val uid = 0


    }

    fun sendNoneAction() : Task<Void> {
        // TODO - ensure this can be hardcoded for now...
        val uid = 0

        return actionEndpoit().setValue(HubAction().apply {
            type = IR_ACTION_NONE
            sender = uid
            timestamp = SimpleDateFormat
        })
    }

    /**
     * Removes any data from the result endpoint
     */
    fun clearResult(): Task<Void> {
        return deviceEndpoint().child("result").removeValue()
    }


    /** Endpoints **/

    private fun deviceEndpoint() = FirebaseDatabase.getInstance().reference
            .child("devices")
            .child(AppState.hubUID)

    private fun actionEndpoit() = deviceEndpoint().child("action")

    private fun rawDataEndpoint() = deviceEndpoint()
            .child("rawData")

    private fun hubResultEndpoint() = deviceEndpoint()
            .child("raw")
}