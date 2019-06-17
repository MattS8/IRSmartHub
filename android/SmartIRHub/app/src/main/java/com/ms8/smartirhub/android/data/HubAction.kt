package com.ms8.smartirhub.android.data

class HubAction(val type: Int, val sender: String, val timestamp: String) {
    var repeat = false
    var rawData = ""
    var rawLen = 0
}