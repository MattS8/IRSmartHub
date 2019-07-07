package com.ms8.smartirhub.android.utils

import android.content.Context
import com.google.gson.Gson
import com.ms8.smartirhub.android.models.firestore.User

object MySharedPreferences {
    fun setUser(context: Context, user : User?) {
        context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(USER_DATA, Gson().toJson(user))
            .apply()
    }

    fun hasSeenSplash(context: Context): Boolean {
        return context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
            .getBoolean(B_SEEN_SPLASH, false)
    }

    fun setHasSeenSplash(context: Context, bHasSeenSplash: Boolean) {
        context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(B_SEEN_SPLASH, bHasSeenSplash)
            .apply()
    }

    fun removeUser(context: Context) {
        context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(USER_NAME)
            .remove(USER_DATA)
            .apply()
    }

    private const val SHARED_PREFS = "MY_SHARED_PREFS"
    private const val USER_NAME = "USER_NAME"
    private const val USER_DATA = "USER_DATA"
    private const val B_SEEN_SPLASH = "B_SEEN_SPLASH"
}