package com.ms8.smartirhub.android.data

import android.annotation.SuppressLint

class HubResult(val resultCode: Int, val timestamp: String) {
    @SuppressLint("UseSparseArrays")
    var rawLen : Int = 0

    var encoding = 0
    var code = ""

}