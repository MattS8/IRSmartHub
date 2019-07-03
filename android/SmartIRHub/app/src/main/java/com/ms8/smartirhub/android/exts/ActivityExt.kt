package com.ms8.smartirhub.android.exts

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Color.TRANSPARENT
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import com.ms8.smartirhub.android.data.RemoteProfile
import com.ms8.smartirhub.android.database.TempData

val AppCompatActivity.hasSourceBounds: Boolean get() = intent?.sourceBounds != null

fun AppCompatActivity.sourceBounds(boundsAction: (Rect) -> Unit) {
    intent?.sourceBounds?.let(boundsAction)
}

/**
 * Disables upcoming transition when [hasSourceBounds] is true. Should be called before [AppCompatActivity.onCreate]
 */
fun AppCompatActivity.preAnimationSetup() {
    if (hasSourceBounds) {
        overridePendingTransition(0, 0)
    }
}

/**
 * @return [ValueAnimator] that animates status bar color from [TRANSPARENT] to current status bar color
 */
val AppCompatActivity.statusBarAnimator: Animator
    get() = ValueAnimator.ofArgb(TRANSPARENT, window.statusBarColor)
        .animatedValue(window::setStatusBarColor)

/**
 * @return [ValueAnimator] that animates navigation bar color from [TRANSPARENT] to current navigation bar color
 */
val AppCompatActivity.navigationBarAnimator: Animator
    get() = ValueAnimator.ofArgb(TRANSPARENT, window.navigationBarColor)
        .animatedValue(window::setNavigationBarColor)

fun AppCompatActivity.startCreateButtonProcess() {
    TempData.tempButton = RemoteProfile.Button()

}