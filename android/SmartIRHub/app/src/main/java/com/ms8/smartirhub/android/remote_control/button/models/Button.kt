package com.ms8.smartirhub.android.remote_control.button.models

import android.annotation.SuppressLint
import android.content.Context
import android.util.ArrayMap
import android.util.Log
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile

@Suppress("UNCHECKED_CAST")
class Button(typeTemp : ButtonStyle) {
    var properties      : ArrayList<Properties>                 = ArrayList()
    var commands        : ArrayList<RemoteProfile.Command>      = ArrayList()
    var name            : String                                = ""
    var columnSpan      : Int                                   = 1
    var rowSpan         : Int                                   = 1
    var type            : ButtonStyle                           = typeTemp
        set(value) {
        field = value

       setupPropertiesAndCommands()
    }

    init {
        setupPropertiesAndCommands()
    }

    @SuppressLint("LogNotTimber")
    private fun setupPropertiesAndCommands() {
        // different button types require different number of properties/commands
        // this ensures whenever the type is changed, there are enough property/command variables to support the selected type
        when (type) {
            // no property/command needed
            ButtonStyle.STYLE_SPACE ->
            {
                columnSpan = 1
                rowSpan = 1
                properties.clear()
                commands.clear()
            }
            ButtonStyle.STYLE_CREATE_BUTTON ->
            {
                Log.e("Button", "setupPropertiesAndCommands - setting up properties and commands for 'Create Button'... this should not happen!")
                columnSpan = 1
                rowSpan = 4
                properties.clear()
                commands.clear()
            }
            // 1 property/command needed
            ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND -> {
                columnSpan = 1
                rowSpan = 1
                properties.clear().also { properties.add(
                    Properties()
                        .apply {
                            bgStyle = BgStyle.BG_CIRCLE
//                            marginTop = 8
//                            marginBottom = 8
                        }) }
                commands.clear().also { commands.add(RemoteProfile.Command()) }
            }
            ButtonStyle.STYLE_BTN_NO_MARGIN ->
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
                commands.clear().also { commands.add(RemoteProfile.Command()) }
            }
            // 2 properties/commands needed
            ButtonStyle.STYLE_BTN_INCREMENTER_VERTICAL ->
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
                commands.clear().also { commands.addAll(listOf(RemoteProfile.Command(), RemoteProfile.Command())) }
            }
            // 4 properties/commands needed
            ButtonStyle.STYLE_BTN_RADIAL ->
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
                commands.clear().also { commands.addAll(listOf(RemoteProfile.Command(), RemoteProfile.Command(), RemoteProfile.Command(), RemoteProfile.Command())) }
            }
            // 5 properties/commands needed
            ButtonStyle.STYLE_BTN_RADIAL_W_CENTER ->
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
                commands.clear().also { commands.addAll(listOf(RemoteProfile.Command(), RemoteProfile.Command(), RemoteProfile.Command(), RemoteProfile.Command(), RemoteProfile.Command())) }
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
                put("name", name)
                put("type", type.value)
                put("columnSpan", columnSpan)
                put("rowSpan", rowSpan)
            }
    }

    fun propertiesFromMap(propertiesMap: Map<String, Any?>) : Properties {
        return Properties()
            .apply {
                if (propertiesMap.containsKey("bgUrl"))
                    bgUrl = propertiesMap["bgUrl"] as String
                if (propertiesMap.containsKey("image"))
                    image = propertiesMap["image"] as String
                if (propertiesMap.containsKey("marginEnd"))
                    marginEnd = (propertiesMap["marginEnd"] as Number).toInt()
                if (propertiesMap.containsKey("marginBottom"))
                    marginBottom = (propertiesMap["marginBottom"] as Number).toInt()
                if (propertiesMap.containsKey("marginTop"))
                    marginTop = (propertiesMap["marginTop"] as Number).toInt()
                if (propertiesMap.containsKey("marginStart"))
                    marginStart = (propertiesMap["marginStart"] as Number).toInt()
                if (propertiesMap.containsKey("bgStyle"))
                    bgStyle = intToBgStyle(
                            (propertiesMap["bgStyle"] as Number).toInt()
                        )
                if (propertiesMap.containsKey("text"))
                    text = propertiesMap["text"] as String
                if (propertiesMap.containsKey("bgTint"))
                    bgTint = propertiesMap["bgTint"] as String
            }
    }

    companion object {
        fun newCommandList(): java.util.ArrayList<RemoteProfile.Command> = ArrayList<RemoteProfile.Command>().apply { add(
            RemoteProfile.Command()
        ) }

        @SuppressLint("LogNotTimber")
        fun fromMap(buttonMap: Map<String, Any?>): Button {
            val newButton = Button(buttonStyleFromInt((buttonMap["type"] as Number).toInt()) ?: ButtonStyle.STYLE_CREATE_BUTTON)
                .apply {
                    name = buttonMap["name"] as String
                    rowSpan = (buttonMap["rowSpan"] as Number).toInt()
                    columnSpan = (buttonMap["columnSpan"] as Number).toInt()
                }

            // set properties
            if (buttonMap.containsKey("properties")) {
                try {
                    newButton.properties.clear()
                    (buttonMap["properties"] as List<Map<String, Any?>>).forEach { p ->
                        newButton.properties.add(
                            newButton.propertiesFromMap(p)
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

        //NOTE: STYLE_CREATE_BUTTON must always be the last one as it is excluded from lists showing available buttons to choose from
        enum class ButtonStyle(val value: Int) {STYLE_BTN_SINGLE_ACTION_ROUND(0), STYLE_BTN_NO_MARGIN(1), STYLE_BTN_INCREMENTER_VERTICAL(2), STYLE_SPACE(3), STYLE_BTN_RADIAL(4), STYLE_BTN_RADIAL_W_CENTER(5), STYLE_CREATE_BUTTON(6)}
        fun buttonStyleFromInt(stateAsInt: Int) = ButtonStyle.values().associateBy(ButtonStyle::value)[stateAsInt]

        enum class BgStyle {
            BG_INVISIBLE,
            BG_CIRCLE,
            BG_ROUND_RECT,
            BG_ROUND_RECT_TOP,
            BG_ROUND_RECT_BOTTOM,
            BG_CUSTOM_IMAGE,
            BG_RADIAL_TOP,
            BG_RADIAL_END,
            BG_RADIAL_BOTTOM,
            BG_RADIAL_START,
            BG_RADIAL_CENTER,
            BG_NONE
        }

        fun nameFromStyle(context: Context, style: ButtonStyle) : String {
            return when (style) {
                ButtonStyle.STYLE_CREATE_BUTTON -> context.getString(R.string.create_button_title)
                ButtonStyle.STYLE_SPACE -> context.getString(R.string.button_space_title)
                ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND -> context.getString(R.string.button_round_title)
                ButtonStyle.STYLE_BTN_NO_MARGIN -> context.getString(R.string.button_full_title)
                ButtonStyle.STYLE_BTN_INCREMENTER_VERTICAL -> context.getString(R.string.button_incrementer_title)
                ButtonStyle.STYLE_BTN_RADIAL_W_CENTER -> context.getString(R.string.button_radial_w_center_title)
                ButtonStyle.STYLE_BTN_RADIAL -> context.getString(R.string.button_radial_title)
            }
        }

        fun imageResourceFromStyle(style: ButtonStyle) : Int {
            return when (style) {
                ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND -> R.drawable.btn_bg_circle
                ButtonStyle.STYLE_BTN_INCREMENTER_VERTICAL -> R.drawable.btn_template_incrementer
                ButtonStyle.STYLE_BTN_RADIAL -> R.drawable.btn_template_radial
                ButtonStyle.STYLE_SPACE -> 0
                ButtonStyle.STYLE_BTN_NO_MARGIN -> R.drawable.btn_bg_round_rect
                ButtonStyle.STYLE_BTN_RADIAL_W_CENTER -> R.drawable.btn_template_radial_w_center_button
                ButtonStyle.STYLE_CREATE_BUTTON -> R.drawable.btn_bg_round_rect
            }
        }



        @SuppressLint("LogNotTimber")
        fun intToBgStyle(intVal : Int) : BgStyle {
            return when(intVal) {
                BgStyle.BG_INVISIBLE.ordinal -> BgStyle.BG_INVISIBLE
                BgStyle.BG_CIRCLE.ordinal -> BgStyle.BG_CIRCLE
                BgStyle.BG_ROUND_RECT.ordinal -> BgStyle.BG_ROUND_RECT
                BgStyle.BG_ROUND_RECT_TOP.ordinal -> BgStyle.BG_ROUND_RECT_TOP
                BgStyle.BG_ROUND_RECT_BOTTOM.ordinal -> BgStyle.BG_ROUND_RECT_BOTTOM
                BgStyle.BG_CUSTOM_IMAGE.ordinal -> BgStyle.BG_CUSTOM_IMAGE
                BgStyle.BG_RADIAL_TOP.ordinal -> BgStyle.BG_RADIAL_TOP
                BgStyle.BG_RADIAL_END.ordinal -> BgStyle.BG_RADIAL_END
                BgStyle.BG_RADIAL_BOTTOM.ordinal -> BgStyle.BG_RADIAL_BOTTOM
                BgStyle.BG_RADIAL_START.ordinal -> BgStyle.BG_RADIAL_START
                BgStyle.BG_RADIAL_CENTER.ordinal -> BgStyle.BG_RADIAL_CENTER
                BgStyle.BG_NONE.ordinal -> BgStyle.BG_NONE

                else -> {
                    Log.e("BgStyle", "Received unknown int value ($intVal)")
                    BgStyle.BG_NONE
                }
            }
        }

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

    inner class Properties {
        var bgStyle         : BgStyle   = BgStyle.BG_CIRCLE
        var bgUrl           : String    = ""
        var image           : String    = ""
        var marginBottom    : Int       = 16
        var marginTop       : Int       = 16
        var marginStart     : Int       = 16
        var marginEnd       : Int       = 16
        var text            : String    = ""
        var bgTint          : String    = ""

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
                    put("text", text)
                    put("bgTint", bgTint)
                }
        }
    }
}