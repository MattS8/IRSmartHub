package com.ms8.smartirhub.android.data

import androidx.databinding.ObservableArrayList

class Command {
    var actions = ObservableArrayList<Action>()

    class Action {
        var hubUID = DEFAULT_HUB
        var irSignal = ""
        var delay = 0
    }

    companion object {
        const val DEFAULT_HUB = "_default_hub_"
    }
}