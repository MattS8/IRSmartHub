package com.ms8.smartirhub.android.remote_control.button.models

import android.annotation.SuppressLint
import android.util.ArrayMap
import android.util.Log
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile

@Suppress("UNCHECKED_CAST")
class Button {
    var properties      : ArrayList<Properties> = ArrayList<Properties>().apply { add(
        Properties()
    ) }
    var commands        : ArrayList<RemoteProfile.Command>    = ArrayList<RemoteProfile.Command>().apply { add(
        RemoteProfile.Command()
    ) }
    var name            : String                = ""
    var style           : Int                   =
        STYLE_BTN_SINGLE_ACTION
    var columnSpan      : Int                   = 1
    var rowSpan         : Int                   = 1


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
                put("name", name)
                put("style", style)
                put("columnSpan", columnSpan)
                put("rowSpan", rowSpan)
            }
    }

    companion object {
        fun newCommandList(): java.util.ArrayList<RemoteProfile.Command> = ArrayList<RemoteProfile.Command>().apply { add(
            RemoteProfile.Command()
        ) }

        @SuppressLint("LogNotTimber")
        fun fromMap(buttonMap: Map<String, Any?>): Button {
            val newButton = Button()
                .apply {
                    name = buttonMap["name"] as String
                    style = (buttonMap["style"] as Number).toInt()
                    rowSpan = (buttonMap["rowSpan"] as Number).toInt()
                    columnSpan = (buttonMap["columnSpan"] as Number).toInt()
                }

            // set properties
            if (buttonMap.containsKey("properties")) {
                try {
                    newButton.properties.clear()
                    (buttonMap["properties"] as List<Map<String, Any?>>).forEach { p ->
                        newButton.properties.add(
                            Properties.fromMap(
                                p
                            )
                        )
                    }
                } catch (e : Exception) { Log.e("Button", "$e") }
            }

            // set commands
            if (buttonMap.containsKey("commands")) {
                try {
                    (buttonMap["commands"] as List<Map<String, Any?>>).forEach { c ->
                        newButton.commands.add(RemoteProfile.Command.fromMap(c))
                    }
                } catch (e : Exception) { Log.e("Button", "$e") }
            }

            return newButton
        }

        const val STYLE_CREATE_BUTTON = 0
        const val STYLE_SPACE = 1
        const val STYLE_BTN_SINGLE_ACTION = 2
        const val STYLE_BTN_NO_MARGIN = 3
        const val STYLE_BTN_INCREMENTER_VERTICAL = 4
        const val STYLE_BTN_RADIAL_W_CENTER = 5
        const val STYLE_BTN_RADIAL = 6

        const val ID_BUTTONS = 80839
        const val ID_NAME = 80840

        const val ADD_TO_END = -1

        const val IMG_ADD           = "_IMG_ADD_"
        const val IMG_SUBTRACT      = "_IMG_SUBTRACT_"
        const val IMG_RADIAL_LEFT   = "_IMG_RADIAL_LEFT"
        const val IMG_RADIAL_UP     = "_IMG_RADIAL_UP"
        const val IMG_RADIAL_DOWN   = "_IMG_RADIAL_DOWN"
        const val IMG_RADIAL_RIGHT  = "_IMG_RADIAL_RIGHT"
    }

    class Properties {
        var bgStyle         : BgStyle =
            BgStyle.BG_CIRCLE
        var bgUrl           : String    = ""
        var image           : String    = ""
        var marginBottom    : Int       = 16
        var marginTop       : Int       = 16
        var marginStart     : Int       = 16
        var marginEnd       : Int       = 16

        enum class BgStyle {
            BG_INVISIBLE,
            BG_CIRCLE,
            BG_ROUND_RECT,
            BG_ROUND_RECT_TOP,
            BG_ROUND_RECT_BOTTOM,
            BG_CUSTOM_IMAGE,
            BG_NONE
        }

        fun toFirebaseObject() : Map<String, Any?> {
            return ArrayMap<String, Any?>()
                .apply {
                    put("bgStyle", bgStyle.ordinal)
                    put("bgUrl", bgUrl)
                    put("image", image)
                    put("marginBottom", marginBottom)
                    put("marginTop", marginTop)
                    put("marginStart", marginStart)
                    put("marginEnd", marginEnd)
                }
        }

        companion object {
            fun fromMap(propertiesMap: Map<String, Any?>) : Properties {
                return Properties()
                    .apply {
                        if (propertiesMap.containsKey("bgUrl")) {
                            bgUrl = propertiesMap["bgUrl"] as String
                        }

                        if (propertiesMap.containsKey("image")) {
                            image = propertiesMap["image"] as String
                        }

                        marginEnd = (propertiesMap["marginEnd"] as Number).toInt()
                        marginBottom = (propertiesMap["marginBottom"] as Number).toInt()
                        marginTop = (propertiesMap["marginTop"] as Number).toInt()
                        marginStart = (propertiesMap["marginStart"] as Number).toInt()

                        bgStyle =
                            toBgStyle(
                                (propertiesMap["bgStyle"] as Number).toInt()
                            )
                    }
            }

            @SuppressLint("LogNotTimber")
            fun toBgStyle(intVal : Int) : BgStyle {
                return when(intVal) {
                    BgStyle.BG_INVISIBLE.ordinal -> BgStyle.BG_INVISIBLE
                    BgStyle.BG_CIRCLE.ordinal -> BgStyle.BG_CIRCLE
                    BgStyle.BG_ROUND_RECT.ordinal -> BgStyle.BG_ROUND_RECT
                    BgStyle.BG_ROUND_RECT_TOP.ordinal -> BgStyle.BG_ROUND_RECT_TOP
                    BgStyle.BG_ROUND_RECT_BOTTOM.ordinal -> BgStyle.BG_ROUND_RECT_BOTTOM
                    BgStyle.BG_CUSTOM_IMAGE.ordinal -> BgStyle.BG_CUSTOM_IMAGE
                    BgStyle.BG_NONE.ordinal -> BgStyle.BG_NONE

                    else -> {
                        Log.e("BgStyle", "Received unknown int value ($intVal)")
                        BgStyle.BG_NONE
                    }
                }
            }
        }


    }
}