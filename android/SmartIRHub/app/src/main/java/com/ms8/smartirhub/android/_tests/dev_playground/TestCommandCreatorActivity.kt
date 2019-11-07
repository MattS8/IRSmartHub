package com.ms8.smartirhub.android._tests.dev_playground

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.firebase.RealtimeDatabaseFunctions
import com.ms8.smartirhub.android.remote_control.command.creation.CommandCreator

class TestCommandCreatorActivity : AppCompatActivity() {
    private val commandCreator = CommandCreator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout._test)

        findViewById<Button>(R.id.btnTest).apply {
            setText(R.string.test_command_creator)
            setOnClickListener { commandCreator.showBottomDialog(0) }
        }
    }

    override fun onResume() {
        super.onResume()
        commandCreator.context = this
        RealtimeDatabaseFunctions.debugMode = true
    }

    override fun onPause() {
        super.onPause()
        commandCreator.context = null
    }
}
