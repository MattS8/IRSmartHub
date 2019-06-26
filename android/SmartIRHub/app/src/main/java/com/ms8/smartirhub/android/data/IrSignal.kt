package com.ms8.smartirhub.android.data

import android.annotation.SuppressLint

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
}
