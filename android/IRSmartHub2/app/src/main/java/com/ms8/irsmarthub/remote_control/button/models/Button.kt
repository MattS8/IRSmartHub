package com.ms8.irsmarthub.remote_control.button.models

import android.util.Log
import com.ms8.irsmarthub.remote_control.command.models.Command
import java.lang.Exception

class Button(typeTemp: Type) {
    var properties: ArrayList<Properties> = ArrayList()
    var commands: ArrayList<Command> = ArrayList()
    var columnSpan: Int = 1
    var rowSpan: Int = 1
    var type: Type = typeTemp
        set(value) {
            field = value

            setupPropertiesAndCommands()
        }

    init {
        setupPropertiesAndCommands()
    }

    private fun setupPropertiesAndCommands() {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun toFirebaseObject() : Map<String, Any?> {
        return HashMap<String, Any?>()
            .apply {
                put("properties", ArrayList<Map<String, Any?>>()
                    .apply {
                        properties.forEach { p ->
                            add(p.toFirebaseObject())
                        }
                    })
                put("commands", ArrayList<Map<String, Any?>>()
                    .apply {
                        commands.forEach { c ->
                            add(c.toFirebaseObject())
                        }
                    })
                put("type", type.ordinal)
                put("columnSpan", columnSpan)
                put("rowSpan", rowSpan)
            }
    }

    companion object {

        @Suppress("UNCHECKED_CAST")
        fun fromFirebaseObject(buttonMap: Map<String, Any?>): Button {
            val type = Type.values().associateBy(Type::ordinal)[(buttonMap["type"] as Number?)?.toInt()]
                ?: Type.BASIC
            return Button(type)
                .apply {
                    if (buttonMap.containsKey("rowSpan"))
                        rowSpan = (buttonMap["rowSpan"] as Number).toInt()
                    if (buttonMap.containsKey("columnSpan"))
                        columnSpan = (buttonMap["columnSpan"] as Number).toInt()
                    if (buttonMap.containsKey("properties"))
                    {
                        properties.clear()
                        try {
                            (buttonMap["properties"] as List<Map<String, Any?>>).forEach { p ->
                                properties.add(Properties.fromFirebaseObject(p))
                            }
                        } catch (e: Exception) { Log.e("Button", "$e") }
                    }
                    if (buttonMap.containsKey("commands"))
                    {
                        try {
                            (buttonMap["commands"] as List<Map<String, Any?>>).forEach { c ->
                                commands.add(Command.copyFrom(c))
                            }
                        } catch (e: Exception) {Log.e("Button", "$e") }
                    }
                }
        }

        enum class Type {
            BASIC,
            STYLE_BTN_INCREMENTER_VERTICAL,
            STYLE_SPACE,
            STYLE_BTN_RADIAL,
            STYLE_BTN_RADIAL_W_CENTER,
            STYLE_CREATE_BUTTON
        }
    }
}