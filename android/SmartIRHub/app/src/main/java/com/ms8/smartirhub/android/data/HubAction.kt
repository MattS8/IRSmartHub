package com.ms8.smartirhub.android.data

class HubAction(val type: Int, val sender: String, val timestamp: String) {
    var repeat = 0
    var rawData = ""
    var rawLen :Long = 0
}