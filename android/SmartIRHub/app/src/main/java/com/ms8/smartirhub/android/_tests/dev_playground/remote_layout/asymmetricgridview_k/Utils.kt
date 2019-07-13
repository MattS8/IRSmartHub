package com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview_k

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

object Utils {
    fun dpToPx(context: Context, dp: Float): Int {
        // Took from http://stackoverflow.com/questions/8309354/formula-px-to-dp-dp-to-px-android
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    internal fun getScreenWidth(context: Context?): Int {
        return if (context == null) {
            0
        } else getDisplayMetrics(context).widthPixels
    }

    /**
     * Returns a valid DisplayMetrics object
     *
     * @param context valid context
     * @return DisplayMetrics object
     */
    internal fun getDisplayMetrics(context: Context): DisplayMetrics {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        return metrics
    }
}