package com.ms8.smartirhub.android._tests.dev_playground.remote_layout

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.models.firestore.RemoteProfile
import kotlinx.android.synthetic.main.test__remote_layout.*

class TestRemoteLayout : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test__remote_layout)

        remoteLayout.remoteProfile = RemoteProfile()
            .apply {
                for (i in 0 until 19)
                    buttons.add(RemoteProfile.Button()
                        .apply {
                            name = "Button $i"
                        }
                    )
            }

    }
}
