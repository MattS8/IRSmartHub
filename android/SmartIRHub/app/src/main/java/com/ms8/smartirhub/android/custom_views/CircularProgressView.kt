package com.ms8.smartirhub.android.custom_views

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.ms8.smartirhub.android.R
import kotlinx.android.synthetic.main.v_circular_progress.view.*

class CircularProgressView(context: Context, attrs: AttributeSet): ConstraintLayout(context, attrs) {
    /* ----------------------- View Properties ----------------------- */
    var description : String? = ""
    set(value) {
        field = value
        descriptionView.text = value
        invalidate()
    }
    var step = 1
    var bOnThisStep = false
    set(value) {
        field = value

        val progress = when (bOnThisStep) {
            true -> circularProgressBar.maxProgress.toFloat()
            false -> circularProgressBar.minProgress.toFloat()
        }

        animateViews(bOnThisStep)
        circularProgressBar.animateBarProgress(progress)
    }
    var barColor = Color.DKGRAY
    set(value) {
        field = value
        circularProgressBar.color = barColor
    }
    var lineWidth = 4f
    var duration = ANIM_DURATION

    /* ------------------------- Nested Views ------------------------ */
    var numberView: TextView
    var descriptionView: TextView
    var circularProgressBar: CircularProgressBar

    init {
        View.inflate(context, R.layout.v_circular_progress, this)

        numberView = findViewById(R.id.tvNumber)
        descriptionView = findViewById(R.id.tvDescription)
        circularProgressBar = findViewById(R.id.progressBar)

        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.CircularProgressView, 0, 0)
        try {
            description = typedArray.getString(R.styleable.CircularProgressView_progressDescription)
            step = typedArray.getInt(R.styleable.CircularProgressView_progressStep, step)
            bOnThisStep = typedArray.getBoolean(R.styleable.CircularProgressView_progressOnThisStep, bOnThisStep)
            barColor = typedArray.getColor(R.styleable.CircularProgressView_barColor, barColor)
            lineWidth = typedArray.getDimension(R.styleable.CircularProgressView_barThickness, lineWidth)
            duration = typedArray.getInt(R.styleable.CircularProgressView_animationDuration, duration)
        } finally {
            typedArray.recycle()
        }

        circularProgressBar.animDuration = duration.toLong()
        circularProgressBar.lineWidth = lineWidth
        circularProgressBar.color = barColor
        numberView.text = step.toString()
        descriptionView.text = description
    }


    private fun animateViews(onStep: Boolean) {
        val alphaVal = if (onStep) 1f else 0.3f
        val scaleVal = if (onStep) 1.1f else 1f

        ObjectAnimator.ofFloat(numberView, "alpha", alphaVal).apply {
            duration = this@CircularProgressView.duration.toLong()
            interpolator = DecelerateInterpolator()
        }.start()
        ObjectAnimator.ofFloat(descriptionView, "alpha", alphaVal).apply {
            duration = this@CircularProgressView.duration.toLong()
            interpolator = DecelerateInterpolator()
        }.start()

        ObjectAnimator.ofFloat(progContainter, "scaleY", scaleVal).apply {
            duration = this@CircularProgressView.duration.toLong()
            interpolator = DecelerateInterpolator()
        }.start()
        ObjectAnimator.ofFloat(progContainter, "scaleX", scaleVal).apply {
            duration = this@CircularProgressView.duration.toLong()
            interpolator = DecelerateInterpolator()
        }.start()
    }

    fun getTransparentColor(color : Int, transparency : Float) = Color.argb(
        Math.round(Color.alpha(color) * transparency),
        Color.red(color),
        Color.green(color),
        Color.blue(color)
    )

    companion object {
        const val ANIM_DURATION = 1000
    }
}