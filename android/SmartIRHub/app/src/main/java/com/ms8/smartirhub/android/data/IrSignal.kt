package com.ms8.smartirhub.android.data

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
class IrSignal {

    fun rawDataToString(): String {
        var retStr = ""

        for (i in 0 until (rawData.size)) {
            retStr += rawData[i]
        }

        return retStr
    }

    var name = ""
    var rawLength = 0
    @SuppressLint("UseSparseArrays")
    var rawData = HashMap<Int, String>()
    var encodingType = 0
    var code = ""
    var repeat = false

    @get:Exclude
    var uid = ""

    fun resetData() {
        rawData = HashMap()
        rawLength = 0
        encodingType = 0
        code = ""
        repeat = false
    }

    fun toFirebaseObject(ownerUID: String): MutableMap<String, Any> {
        val firebaseObject = HashMap<String, Any>()

        firebaseObject["owner"] = ownerUID
        firebaseObject["name"] = name
        firebaseObject["rawLength"] = rawLength
        firebaseObject["encodingType"] = encodingType
        firebaseObject["code"] = code
        firebaseObject["repeat"] = repeat
        val rawDataStrHash = HashMap<String, String>()
        for (i in 0 until rawData.size) {
            Log.d("TEST", "Setting rawDataStrHash[$i] to ${rawData[i]}")
            rawDataStrHash[i.toString()] = rawData[i] ?: ""
        }
        firebaseObject["rawData"] = rawDataStrHash


        return firebaseObject
    }
}
