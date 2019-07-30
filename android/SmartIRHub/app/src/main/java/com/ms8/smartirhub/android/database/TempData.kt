package com.ms8.smartirhub.android.database

import com.ms8.smartirhub.android.models.firestore.IrSignal
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile

object TempData {
    var tempRemoteProfile   : RemoteProfile =
        RemoteProfile()
    var tempButton          : RemoteProfile.Button? = null
    var tempSignal          : IrSignal?             = null
}