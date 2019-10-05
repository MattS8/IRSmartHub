package com.ms8.smartirhub.android.utils

import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.remote_control.button.models.Button

object DebugUtils {

    fun createMockData() {
        AppState.tempData.tempRemoteProfile.inEditMode.set(true)
        AppState.tempData.tempRemoteProfile.buttons
            .apply {
                for (i in 0 until 50) {
                    add(
                        Button(Button.Companion.ButtonStyle.STYLE_BTN_INCREMENTER_VERTICAL)
                            .apply {
                                name = "B $i"
                                when (i) {
                                    0,1  -> {
                                        columnSpan = 2
                                    }
                                    2 -> {
                                        rowSpan = 2
                                        type = Button.Companion.ButtonStyle.STYLE_BTN_INCREMENTER_VERTICAL
                                        properties[0].bgStyle = Button.Companion.BgStyle.BG_ROUND_RECT_TOP
                                        properties[0].marginTop = 16
                                        properties[0].marginStart = 16
                                        properties[0].marginEnd = 16
                                        properties[0].marginBottom = 0
                                        properties[0].image = Button.IMG_ADD

                                        properties.add(
                                            this.Properties()
                                                .apply {
                                                    bgStyle = Button.Companion.BgStyle.BG_ROUND_RECT_BOTTOM
                                                    marginTop = 0
                                                    marginStart = 16
                                                    marginBottom = 16
                                                    marginEnd = 16
                                                    image = Button.IMG_SUBTRACT
                                                })

                                        name = "VOL"
                                    }
                                    4 -> {
                                        rowSpan = 2
                                        type = Button.Companion.ButtonStyle.STYLE_BTN_INCREMENTER_VERTICAL
                                        properties[0].bgStyle = Button.Companion.BgStyle.BG_ROUND_RECT_TOP
                                        properties[0].marginTop = 16
                                        properties[0].marginStart = 16
                                        properties[0].marginEnd = 16
                                        properties[0].marginBottom = 0
                                        properties[0].image = Button.IMG_ADD

                                        properties.add(
                                            this.Properties()
                                                .apply {
                                                    bgStyle = Button.Companion.BgStyle.BG_ROUND_RECT_BOTTOM
                                                    marginTop = 0
                                                    marginStart = 16
                                                    marginBottom = 16
                                                    marginEnd = 16
                                                    image = Button.IMG_SUBTRACT
                                                })

                                        name = "CH"
                                    }
                                    3 -> {
                                        rowSpan = 2
                                        columnSpan = 2
                                        type = Button.Companion.ButtonStyle.STYLE_BTN_RADIAL_W_CENTER

                                        // add topButton Properties
                                        properties[0].bgStyle = Button.Companion.BgStyle.BG_NONE
                                        properties[0].marginTop = 16
                                        properties[0].marginStart = 0
                                        properties[0].marginEnd = 0
                                        properties[0].marginBottom = 0
                                        properties[0].image = Button.IMG_RADIAL_UP

                                        // add endButton Properties
                                        properties.add(
                                            this.Properties()
                                                .apply {
                                                    bgStyle = Button.Companion.BgStyle.BG_NONE
                                                    marginTop = 0
                                                    marginStart = 0
                                                    marginEnd = 16
                                                    marginBottom = 0
                                                    image = Button.IMG_RADIAL_RIGHT
                                                })
                                        // add bottomButton Properties
                                        properties.add(
                                            this.Properties()
                                                .apply {
                                                    bgStyle = Button.Companion.BgStyle.BG_NONE
                                                    marginTop = 0
                                                    marginStart = 0
                                                    marginEnd = 0
                                                    marginBottom = 16
                                                    image = Button.IMG_RADIAL_DOWN
                                                })
                                        // add startButton Properties
                                        properties.add(
                                            this.Properties()
                                                .apply {
                                                    bgStyle = Button.Companion.BgStyle.BG_NONE
                                                    marginTop = 0
                                                    marginStart = 16
                                                    marginEnd = 0
                                                    marginBottom = 0
                                                    image = Button.IMG_RADIAL_LEFT
                                                })
                                        // add centerButton Properties
                                        properties.add(
                                            this.Properties()
                                                .apply {
                                                    bgStyle = Button.Companion.BgStyle.BG_CIRCLE
                                                    marginTop = 0
                                                    marginStart = 0
                                                    marginEnd = 0
                                                    marginBottom = 0
                                                })

                                        name = "OK"
                                    }
                                }
                            })
                }
            }
    }
}