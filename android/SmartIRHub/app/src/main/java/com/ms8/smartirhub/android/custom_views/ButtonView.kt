package com.ms8.smartirhub.android.custom_views

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import com.ms8.smartirhub.android.models.firestore.RemoteProfile
import com.ms8.smartirhub.android.models.firestore.RemoteProfile.Button.Properties.BgStyle

class ButtonView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    class ButtonOutlineProvider(var buttonProperties: RemoteProfile.Button.Properties): ViewOutlineProvider() {
        override fun getOutline(view: View?, outline: Outline?) {
            when (buttonProperties.bgStyle) {
                BgStyle.BG_CIRCLE -> {
                    outline?.setOval(0, 0, view?.width ?: 0, view?.height ?: 0)
                }
                BgStyle.BG_ROUND_RECT -> {
                    outline?.setRoundRect(0, 0, view?.width ?: 0, view?.height ?: 0, (view?.height ?: 0 / 2).toFloat())
                }
                BgStyle.BG_ROUND_RECT_BOTTOM -> {
                    outline?.setRoundRect(0, 0, view?.width ?: 0, view?.height ?: 0, (view?.height ?: 0 / 2).toFloat())
                }
                BgStyle.BG_ROUND_RECT_TOP -> {
                    outline?.setRoundRect(0, 0, view?.width ?: 0, view?.height ?: 0, (view?.height ?: 0 / 2).toFloat())
                }
                BgStyle.BG_CUSTOM_IMAGE -> {

                }
                BgStyle.BG_INVISIBLE -> {

                }
            }
        }

    }
}