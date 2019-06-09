package com.ms8.smartirhub.android.database

import android.databinding.ObservableArrayMap
import com.ms8.smartirhub.android.data.Group
import com.ms8.smartirhub.android.data.User

object LocalData {
    var userData : User? = null
    val userGroups = ObservableArrayMap<String, Group>()

    fun setUserData(user: User, username: String) {
        userData = user
        userData!!.username = username
    }

    fun removeUserData() {
        userData = null
        userGroups.clear()
    }
}