package com.ms8.irsmarthub.remote_control.button.views

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
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.ms8.irsmarthub.R
import com.ms8.irsmarthub.remote_control.button.models.Properties
import com.ms8.irsmarthub.remote_control.remote.views.asymmetric_gridview.Utils
import org.jetbrains.anko.backgroundResource
import com.ms8.irsmarthub.remote_control.button.models.Properties.Companion.BgStyle
import com.ms8.irsmarthub.remote_control.button.models.Properties.Companion.IMG_ADD
import com.ms8.irsmarthub.remote_control.button.models.Properties.Companion.IMG_RADIAL_DOWN
import com.ms8.irsmarthub.remote_control.button.models.Properties.Companion.IMG_RADIAL_LEFT
import com.ms8.irsmarthub.remote_control.button.models.Properties.Companion.IMG_RADIAL_RIGHT
import com.ms8.irsmarthub.remote_control.button.models.Properties.Companion.IMG_RADIAL_UP
import com.ms8.irsmarthub.remote_control.button.models.Properties.Companion.IMG_SUBTRACT
import com.wajahatkarim3.easyvalidation.core.view_ktx.validUrl

class RemoteButtonView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    private var buttonTextView  : TextView?     = null
    private var buttonImageView : ImageView?    = null
    /*
        Set margin
        Set background
        Set visibility
        Set text
     */
    private lateinit var bgStyle: BgStyle
    fun setupProperties(properties : Properties) {
        // save bgStyle
        bgStyle = properties.bgStyle

        Log.d("TEST", "Setting up properties with bg ${properties.bgStyle}")

        /* set background resource and margins
           note: certain bg styles are exempt from set margins
         */
        when (properties.bgStyle) {
            BgStyle.BG_CIRCLE ->
            {
                backgroundResource = R.drawable.bg_btn_rmt_circle_ripple
                (layoutParams as MarginLayoutParams).setMargins(
                    Utils.dpToPx(context, properties.marginStart.toFloat()),
                    Utils.dpToPx(context, properties.marginTop.toFloat()),
                    Utils.dpToPx(context, properties.marginEnd.toFloat()),
                    Utils.dpToPx(context, properties.marginBottom.toFloat())
                )
            }
            BgStyle.BG_ROUND_RECT ->
            {
                backgroundResource = R.drawable.bg_btn_rmt_square_rounded_ripple
                if (layoutParams is ConstraintLayout.LayoutParams) {
                    (layoutParams as ConstraintLayout.LayoutParams).apply {
                        dimensionRatio = "3:1"
                        setMargins(
                            Utils.dpToPx(context, properties.marginStart.toFloat()),
                            Utils.dpToPx(context, properties.marginTop.toFloat()),
                            Utils.dpToPx(context, properties.marginEnd.toFloat()),
                            Utils.dpToPx(context, properties.marginBottom.toFloat())
                        )
                    }
                } else {
                    (layoutParams as MarginLayoutParams).setMargins(
                        Utils.dpToPx(context, properties.marginStart.toFloat()),
                        Utils.dpToPx(context, properties.marginTop.toFloat()),
                        Utils.dpToPx(context, properties.marginEnd.toFloat()),
                        Utils.dpToPx(context, properties.marginBottom.toFloat())
                    )
                    layoutParams.width = MarginLayoutParams.MATCH_PARENT
                    layoutParams.height = MarginLayoutParams.WRAP_CONTENT
                }
            }
            BgStyle.BG_ROUND_RECT_BOTTOM ->
            {
                backgroundResource = R.drawable.bg_btn_rmt_square_rounded_bottom_ripple
                (layoutParams as MarginLayoutParams).setMargins(
                    Utils.dpToPx(context, properties.marginStart.toFloat()),
                    Utils.dpToPx(context, properties.marginTop.toFloat()),
                    Utils.dpToPx(context, properties.marginEnd.toFloat()),
                    Utils.dpToPx(context, properties.marginBottom.toFloat())
                )
            }
            BgStyle.BG_ROUND_RECT_TOP ->
            {
                backgroundResource = R.drawable.bg_btn_rmt_square_rounded_top_ripple
                (layoutParams as MarginLayoutParams).setMargins(
                    Utils.dpToPx(context, properties.marginStart.toFloat()),
                    Utils.dpToPx(context, properties.marginTop.toFloat()),
                    Utils.dpToPx(context, properties.marginEnd.toFloat()),
                    Utils.dpToPx(context, properties.marginBottom.toFloat())
                )
            }
            BgStyle.BG_INVISIBLE ->
            {
                backgroundResource = 0
                (layoutParams as MarginLayoutParams).setMargins(
                    Utils.dpToPx(context, properties.marginStart.toFloat()),
                    Utils.dpToPx(context, properties.marginTop.toFloat()),
                    Utils.dpToPx(context, properties.marginEnd.toFloat()),
                    Utils.dpToPx(context, properties.marginBottom.toFloat())
                )
            }
            BgStyle.BG_RADIAL_TOP ->
            {
                Log.d("TEST", "binding - setting bg to RADIAL_TOP")
                backgroundResource = R.drawable.bg_btn_rmt_radial_top_ripple
            }
            BgStyle.BG_RADIAL_END ->
            {
                Log.d("TEST", "binding - setting bg to RADIAL_END")
                backgroundResource = R.drawable.bg_btn_rmt_radial_end_ripple
            }
            BgStyle.BG_RADIAL_BOTTOM ->
            {
                Log.d("TEST", "binding - setting bg to RADIAL_BOTTOM")
                backgroundResource = R.drawable.bg_btn_rmt_radial_bottom_ripple
            }
            BgStyle.BG_RADIAL_START ->
            {
                Log.d("TEST", "binding - setting bg to RADIAL_START")
                backgroundResource = R.drawable.bg_btn_rmt_radial_start_ripple
            }
            BgStyle.BG_RADIAL_CENTER ->
            {
                backgroundResource = R.drawable.bg_btn_rmt_circle_ripple
            }
            BgStyle.BG_NONE ->
            {
                backgroundResource = 0
                // layoutParams = newLayoutParams
            }
            BgStyle.BG_CUSTOM_IMAGE -> {
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
                IMG_RADIAL_LEFT -> buttonImageView?.setImageDrawable(context.getDrawable(R.drawable.ic_arrow_left_black_24dp))
                IMG_RADIAL_RIGHT -> buttonImageView?.setImageDrawable(context.getDrawable(R.drawable.ic_arrow_right_black_24dp))
                IMG_RADIAL_UP -> buttonImageView?.setImageDrawable(context.getDrawable(R.drawable.ic_arrow_up_black_24dp))
                IMG_RADIAL_DOWN -> buttonImageView?.setImageDrawable(context.getDrawable(R.drawable.ic_arrow_down_black_24dp))
                else -> {
                    Log.d("TEST", "Checking URL")
                    if (properties.image.validUrl())
                        buttonImageView?.let { Glide.with(it).load(properties.image).into(it) }
                }
            }
//            val states = arrayOf(
//                IntArray(1).apply { set(0, android.R.attr.state_pressed) },
//                IntArray(1).apply { set(0, android.R.attr.state_enabled) },
//                IntArray(1).apply { set(0, -android.R.attr.state_enabled) }
//                )
//            val colors = IntArray(3).apply {
//                set(1, ContextCompat.getColor(context, R.color.colorButtonBG))
//                set(1, ContextCompat.getColor(context, R.color.white))
//                set(1, ContextCompat.getColor(context, R.color.warm_grey))
//            }
//            buttonImageView?.imageTintList = ColorStateList(states, colors)
        } else if (buttonImageView != null) {
            removeView(buttonImageView)
            buttonImageView = null
        }

        // set background tint
        backgroundTintList = if (properties.bgTint != "") {
            Log.d("TEST", "adding custom bg tint")
            ColorStateList.valueOf(Color.parseColor(properties.bgTint))
        } else {
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorBgRemoteButton))
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
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    gravity = Gravity.CENTER
                }
        } catch (e: Exception) {
            Log.e("ButtonView", "$e")
        }
        addView(buttonImageView)
    }

    @SuppressLint("LogNotTimber")
    private fun addTextView() {
        Log.d("TEST", "Adding text view")
        buttonTextView = TextView(context, null, R.style.TextAppearance_RemoteButtonText)
            .apply {
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                    this,
                    8,
                    16,
                    1, TypedValue.COMPLEX_UNIT_SP)
            }

        try {
            buttonTextView!!.layoutParams = LayoutParams(layoutParams)
                .apply {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                    gravity = Gravity.CENTER
                    when (bgStyle) {
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
                        BgStyle.BG_RADIAL_TOP -> { /*TODO*/ }
                        BgStyle.BG_RADIAL_END -> { /*TODO*/ }
                        BgStyle.BG_RADIAL_BOTTOM -> { /*TODO*/ }
                        BgStyle.BG_RADIAL_START -> { /*TODO*/ }
                        BgStyle.BG_RADIAL_CENTER -> {
                            Log.d("Test", "HERE")
                            /*TODO*/ }
                        BgStyle.BG_NONE -> { /*TODO*/ }
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
    class ButtonOutlineProvider(private var buttonProperties: Properties): ViewOutlineProvider() {
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
                BgStyle.BG_INVISIBLE -> {}
                BgStyle.BG_RADIAL_TOP -> {}
                BgStyle.BG_RADIAL_END -> {}
                BgStyle.BG_RADIAL_BOTTOM -> {}
                BgStyle.BG_RADIAL_START -> {}
                BgStyle.BG_RADIAL_CENTER -> {}
                BgStyle.BG_NONE -> {}
            }
        }
    }
}