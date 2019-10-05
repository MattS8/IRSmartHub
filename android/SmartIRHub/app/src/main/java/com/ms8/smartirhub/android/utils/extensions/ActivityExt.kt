package com.ms8.smartirhub.android.utils.extensions

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Color.TRANSPARENT
import android.graphics.Rect
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.andrognito.flashbar.Flashbar
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.remote_control.button.models.Button


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

fun AppCompatActivity.findNavBarHeight(): Int {
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        resources.getDimensionPixelSize(resourceId)
    } else 0
}

fun Activity.getStatusBarHeight() : Int {
    val rectangle = Rect()
    window.decorView.getWindowVisibleDisplayFrame(rectangle)
    return rectangle.top
}

fun Context.getNavBarHeight() : Int {
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    Log.d("NAVBAR", "height = $resourceId")
    return if (resourceId > 0)
        resources.getDimensionPixelSize(resourceId)
    else 0
}

fun AppCompatActivity.getGenericErrorFlashbar(showPositiveAction : Boolean = false) = Flashbar.Builder(this)
    .gravity(Flashbar.Gravity.BOTTOM)
    .showOverlay()
    .backgroundColorRes(R.color.colorCardDark)
    .messageColorRes(android.R.color.holo_red_dark)
    .enableSwipeToDismiss()
    .dismissOnTapOutside()
    .duration(Flashbar.DURATION_LONG)
    .apply {
        if (showPositiveAction) {
            positiveActionText(R.string.dismiss)
            positiveActionTapListener(object : Flashbar.OnActionTapListener {
                override fun onActionTapped(bar: Flashbar) {
                    bar.dismiss()
                }
            })
        }
    }

fun AppCompatActivity.getGenericComingSoonFlashbar() = Flashbar.Builder(this)
    .gravity(Flashbar.Gravity.BOTTOM)
    .showOverlay()
    .message(R.string.coming_soon)
    .backgroundColorRes(R.color.colorCardDark)
    .messageColorRes(R.color.md_yellow_A100)
    .enableSwipeToDismiss()
    .dismissOnTapOutside()
    .duration(Flashbar.DURATION_LONG)

fun AppCompatActivity.getGenericNotificationFlashbar(message : String) = Flashbar.Builder(this)
    .gravity(Flashbar.Gravity.BOTTOM)
    .showOverlay()
    .message(message)
    .backgroundColorRes(R.color.colorCardDark)
    .messageColorRes(R.color.white)
    .enableSwipeToDismiss()
    .dismissOnTapOutside()
    .duration(Flashbar.DURATION_LONG)

/*
-----------------------------------------------
    Result Codes
-----------------------------------------------
*/

const val RES_SIGN_IN = 4
const val RES_BUTTON_SETUP = 5

