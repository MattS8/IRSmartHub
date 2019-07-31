package com.ms8.smartirhub.android.remote_control.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.remote_control.views.asymmetric_gridview.Utils
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.IMG_ADD
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.IMG_RADIAL_DOWN
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.IMG_RADIAL_LEFT
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.IMG_RADIAL_RIGHT
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.IMG_RADIAL_UP
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.IMG_SUBTRACT
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Properties.BgStyle
import com.wajahatkarim3.easyvalidation.core.view_ktx.validUrl
import org.jetbrains.anko.backgroundResource
import java.lang.Exception

class ButtonView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    private var buttonTextView  : TextView?     = null
    private var buttonImageView : ImageView?    = null

    var buttonText: CharSequence = ""
    set(value) {
        field = value

        if (buttonTextView == null)
            addTextView()

        buttonTextView?.text = field
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

        // set layout margins
        val newLayoutParams = layoutParams as MarginLayoutParams
        newLayoutParams.setMargins(
            Utils.dpToPx(context, field.marginStart.toFloat()),
            Utils.dpToPx(context, field.marginTop.toFloat()),
            Utils.dpToPx(context, field.marginEnd.toFloat()),
            Utils.dpToPx(context, field.marginBottom.toFloat())
        )

        // set background resource
        when (properties.bgStyle) {
            BgStyle.BG_CIRCLE -> { backgroundResource = R.drawable.btn_bg_circle_ripple }
            BgStyle.BG_ROUND_RECT -> { backgroundResource = R.drawable.btn_bg_round_rect }
            BgStyle.BG_ROUND_RECT_BOTTOM -> { backgroundResource = R.drawable.btn_bg_round_bottom_ripple }
            BgStyle.BG_ROUND_RECT_TOP -> { backgroundResource = R.drawable.btn_bg_round_top_ripple }
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

        // set image
        if (properties.image != "") {
            if (buttonImageView == null)
                addImageView()

            when (properties.image) {
                IMG_ADD -> {
                    buttonImageView?.setImageDrawable(context.getDrawable(R.drawable.ic_add_black_24dp))
                }
                IMG_SUBTRACT -> buttonImageView?.setImageDrawable(context.getDrawable(R.drawable.ic_remove_black_24dp))
                IMG_RADIAL_LEFT -> buttonImageView?.setImageDrawable(context.getDrawable(R.drawable.ic_keyboard_arrow_left_black_24dp))
                IMG_RADIAL_RIGHT -> buttonImageView?.setImageDrawable(context.getDrawable(R.drawable.ic_keyboard_arrow_right_black_24dp))
                IMG_RADIAL_UP -> buttonImageView?.setImageDrawable(context.getDrawable(R.drawable.ic_keyboard_arrow_up_black_24dp))
                IMG_RADIAL_DOWN -> buttonImageView?.setImageDrawable(context.getDrawable(R.drawable.ic_keyboard_arrow_down_black_24dp))
                else -> {
                    if (properties.image.validUrl())
                        buttonImageView?.let { Glide.with(it).load(properties.image).into(it) }
                }
            }
        }

        // apply changes
        layoutParams = newLayoutParams
        requestLayout()
        invalidate()
    }

    @SuppressLint("LogNotTimber")
    private fun addImageView() {
        buttonImageView = ImageView(context)
            .apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
        try {
            buttonImageView!!.layoutParams = LayoutParams(layoutParams)
                .apply {
                    width = WRAP_CONTENT
                    height = WRAP_CONTENT
                    gravity = Gravity.CENTER
                }
        } catch (e: Exception) {
            Log.e("ButtonView", "$e")
        }
        addView(buttonImageView)
    }

    @SuppressLint("LogNotTimber")
    private fun addTextView() {
        buttonTextView = TextView(context)
            .apply {
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this, 8, 16, 1, TypedValue.COMPLEX_UNIT_SP)
                setTextColor(ContextCompat.getColor(context, R.color.black))
                gravity = Gravity.CENTER
            }

        try {
            buttonTextView!!.layoutParams = LayoutParams(layoutParams)
                .apply {
                    width = MATCH_PARENT
                    height = MATCH_PARENT
                    gravity = Gravity.CENTER
                    when (properties.bgStyle) {
                        BgStyle.BG_CIRCLE ->  {
                            val margin = Utils.dpToPx(context,
                                BG_CIRCLE_MARGIN
                            )
                            setMargins(margin, margin, margin, margin)
                        }
                        BgStyle.BG_ROUND_RECT -> {
                            val margin = Utils.dpToPx(context,
                                BG_ROUND_RECT_MARGIN
                            )
                            setMargins(margin, margin, margin, margin)
                        }
                        BgStyle.BG_ROUND_RECT_TOP -> {
                            val margin = Utils.dpToPx(context,
                                BG_ROUND_RECT_TOP_MARGIN
                            )
                            setMargins(margin, margin, margin, margin)
                        }
                        BgStyle.BG_ROUND_RECT_BOTTOM -> {
                            val margin = Utils.dpToPx(context,
                                BG_ROUND_RECT_BOTTOM_MARGIN
                            )
                            setMargins(margin, margin, margin, margin)
                        }
                        BgStyle.BG_CUSTOM_IMAGE -> { /*TODO*/ }
                        BgStyle.BG_INVISIBLE -> { /*TODO*/ }
                    }
                }
        } catch (e: Exception) {
            Log.e("ButtonView", "$e")
        }
        addView(buttonTextView)
    }

    companion object {
        const val BG_CIRCLE_MARGIN = 10f
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