package com.ms8.irsmarthub.remote_control.command.models

data class Command(
    var actions: ArrayList<Action> = ArrayList()
) {

    fun toFirebaseObject() : Map<String, Any?> {
        return HashMap<String, Any?>()
            .apply {
                put("actions", ArrayList<Map<String, Any?>>()
                    .apply {
                        actions.forEach { a ->
                            add(a.toFirebaseObject())
                        }
                    })
            }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun copyFrom(commandMap : Map<String, Any?>) : Command {
            return Command()
                .apply {
                    if (commandMap.containsKey("actions")) {
                        (commandMap["actions"] as List<Map<String, Any?>>).forEach { a ->
                            actions.add(Action.copyFrom(a))
                        }
                    }
                }
        }
    }
}