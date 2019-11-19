package com.ms8.smartirhub.android._tests.dev_playground

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.TestBinding
import com.ms8.smartirhub.android.firebase.RealtimeDatabaseFunctions
import com.ms8.smartirhub.android.remote_control.command.creation.CommandCreator
import com.ms8.smartirhub.android.remote_control.button.creation.ButtonCreator
import com.ms8.smartirhub.android.remote_control.button.models.Button
import com.ms8.smartirhub.android.remote_control.views.RemoteLayout

class TestEnvActivity : AppCompatActivity() {
    enum class TestType {COMMAND_CREATOR, BUTTON_CREATOR, REMOTE_LAYOUT}

    private val testType = TestType.REMOTE_LAYOUT

    private val commandCreator = CommandCreator()
    private val buttonCreator = ButtonCreator()
    private lateinit var binding: TestBinding
    private var remoteLayout : RemoteLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout._test)

        when (testType) {
            TestType.COMMAND_CREATOR -> setupCommandCreator()
            TestType.BUTTON_CREATOR -> setupButtonCreator()
            TestType.REMOTE_LAYOUT -> setupRemoteLayout()
        }
    }

    private fun setupRemoteLayout() {
        AppState.tempData.tempRemoteProfile.apply {
            name = "Test Remote"
            inEditMode.set(true)

            buttons.addAll(arrayListOf(
                Button(Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND).apply {
                    properties[0].text = "1"
                },
                Button(Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND).apply {
                    properties[0].text = "2"
                },
                Button(Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND).apply {
                    properties[0].text = "3"
                },
                Button(Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND).apply {
                    properties[0].text = "4"
                },
                Button(Button.Companion.ButtonStyle.STYLE_BTN_INCREMENTER_VERTICAL).apply {
                    properties[1].text = "VOL"
                },
                Button(Button.Companion.ButtonStyle.STYLE_BTN_RADIAL_W_CENTER).apply {
                    properties[4].text = "OK"
                },
                Button(Button.Companion.ButtonStyle.STYLE_BTN_INCREMENTER_VERTICAL).apply {
                    properties[1].text = "CH"
                },
                Button(Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND).apply {
                    properties[0].text = "5"
                },
                Button(Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND).apply {
                    properties[0].text = "6"
                },
                Button(Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND).apply {
                    properties[0].text = "7"
                },
                Button(Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND).apply {
                    properties[0].text = "8"
                },
                Button(Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND).apply {
                    properties[0].text = "9"
                },
                Button(Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND).apply {
                    properties[0].text = "10"
                },
                Button(Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND).apply {
                    properties[0].text = "11"
                },
                Button(Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND).apply {
                    properties[0].text = "12"
                    properties[0].bgTint = "#EF5350"
                }
            ))
        }
        remoteLayout = RemoteLayout(this)
        remoteLayout?.apply {
            setupAdapter()
            onAddNewButton = { buttonCreator.showBottomDialog() }
        }

        binding.testRoot.addView(remoteLayout?.getRemoteView())

        //Handler().postDelayed({AppState.tempData.tempRemoteProfile.buttons.clear()}, 5000)
    }

    private fun setupButtonCreator() {
        binding.testRoot.addView(android.widget.Button(this).apply {
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_outline_white_ripple)
            setText(R.string.test_button_creator)
            setOnClickListener { buttonCreator.showBottomDialog() }
        })
    }

    private fun setupCommandCreator() {
        binding.apply {
            testRoot.addView(android.widget.Button(this@TestEnvActivity).apply {
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_outline_white_ripple)
                setText(R.string.test_command_creator)
                setOnClickListener { commandCreator.showBottomDialog(0) }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        commandCreator.context = this
        buttonCreator.context = this
        RealtimeDatabaseFunctions.debugMode = true

        when (testType) {
            TestType.REMOTE_LAYOUT ->
            {
                Log.d("TEST", "Start Listening")
                remoteLayout?.startListening()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        commandCreator.context = null
        buttonCreator.context = null

        when (testType) {
            TestType.REMOTE_LAYOUT ->
            {
                Log.d("TEST", "Stop listening...")
                remoteLayout?.stopListening()
            }
        }
    }
}
