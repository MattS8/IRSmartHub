package com.ms8.irsmarthub.remote_control.command.models

import com.google.firebase.database.DataSnapshot
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude

class IrSignal (
    var rawData: ArrayList<String> = ArrayList(),
    var name: String = "",
    var code: String = "",
    var repeat: Boolean = false,
    var rawLength: Int = 0,
    var encodingType: Int = 0
) {
    @get:Exclude
    var uid: String = ""

    fun toFirebaseObject(ownerUID: String): MutableMap<String, Any> {
        val firebaseObject = HashMap<String, Any>()

        firebaseObject["owner"] = ownerUID
        firebaseObject["name"] = name
        firebaseObject["rawLength"] = rawLength
        firebaseObject["encodingType"] = encodingType
        firebaseObject["code"] = code
        firebaseObject["repeat"] = repeat
        firebaseObject["rawData"] = ArrayList<String>().apply {
            for (i in 0 until rawData.size) {
                add(rawData[i])
            }
        }

        return firebaseObject
    }

    companion object {
        fun copyFrom(oldSignal : IrSignal?) : IrSignal {
            return IrSignal()
                .apply {
                    oldSignal?.let {
                        rawData = it.rawData
                        name = it.name
                        code = it.code
                        repeat = it.repeat
                        rawLength = it.rawLength
                        encodingType = it.encodingType
                        uid = it.uid
                    }
                }
        }

        fun copyFrom(snapshot: DocumentSnapshot?) : IrSignal? {
            val newIrSignal = snapshot?.toObject(IrSignal::class.java)
                ?: return null
            newIrSignal.uid = snapshot.id

            return newIrSignal
        }
    }
}