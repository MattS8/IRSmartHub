package com.ms8.smartirhub.android._tests.dev_playground.remote_layout

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import kotlinx.android.synthetic.main.test__remote_layout.*
import kotlin.math.roundToInt

class TestRemoteLayout : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test__remote_layout)

//        remoteLayout.remoteProfile = RemoteProfile()
//            .apply {
//                for (i in 0 until 19)
//                    buttons.add(RemoteProfile.Button()
//                        .apply {
//                            name = "Button $i"
//                        }
//                    )
//            }


        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        //val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels
        //remoteLayout.setRequestedColumnWidth(((1 / 5) * width.toFloat()).roundToInt())
        remoteLayout.setRequestedColumnCount(4)
        createMockData()
        remoteLayout.setupAdapter()
    }

    private fun createMockData() {
        TempData.tempRemoteProfile.buttons
            .apply {
                for (i in 0 until 50) {
                    add(
                        RemoteProfile.Button()
                            .apply {
                                name = "Button $i"
                                when (i) {
                                    0,1  -> {
                                        columnSpan = 2
                                    }
                                    2 -> {
                                        rowSpan = 2
                                        style = RemoteProfile.Button.STYLE_BTN_INCREMENTER_VERTICAL
                                        properties[0].bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_ROUND_RECT_TOP
                                        properties[0].marginTop = 16
                                        properties[0].marginStart = 16
                                        properties[0].marginEnd = 16
                                        properties[0].marginBottom = 0
                                        properties[0].image = RemoteProfile.Button.IMG_ADD

                                        properties.add(
                                            RemoteProfile.Button.Properties()
                                                .apply {
                                                    bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_ROUND_RECT_BOTTOM
                                                    marginTop = 0
                                                    marginStart = 16
                                                    marginBottom = 16
                                                    marginEnd = 16
                                                    image = RemoteProfile.Button.IMG_SUBTRACT
                                                })

                                        name = "VOL"
                                    }
                                    4 -> {
                                        rowSpan = 2
                                        style = RemoteProfile.Button.STYLE_BTN_INCREMENTER_VERTICAL
                                        properties[0].bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_ROUND_RECT_TOP
                                        properties[0].marginTop = 16
                                        properties[0].marginStart = 16
                                        properties[0].marginEnd = 16
                                        properties[0].marginBottom = 0
                                        properties[0].image = RemoteProfile.Button.IMG_ADD

                                        properties.add(
                                            RemoteProfile.Button.Properties()
                                                .apply {
                                                    bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_ROUND_RECT_BOTTOM
                                                    marginTop = 0
                                                    marginStart = 16
                                                    marginBottom = 16
                                                    marginEnd = 16
                                                    image = RemoteProfile.Button.IMG_SUBTRACT
                                                })

                                        name = "CH"
                                    }
                                    3 -> {
                                        rowSpan = 2
                                        columnSpan = 2
                                        style = RemoteProfile.Button.STYLE_BTN_RADIAL_W_CENTER

                                        // add topButton Properties
                                        properties[0].bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_NONE
                                        properties[0].marginTop = 16
                                        properties[0].marginStart = 0
                                        properties[0].marginEnd = 0
                                        properties[0].marginBottom = 0
                                        properties[0].image = RemoteProfile.Button.IMG_RADIAL_UP

                                        // add endButton Properties
                                        properties.add(
                                            RemoteProfile.Button.Properties()
                                                .apply {
                                                    bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_NONE
                                                    marginTop = 0
                                                    marginStart = 0
                                                    marginEnd = 16
                                                    marginBottom = 0
                                                    image = RemoteProfile.Button.IMG_RADIAL_RIGHT
                                                })
                                        // add bottomButton Properties
                                        properties.add(
                                            RemoteProfile.Button.Properties()
                                                .apply {
                                                    bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_NONE
                                                    marginTop = 0
                                                    marginStart = 0
                                                    marginEnd = 0
                                                    marginBottom = 16
                                                    image = RemoteProfile.Button.IMG_RADIAL_DOWN
                                                })
                                        // add startButton Properties
                                        properties.add(
                                            RemoteProfile.Button.Properties()
                                                .apply {
                                                    bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_NONE
                                                    marginTop = 0
                                                    marginStart = 16
                                                    marginEnd = 0
                                                    marginBottom = 0
                                                    image = RemoteProfile.Button.IMG_RADIAL_LEFT
                                                })
                                        // add centerButton Properties
                                        properties.add(
                                            RemoteProfile.Button.Properties()
                                                .apply {
                                                    bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_CIRCLE
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
