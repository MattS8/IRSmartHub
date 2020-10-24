package com.ms8.irsmarthub.models

import android.annotation.SuppressLint
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import com.ms8.irsmarthub.firebase.RealtimeDatabaseFunctions.calculateNumChunks

@SuppressLint("UseSparseArrays")
@IgnoreExtraProperties
class IrSignal {
    var rawData         : ArrayList<String>     = ArrayList()
    var name            : String                = ""
    var code            : String                = ""
    var repeat          : Boolean               = false
    var rawLength       : Int                   = 0
    var encodingType    : Int                   = 0

    @get:Exclude
    var uid : String = ""


    fun resetData() {
        rawData = ArrayList()
        rawLength = 0
        encodingType = 0
        code = ""
        repeat = false
    }

    fun rawDataToString(): String {
        var retStr = ""

        for (i in 0 until (rawData.size)) {
            retStr += rawData[i]
        }

        return retStr
    }

    fun toFirebaseObject(ownerUID: String): MutableMap<String, Any> {
        val firebaseObject = HashMap<String, Any>()

        firebaseObject["owner"] = ownerUID
        firebaseObject["name"] = name
        firebaseObject["rawLength"] = rawLength
        firebaseObject["encodingType"] = encodingType
        firebaseObject["code"] = code
        firebaseObject["repeat"] = repeat
//        firebaseObject["rawData"] = HashMap<String, String>().apply {
//            for (i in 0 until rawData.size) {
//                set(i.toString(), rawData[i] ?: "")
//            }
//        }
        firebaseObject["rawData"] = ArrayList<String>().apply {
            for (i in 0 until rawData.size) {
                add(rawData[i])
            }
        }

        return firebaseObject
    }

    fun containsAllRawData() : Boolean {
        return rawData.size == calculateNumChunks(rawLength)
    }

    override fun toString(): String {
        return "Signal: (name = $name, code = $code, encodingType = $encodingType, rawLength = $rawLength, rawdata = ${rawDataToString()}, repeat = $repeat)"
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
    }
}