package com.ms8.smartirhub.android._tests.dev_playground.misc

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.firebase.FirestoreActions.TEST_REMOTE
import com.ms8.smartirhub.android.models.firestore.Hub.Companion.DEFAULT_HUB
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.IMG_ADD
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.IMG_SUBTRACT
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.STYLE_BTN_INCREMENTER_VERTICAL

class TestBackendActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_backend)

        Log.d("TEST###", "User UID: ${FirebaseAuth.getInstance().currentUser?.uid}")



        // Send _Test_Remote
        val testRemote = RemoteProfile()
            .apply {
                name = "_TEST_REMOTE"
                buttons.add(RemoteProfile.Button())
                buttons.add(RemoteProfile.Button()
                    .apply {
                        properties[0] = RemoteProfile.Button.Properties()
                            .apply {
                                bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_ROUND_RECT
                                name = "B0"
                            }
                        commands.add(RemoteProfile.Command()
                            .apply {
                                actions.add(RemoteProfile.Command.Action()
                                    .apply {
                                        hubUID = DEFAULT_HUB
                                        irSignal = "_TEST_SIGNAL"
                                    })
                            })
                        name = "TEST BUTTON 1"
                    })
                buttons.add(RemoteProfile.Button()
                    .apply {
                        name = "Test Incr"
                        properties[0] = RemoteProfile.Button.Properties()
                            .apply {
                                marginBottom = 0
                                image = IMG_ADD
                                bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_ROUND_RECT_TOP
                            }
                        properties.add(RemoteProfile.Button.Properties()
                            .apply {
                                marginTop = 0
                                image = IMG_SUBTRACT
                                bgStyle = RemoteProfile.Button.Properties.BgStyle.BG_ROUND_RECT_BOTTOM
                            })
                        commands.add(RemoteProfile.Command()
                            .apply {
                                name = "Test Up"
                            })
                        commands.add(RemoteProfile.Command()
                            .apply {
                                name = "Test Down"
                            })
                        rowSpan = 2
                        style = STYLE_BTN_INCREMENTER_VERTICAL
                    })
            }
        testRemote.uid = TEST_REMOTE
        FirestoreActions.updateRemote(testRemote)
            .addOnSuccessListener {
                Log.w("TEST###", "Added successfully!")
            }.addOnFailureListener { e ->
                Log.e("TEST###", "Failed... $e")
            }
    }
}
