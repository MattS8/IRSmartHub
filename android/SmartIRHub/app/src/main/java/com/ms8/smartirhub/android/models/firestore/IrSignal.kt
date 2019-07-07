package com.ms8.smartirhub.android.models.firestore

import android.annotation.SuppressLint
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@SuppressLint("UseSparseArrays")
@IgnoreExtraProperties
class IrSignal {
    var rawData         : HashMap<Int, String>  = HashMap()
    var name            : String                = ""
    var code            : String                = ""
    var repeat          : Boolean               = false
    var rawLength       : Int                   = 0
    var encodingType    : Int                   = 0

    @get:Exclude
    var uid : String = ""


    fun resetData() {
        rawData = HashMap()
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
        firebaseObject["rawData"] = HashMap<String, String>().apply {
            for (i in 0 until rawData.size) {
                set(i.toString(), rawData[i] ?: "")
            }
        }

        return firebaseObject
    }
}
