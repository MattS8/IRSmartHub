package com.ms8.irsmarthub.remote_control.button.models

import android.util.Log
import com.ms8.irsmarthub.remote_control.command.models.Command
import com.ms8.irsmarthub.remote_control.button.models.Properties.Companion.BgStyle
import com.ms8.irsmarthub.remote_control.button.models.Properties.Companion.IMG_ADD
import com.ms8.irsmarthub.remote_control.button.models.Properties.Companion.IMG_RADIAL_DOWN
import com.ms8.irsmarthub.remote_control.button.models.Properties.Companion.IMG_RADIAL_LEFT
import com.ms8.irsmarthub.remote_control.button.models.Properties.Companion.IMG_RADIAL_RIGHT
import com.ms8.irsmarthub.remote_control.button.models.Properties.Companion.IMG_RADIAL_UP
import com.ms8.irsmarthub.remote_control.button.models.Properties.Companion.IMG_SUBTRACT
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
// different button types require different number of properties/commands
        // this ensures whenever the type is changed, there are enough property/command variables to support the selected type
        when (type) {
            // no property/command needed
            Type.STYLE_SPACE ->
            {
                columnSpan = 1
                rowSpan = 1
                properties.clear()
                commands.clear()
            }
            Type.STYLE_CREATE_BUTTON ->
            {
                Log.e("Button", "setupPropertiesAndCommands - setting up properties and commands for 'Create Button'... this should not happen!")
                columnSpan = 1
                rowSpan = 4
                properties.clear()
                commands.clear()
            }
            // 1 property/command needed
            Type.BASIC -> {
                columnSpan = 1
                rowSpan = 1
                properties.clear().also { properties.add(
                    Properties()
                        .apply {
                            bgStyle = BgStyle.BG_CIRCLE
//                            marginTop = 8
//                            marginBottom = 8
                        }) }
                commands.clear().also { commands.add(Command()) }
            }
            Type.STYLE_BTN_NO_MARGIN ->
            {
                columnSpan = 2
                rowSpan = 1
                properties.clear().also { properties.add(
                    Properties()
                        .apply {
                            bgStyle = BgStyle.BG_ROUND_RECT
                            marginTop = 8
                            marginBottom = 8
                        }) }
                commands.clear().also { commands.add(Command()) }
            }
            // 2 properties/commands needed
            Type.STYLE_BTN_INCREMENTER_VERTICAL ->
            {
                rowSpan = 2
                columnSpan = 1
                properties.clear().also { properties.addAll(
                    listOf(
                        Properties()
                            .apply {
                                bgStyle = BgStyle.BG_ROUND_RECT_TOP
                                image = IMG_ADD
                                marginBottom = 0
                            },
                        Properties()
                            .apply {
                                bgStyle = BgStyle.BG_ROUND_RECT
                                marginStart = 0
                                marginEnd = 0
                                marginTop = 4
                                marginBottom = 4
                            },
                        Properties()
                            .apply {
                                bgStyle = BgStyle.BG_ROUND_RECT_BOTTOM
                                image = IMG_SUBTRACT
                                marginTop = 0
                            })) }
                commands.clear().also { commands.addAll(listOf(Command(), Command())) }
            }
            // 4 properties/commands needed
            Type.STYLE_BTN_RADIAL ->
            {
                rowSpan = 2
                columnSpan = 2
                properties.clear().also { properties.addAll(listOf(
                    Properties()
                        .apply {
                            bgStyle = BgStyle.BG_RADIAL_TOP
                            image = IMG_RADIAL_UP
                        },
                    Properties()
                        .apply {
                            bgStyle = BgStyle.BG_RADIAL_END
                            image = IMG_RADIAL_RIGHT
                        },
                    Properties()
                        .apply {
                            bgStyle = BgStyle.BG_RADIAL_BOTTOM
                            image = IMG_RADIAL_DOWN
                        },
                    Properties()
                        .apply {
                            bgStyle = BgStyle.BG_RADIAL_START
                            image = IMG_RADIAL_LEFT
                        })) }
                commands.clear().also { commands.addAll(listOf(Command(), Command(), Command(), Command())) }
            }
            // 5 properties/commands needed
            Type.STYLE_BTN_RADIAL_W_CENTER ->
            {
                rowSpan = 2
                columnSpan = 2
                properties.clear().also { properties.addAll(listOf(
                    Properties()
                        .apply {
                            bgStyle = BgStyle.BG_RADIAL_TOP
                            image = IMG_RADIAL_UP
                        },
                    Properties()
                        .apply {
                            bgStyle = BgStyle.BG_RADIAL_END
                            image = IMG_RADIAL_RIGHT
                        },
                    Properties()
                        .apply {
                            bgStyle = BgStyle.BG_RADIAL_BOTTOM
                            image = IMG_RADIAL_DOWN
                        },
                    Properties()
                        .apply {
                            bgStyle = BgStyle.BG_RADIAL_START
                            image = IMG_RADIAL_LEFT
                        },
                    Properties()
                        .apply {
                            bgStyle = BgStyle.BG_RADIAL_CENTER
                        }
                )) }
                commands.clear().also { commands.addAll(listOf(Command(), Command(), Command(), Command(), Command())) }
            }
        }
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
            STYLE_BTN_NO_MARGIN,
            STYLE_BTN_INCREMENTER_VERTICAL,
            STYLE_SPACE,
            STYLE_BTN_RADIAL,
            STYLE_BTN_RADIAL_W_CENTER,
            STYLE_CREATE_BUTTON
        }
    }
}