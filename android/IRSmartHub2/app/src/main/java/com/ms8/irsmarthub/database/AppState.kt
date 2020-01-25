package com.ms8.irsmarthub.database

import androidx.databinding.ObservableArrayMap
import com.ms8.irsmarthub.database.models.TempRemote
import com.ms8.irsmarthub.database.models.TempUser
import com.ms8.irsmarthub.remote_control.remote.models.Remote

object AppState {
    val tempData: TempData = TempData()

    data class TempData(
        val tempRemote: TempRemote = TempRemote(),
        val tempUser: TempUser = TempUser(),
        val tempRemoteList: ObservableArrayMap<String, Remote> = ObservableArrayMap()
    )
}