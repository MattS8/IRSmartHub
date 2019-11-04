package com.ms8.smartirhub.android.remote_control.command.creation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ms8.smartirhub.android.R

class GetFromRemoteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_command_from_remote)
    }

    companion object {
        enum class ResultType {COMMAND, ACTIONS}
        const val EXTRA_TYPE = "EXTRAS_RESULT_TYPE"
        const val EXTRA_COMMAND_UID = "EXTRAS_COMMAND_UID"
        const val EXTRA_REMOTE_UID = "EXTRAS_REMOTE_UID"
    }
}
