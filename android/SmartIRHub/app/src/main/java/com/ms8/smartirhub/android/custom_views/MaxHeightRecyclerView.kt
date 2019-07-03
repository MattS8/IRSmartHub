package com.ms8.smartirhub.android.custom_views

import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import androidx.recyclerview.widget.RecyclerView
import org.jetbrains.anko.dip
import org.jetbrains.anko.windowManager

class MaxHeightRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {
    fun getCustomMaxHeight() : Int {
        val displayMetrics = DisplayMetrics()
        displayMetrics.heightPixels = 0
        context?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        var height = displayMetrics.heightPixels
        height = if (height <= 0){
            500
        }
        else {
            height - (height * .3).toInt()
        }

        return height
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val newHeightSpec = MeasureSpec.makeMeasureSpec(getCustomMaxHeight(), MeasureSpec.AT_MOST)
        super.onMeasure(widthSpec, newHeightSpec)
    }
}