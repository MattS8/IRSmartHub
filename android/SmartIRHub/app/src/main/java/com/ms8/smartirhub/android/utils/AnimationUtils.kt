package com.ms8.smartirhub.android.utils

import android.animation.Animator
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator

object AnimationUtils {
    fun getCircularAnimator(targetView: View, sourceX: Int, sourceY: Int, speed: Long): Animator {
        val finalRadius = Math.hypot(targetView.width.toDouble(), targetView.height.toDouble()).toFloat()
        return ViewAnimationUtils.createCircularReveal(targetView, sourceX, sourceY, 0f, finalRadius).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = speed
        }
    }

    fun View.getCenter() : Pair<Float, Float> {
        return Pair(x + width/2, y + height/2)
    }
}