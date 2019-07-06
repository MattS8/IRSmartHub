package com.ms8.smartirhub.android.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.util.Log
import java.lang.Exception

object TypefaceCache {
    private val fontCache = HashMap<String, Typeface>()


    fun get(name : String, context: Context) = fontCache[name] ?: loadTypeface(name, context)

    @SuppressLint("LogNotTimber")
    private fun loadTypeface (name: String, context: Context) : Typeface? {
        return try {
            Typeface.createFromAsset(context.assets, name)
        } catch (e : Exception) {
            Log.w("TypefaceCache", "$e")
            null
        }
    }
}