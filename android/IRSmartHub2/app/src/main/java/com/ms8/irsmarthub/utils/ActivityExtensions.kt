package com.ms8.irsmarthub.utils

import android.app.Activity
import android.util.TypedValue
import com.andrognito.flashbar.Flashbar
import com.ms8.irsmarthub.R
import com.ms8.irsmarthub.remote_control.remote.views.asymmetric_gridview.Utils

fun Activity.findNavBarHeight(): Int {
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        resources.getDimensionPixelSize(resourceId)
    } else 0
}

fun Activity.getActionBarSize() : Int {
    val tv = TypedValue()
    return if (theme?.resolveAttribute(android.R.attr.actionBarSize, tv, true) == true)
        TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
    else
        Utils.dpToPx(this, 56f)
}

fun Activity.getGenericComingSoonFlashbar() = Flashbar.Builder(this)
    .gravity(Flashbar.Gravity.BOTTOM)
    .showOverlay()
    .message(R.string.coming_soon)
    .backgroundColorRes(R.color.colorBgFlashbar)
    .messageColorRes(R.color.colorTextFlashbarComingSoon)
    .enableSwipeToDismiss()
    .dismissOnTapOutside()
    .duration(Flashbar.DURATION_LONG)

fun Activity.getGenericErrorFlashbar(showPositiveAction : Boolean = false) = Flashbar.Builder(this)
    .gravity(Flashbar.Gravity.BOTTOM)
    .showOverlay()
    .backgroundColorRes(R.color.colorBgFlashbar)
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

fun Activity.getRemoteNameErrorString() : String {
    return "${getString(R.string.err_name_req_start)} ${MyValidators.MIN_REMOTE_NAME_LENGTH} - ${MyValidators.MAX_REMOTE_NAME_LENGTH} ${getString(R.string.err_name_req_end)}"
}

fun Activity?.showRemoteNameEmptyFlashbar() {
    this?.let {
        getGenericErrorFlashbar(true)
            .message(getString(R.string.err_empty_remote_name))
            .build()
            .show()
    }
}

fun Activity?.showInvalidRemoteNameFlashbar() {
    this?.let {
        getGenericErrorFlashbar(true)
            .message(getRemoteNameErrorString())
            .build()
            .show()
    }
}

fun Activity?.showUnknownRemoteSaveError() {
    this?.let {
        getGenericErrorFlashbar(true)
            .message(getString(R.string.err_unknown_save_remote))
            .build()
            .show()
    }
}