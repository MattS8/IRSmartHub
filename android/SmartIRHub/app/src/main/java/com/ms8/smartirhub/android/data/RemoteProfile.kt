package com.ms8.smartirhub.android.data

import android.util.ArrayMap

class RemoteProfile {
    val buttons = ArrayMap<Int, Button>()
    var name = ""

    class Button(var id : Int) {
        var name = ""
        var style = ""
    }
}