package com.ms8.smartirhub.android.remote_control.button.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
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
import com.ms8.smartirhub.android.remote_control.button.models.Button
import com.ms8.smartirhub.android.remote_control.button.models.Button.Companion.IMG_ADD
import com.ms8.smartirhub.android.remote_control.button.models.Button.Companion.IMG_RADIAL_DOWN
import com.ms8.smartirhub.android.remote_control.button.models.Button.Companion.IMG_RADIAL_LEFT
import com.ms8.smartirhub.android.remote_control.button.models.Button.Companion.IMG_RADIAL_RIGHT
import com.ms8.smartirhub.android.remote_control.button.models.Button.Companion.IMG_RADIAL_UP
import com.ms8.smartirhub.android.remote_control.button.models.Button.Companion.IMG_SUBTRACT
import com.ms8.smartirhub.android.remote_control.views.asymmetric_gridview.Utils
import com.wajahatkarim3.easyvalidation.core.view_ktx.validUrl
import org.jetbrains.anko.backgroundResource

class RemoteButtonView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    private var buttonTextView  : TextView?     = null
    private var buttonImageView : ImageView?    = null
    /*
        Set margin
        Set background
        Set visibility
        Set text
     */
    private lateinit var bgStyle: Button.Companion.BgStyle
    fun setupProperties(properties : Button.Properties) {
        // save bgStyle
        bgStyle = properties.bgStyle

        /* set background resource and margins
           note: certain bg styles are exempt from set margins
         */
        when (properties.bgStyle) {
            Button.Companion.BgStyle.BG_CIRCLE ->
            {
                backgroundResource = R.drawable.btn_bg_circle_ripple
                (layoutParams as MarginLayoutParams).setMargins(
                    Utils.dpToPx(context, properties.marginStart.toFloat()),
                    Utils.dpToPx(context, properties.marginTop.toFloat()),
                    Utils.dpToPx(context, properties.marginEnd.toFloat()),
                    Utils.dpToPx(context, properties.marginBottom.toFloat())
                )
            }
            Button.Companion.BgStyle.BG_ROUND_RECT ->
            {
                backgroundResource = R.drawable.btn_bg_round_rect_ripple
                (layoutParams as MarginLayoutParams).setMargins(
                    Utils.dpToPx(context, properties.marginStart.toFloat()),
                    Utils.dpToPx(context, properties.marginTop.toFloat()),
                    Utils.dpToPx(context, properties.marginEnd.toFloat()),
                    Utils.dpToPx(context, properties.marginBottom.toFloat())
                )
            }
            Button.Companion.BgStyle.BG_ROUND_RECT_BOTTOM ->
            {
                backgroundResource = R.drawable.btn_bg_round_bottom_ripple
                (layoutParams as MarginLayoutParams).setMargins(
                    Utils.dpToPx(context, properties.marginStart.toFloat()),
                    Utils.dpToPx(context, properties.marginTop.toFloat()),
                    Utils.dpToPx(context, properties.marginEnd.toFloat()),
                    Utils.dpToPx(context, properties.marginBottom.toFloat())
                )
            }
            Button.Companion.BgStyle.BG_ROUND_RECT_TOP ->
            {
                backgroundResource = R.drawable.btn_bg_round_top_ripple
                (layoutParams as MarginLayoutParams).setMargins(
                    Utils.dpToPx(context, properties.marginStart.toFloat()),
                    Utils.dpToPx(context, properties.marginTop.toFloat()),
                    Utils.dpToPx(context, properties.marginEnd.toFloat()),
                    Utils.dpToPx(context, properties.marginBottom.toFloat())
                )
            }
            Button.Companion.BgStyle.BG_INVISIBLE ->
            {
                backgroundResource = 0
                (layoutParams as MarginLayoutParams).setMargins(
                    Utils.dpToPx(context, properties.marginStart.toFloat()),
                    Utils.dpToPx(context, properties.marginTop.toFloat()),
                    Utils.dpToPx(context, properties.marginEnd.toFloat()),
                    Utils.dpToPx(context, properties.marginBottom.toFloat())
                )
            }
            Button.Companion.BgStyle.BG_RADIAL_TOP ->
            {
                backgroundResource = R.drawable.btn_bg_radial_top_ripple
            }
            Button.Companion.BgStyle.BG_RADIAL_END ->
            {
                backgroundResource = R.drawable.btn_bg_radial_end_ripple
            }
            Button.Companion.BgStyle.BG_RADIAL_BOTTOM ->
            {
                backgroundResource = R.drawable.btn_bg_radial_bottom_ripple
            }
            Button.Companion.BgStyle.BG_RADIAL_START ->
            {
                backgroundResource = R.drawable.btn_bg_radial_start_ripple
            }
            Button.Companion.BgStyle.BG_RADIAL_CENTER ->
            {
                backgroundResource = R.drawable.btn_bg_circle_ripple
            }
            Button.Companion.BgStyle.BG_NONE ->
            {
                backgroundResource = 0
               // layoutParams = newLayoutParams
            }
            Button.Companion.BgStyle.BG_CUSTOM_IMAGE -> {
                //TODO test and add ripple effect
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

        // set text
        if (properties.text != "") {
            if (buttonTextView == null)
                addTextView()

            buttonTextView?.text = properties.text
        } else if (buttonTextView != null) {
            removeView(buttonTextView)
            buttonImageView = null
        }

        // set image
        if (properties.image != "") {
            Log.d("Test", "Image is ${properties.image}")
            if (buttonImageView == null)
                addImageView()

            when (properties.image) {
                IMG_ADD -> buttonImageView?.setImageDrawable(context.getDrawable(R.drawable.ic_add_black_24dp))
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
        } else if (buttonImageView != null) {
            removeView(buttonImageView)
            buttonImageView = null
        }

        // set background tint
        backgroundTintList = if (properties.bgTint != "") {
            ColorStateList.valueOf(Color.parseColor(properties.bgTint))
        } else {
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorButtonBG))
        }

        // apply changes
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
                    when (bgStyle) {
                        Button.Companion.BgStyle.BG_CIRCLE ->  {
                            val margin = Utils.dpToPx(context,
                                BG_CIRCLE_MARGIN
                            )
                            setMargins(margin, margin, margin, margin)
                        }
                        Button.Companion.BgStyle.BG_ROUND_RECT -> {
                            val margin = Utils.dpToPx(context,
                                BG_ROUND_RECT_MARGIN
                            )
                            setMargins(margin, margin, margin, margin)
                        }
                        Button.Companion.BgStyle.BG_ROUND_RECT_TOP -> {
                            val margin = Utils.dpToPx(context,
                                BG_ROUND_RECT_TOP_MARGIN
                            )
                            setMargins(margin, margin, margin, margin)
                        }
                        Button.Companion.BgStyle.BG_ROUND_RECT_BOTTOM -> {
                            val margin = Utils.dpToPx(context,
                                BG_ROUND_RECT_BOTTOM_MARGIN
                            )
                            setMargins(margin, margin, margin, margin)
                        }
                        Button.Companion.BgStyle.BG_CUSTOM_IMAGE -> { /*TODO*/ }
                        Button.Companion.BgStyle.BG_INVISIBLE -> { /*TODO*/ }
                        Button.Companion.BgStyle.BG_RADIAL_TOP -> { /*TODO*/ }
                        Button.Companion.BgStyle.BG_RADIAL_END -> { /*TODO*/ }
                        Button.Companion.BgStyle.BG_RADIAL_BOTTOM -> { /*TODO*/ }
                        Button.Companion.BgStyle.BG_RADIAL_START -> { /*TODO*/ }
                        Button.Companion.BgStyle.BG_RADIAL_CENTER -> {
                            Log.d("Test", "HERE")
                            /*TODO*/ }
                        Button.Companion.BgStyle.BG_NONE -> { /*TODO*/ }
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
    class ButtonOutlineProvider(private var buttonProperties: Button.Properties): ViewOutlineProvider() {
        override fun getOutline(view: View?, outline: Outline?) {
            when (buttonProperties.bgStyle) {
                Button.Companion.BgStyle.BG_CIRCLE -> {
                    outline?.setOval(0, 0, view?.width ?: 0, view?.height ?: 0)
                }
                Button.Companion.BgStyle.BG_ROUND_RECT -> {
                    outline?.setRoundRect(0, 0, view?.width ?: 0, view?.height ?: 0, (view?.height ?: 0 / 2).toFloat())
                }
                Button.Companion.BgStyle.BG_ROUND_RECT_BOTTOM -> {
                    outline?.setRoundRect(0, 0, view?.width ?: 0, view?.height ?: 0, (view?.height ?: 0 / 2).toFloat())
                }
                Button.Companion.BgStyle.BG_ROUND_RECT_TOP -> {
                    outline?.setRoundRect(0, 0, view?.width ?: 0, view?.height ?: 0, (view?.height ?: 0 / 2).toFloat())
                }
                Button.Companion.BgStyle.BG_CUSTOM_IMAGE -> {
                    //todo find outline?
                }
                Button.Companion.BgStyle.BG_INVISIBLE -> {

                }
            }
        }
    }
}