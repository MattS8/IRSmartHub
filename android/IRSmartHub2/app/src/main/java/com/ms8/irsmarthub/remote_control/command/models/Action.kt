package com.ms8.irsmarthub.remote_control.command.models

import com.ms8.irsmarthub.hub.models.Hub.Companion.DEFAULT_HUB

data class Action(
    var hubUID: String = DEFAULT_HUB,
    var irSignal: String = "",
    var delay: Int = 0) {

    fun toFirebaseObject() : Map<String, Any?> {
        return HashMap<String, Any?>()
            .apply {
                put("hubUID", hubUID)
                put("irSignal", irSignal)
                if (delay != 0)
                    put("delay", delay)
            }
    }

    companion object {
        fun copyFrom(actionMap : Map<String, Any?>) : Action {
            return Action()
                .apply {
                    hubUID = actionMap["hubUID"] as String
                    irSignal = actionMap["irSignal"] as String
                    if (actionMap.containsKey("delay"))
                        delay = (actionMap["delay"] as Number).toInt()
                }
        }
    }
}