package com.ms8.smartirhub.android.custom_views

import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview_k.Utils
import com.ms8.smartirhub.android.models.firestore.RemoteProfile
import com.ms8.smartirhub.android.models.firestore.RemoteProfile.Button.Properties.BgStyle
import com.wajahatkarim3.easyvalidation.core.view_ktx.validUrl
import org.jetbrains.anko.backgroundResource

class ButtonView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    private var buttonTextView: TextView
    var buttonText: CharSequence = ""
    set(value) {
        field = value
        buttonTextView.text = field
        buttonTextView.layoutParams = LayoutParams(layoutParams)
            .apply {
                width = MATCH_PARENT
                height = MATCH_PARENT
                gravity = Gravity.CENTER_HORIZONTAL
                when (properties.bgStyle) {
                    BgStyle.BG_CIRCLE ->  {
                        val margin = Utils.dpToPx(context, BG_CIRCLE_MARGIN)
                        setMargins(margin, margin, margin, margin)
                    }
                    BgStyle.BG_ROUND_RECT -> {
                        val margin = Utils.dpToPx(context, BG_ROUND_RECT_MARGIN)
                        setMargins(margin, margin, margin, margin)
                    }
                    BgStyle.BG_ROUND_RECT_TOP -> {
                        val margin = Utils.dpToPx(context, BG_ROUND_RECT_TOP_MARGIN)
                        setMargins(margin, margin, margin, margin)
                    }
                    BgStyle.BG_ROUND_RECT_BOTTOM -> {
                        val margin = Utils.dpToPx(context, BG_ROUND_RECT_BOTTOM_MARGIN)
                        setMargins(margin, margin, margin, margin)
                    }
                    BgStyle.BG_CUSTOM_IMAGE -> { /*TODO*/ }
                    BgStyle.BG_INVISIBLE -> { /*TODO*/ }
                }
            }
        Log.d("TEST##", "Textsize = ${buttonTextView.textSize}... text is ${buttonTextView.text}")
    }

    /*
        Set margin
        Set background
        Set visibility
        Set text
     */
    var properties: RemoteProfile.Button.Properties = RemoteProfile.Button.Properties()
    set(value) {
        field = value

        val newLayoutParams = layoutParams as MarginLayoutParams
        newLayoutParams.setMargins(
            Utils.dpToPx(context, field.marginStart.toFloat()),
            Utils.dpToPx(context, field.marginTop.toFloat()),
            Utils.dpToPx(context, field.marginEnd.toFloat()),
            Utils.dpToPx(context, field.marginBottom.toFloat())
        )

        when (properties.bgStyle) {
            BgStyle.BG_CIRCLE -> { backgroundResource = R.drawable.btn_bg_circle_ripple }
            BgStyle.BG_ROUND_RECT -> { backgroundResource = R.drawable.btn_bg_round_rect }
            BgStyle.BG_ROUND_RECT_BOTTOM -> { backgroundResource = R.drawable.btn_bg_round_bottom }
            BgStyle.BG_ROUND_RECT_TOP -> { backgroundResource = R.drawable.btn_bg_round_top }
            BgStyle.BG_INVISIBLE -> { backgroundResource = 0 }
            BgStyle.BG_CUSTOM_IMAGE -> {
                //TODO Test
                //TODO add ripple effect
                if (properties.bgUrl.validUrl()) {
                    Glide.with(this).load(properties.bgUrl).into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            background = resource
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
                }
            }
        }
        invalidate()
    }

    /*
        Add text view
     */
    init {
        /*
        TextView Properties:
            Set autoTextSizing
            Set text color
            Set gravity
         */
        try {
            buttonTextView = TextView(context)
                .apply {
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    TextViewCompat.setAutoSizeTextTypeWithDefaults(this, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
                    setTextColor(ContextCompat.getColor(context, R.color.black))
                }
            addView(buttonTextView)
        } catch (e: Exception) {
            Log.e("TEST##", "$e")
            buttonTextView = TextView(context)
        }
    }


    companion object {
        const val BG_CIRCLE_MARGIN = 16f
        const val BG_ROUND_RECT_MARGIN = 8f
        const val BG_ROUND_RECT_TOP_MARGIN = 8f
        const val BG_ROUND_RECT_BOTTOM_MARGIN = 8f
    }

    /*
        The purpose of this class was to make the ripple effect adhere to the shape of the background resource.
        This has been accomplished by adding a mask to the drawable resource instead of using this class.

        The drawback is that the current implementation does NOT support custom image outlines foo
     */
    class ButtonOutlineProvider(private var buttonProperties: RemoteProfile.Button.Properties): ViewOutlineProvider() {
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
                    //todo find outline?
                }
                BgStyle.BG_INVISIBLE -> {

                }
            }
        }
    }
}