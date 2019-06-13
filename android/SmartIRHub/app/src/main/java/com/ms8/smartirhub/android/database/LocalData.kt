package com.ms8.smartirhub.android.database

import androidx.databinding.ObservableArrayMap
import com.ms8.smartirhub.android.data.Group
import com.ms8.smartirhub.android.data.RemoteProfile
import com.ms8.smartirhub.android.data.User

object LocalData {
    var user : User? = null
    val userGroups = ObservableArrayMap<String, Group>()
    val remoteProfiles = ObservableArrayMap<String, RemoteProfile>()

    fun setupUser(user: User, username: String) {
        this.user = user
        this.user!!.username = username
    }

    fun removeUserData() {
        user = null
        userGroups.clear()
        remoteProfiles.clear()
    }
}