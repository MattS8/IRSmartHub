package com.ms8.smartirhub.android.database

import com.ms8.smartirhub.android.data.Command
import com.ms8.smartirhub.android.data.IrSignal
import com.ms8.smartirhub.android.data.RemoteProfile

object TempData {
    var tempRemoteProfile = RemoteProfile()
    var tempButton: RemoteProfile.Button? = null
    var tempSignal: IrSignal? = null
}