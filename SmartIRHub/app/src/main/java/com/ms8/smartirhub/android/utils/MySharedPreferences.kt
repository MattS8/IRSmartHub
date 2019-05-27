package com.ms8.smartirhub.android.utils

import android.content.Context

object MySharedPreferences {

    fun hasUsername(context: Context) : Boolean {
        return context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
            .getString(USER_NAME, "") != ""
    }

    fun setUsername(context: Context, username: String?) {
        context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(USER_NAME, username)
            .apply()
    }

    private const val SHARED_PREFS = "MY_SHARED_PREFS"
    private const val USER_NAME = "USER_NAME"
}