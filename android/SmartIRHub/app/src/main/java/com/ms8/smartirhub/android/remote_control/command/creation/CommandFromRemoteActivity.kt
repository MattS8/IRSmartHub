package com.ms8.smartirhub.android.remote_control.command.creation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ms8.smartirhub.android.R

class CommandFromRemoteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_command_from_remote)
    }

    companion object {
        const val EXTRA_REMOTE_UID = "EXTRAS_REMOTE_UID"
    }
}
