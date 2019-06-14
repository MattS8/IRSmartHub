package com.ms8.smartirhub.android.custom_views

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.ms8.smartirhub.android.R


/**
 * Custom View that shows a circular progress bar and some inner text.
 */
class CircularProgressBar(context: Context, attr : AttributeSet) : View(context, attr) {
    /* ----------------------- View Properties ----------------------- */
    var minProgress = 0                                                 // Ratio of completion (minProgress : maxProgress)
    var maxProgress = 100                                               // Ratio of completion (minProgress : maxProgress)
    var initialAngle = -90                                              // Starting angle to begin filling from
    var rectF = RectF()                                                 //
    var backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)                  // Paint used for unfilled part circle (technically all of circle)
    var foregroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)                  // Paint used for filled part of circle
    var progress = 0f                                                   // Percentage of bar to fill
    set(value) {
        field = value
        invalidate()
    }
    var color = Color.DKGRAY                                       // Color of progress bar
        set(value) {
            field = value
            backgroundPaint.color = getTransparentColor(color, 0.3f)
            foregroundPaint.color = color
            invalidate()
            requestLayout()
        }
    var lineWidth = 4f                                                  // Width of progress bar
        set(value) {
            field = value
            backgroundPaint.strokeWidth = lineWidth
            foregroundPaint.strokeWidth = lineWidth
            invalidate()
            requestLayout()
        }
    var animDuration = PROGRESS_ANIM_DURATION

    init {
        // Get xml-declared values
        val typedArray = context.theme.obtainStyledAttributes(attr, R.styleable.CircleProgressBar, 0 ,0)
        try {
            lineWidth = typedArray.getDimension(R.styleable.CircleProgressBar_progressBarThickness, lineWidth)
            progress = typedArray.getFloat(R.styleable.CircleProgressBar_barProgress, progress)
            color = typedArray.getInt(R.styleable.CircleProgressBar_progressbarColor, color)
            minProgress = typedArray.getInt(R.styleable.CircleProgressBar_min, minProgress)
            maxProgress = typedArray.getInt(R.styleable.CircleProgressBar_max, maxProgress)

        } finally { typedArray.recycle() }

        // Setup paint
        backgroundPaint.apply {
            this.color = getTransparentColor(color, 0.3f)
            this.style = Paint.Style.STROKE
            this.strokeWidth = lineWidth
        }
        foregroundPaint.apply {
            this.color = color
            this.style = Paint.Style.STROKE
            this.strokeWidth = lineWidth
        }
    }

    fun animateBarProgress(newProgress : Float) =
        ObjectAnimator.ofFloat(this, "progress", newProgress).apply {
            duration = animDuration
            interpolator = DecelerateInterpolator()
        }.start()


    fun getTransparentColor(color : Int, transparency : Float) = Color.argb(
            Math.round(Color.alpha(color) * transparency),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )

/* ---------------------------------------------- Overridden Functions ---------------------------------------------- */

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {c ->
            c.drawOval(rectF, backgroundPaint)
            val arcAngle = ANGLE_FULL_CIRCLE * progress / maxProgress
            c.drawArc(rectF, initialAngle.toFloat(), arcAngle, false, foregroundPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mHeight = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val mWidth = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val minVal = Math.min(mWidth, mHeight)
        setMeasuredDimension(minVal, minVal)
        rectF.set(lineWidth/2, lineWidth/2, minVal - lineWidth/2, minVal - lineWidth/2)

    }

    /* -------------------------------------------------- Static Stuff -------------------------------------------------- */

    companion object {
        const val ANGLE_FULL_CIRCLE = 360
        const val PROGRESS_ANIM_DURATION : Long = 1000
    }
}