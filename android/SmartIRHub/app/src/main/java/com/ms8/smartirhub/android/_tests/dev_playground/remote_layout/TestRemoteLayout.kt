package com.ms8.smartirhub.android._tests.dev_playground.remote_layout

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import com.ms8.smartirhub.android.R
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
        remoteLayout.setupAdapter()
    }
}
