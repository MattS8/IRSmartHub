package com.ms8.smartirhub.android.custom_views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.Nullable
import com.ms8.smartirhub.android.R


/**
 * Custom wrapper view to get round corner round view. Original code created by ahmed on 9/17/2017. Adapted by MattS8 on 6/27/2019.
 */
open class RoundedView : FrameLayout {

    /**
     * The corners than can be changed
     */
    private var topLeftCornerRadius: Float = 0.toFloat()
    private var topRightCornerRadius: Float = 0.toFloat()
    private var bottomLeftCornerRadius: Float = 0.toFloat()
    private var bottomRightCornerRadius: Float = 0.toFloat()

    constructor(context: Context) : super(context) {
        init(context, null, 0)
    }

    constructor(context: Context, @Nullable attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs, 0)
    }

    constructor(context: Context, @Nullable attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyle: Int) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.RoundedView, 0, 0
        )

        //get the default value form the attrs
        val cornerRadius = typedArray.getDimension(R.styleable.RoundedView_cornerRadius, -1f)
        if (cornerRadius != -1f) {
            topLeftCornerRadius = cornerRadius
            topRightCornerRadius = cornerRadius
            bottomLeftCornerRadius = cornerRadius
            bottomRightCornerRadius = cornerRadius
        } else {
            topLeftCornerRadius = typedArray.getDimension(R.styleable.RoundedView_topLeftCornerRadius, 0f)
            topRightCornerRadius = typedArray.getDimension(R.styleable.RoundedView_topRightCornerRadius, 0f)
            bottomLeftCornerRadius = typedArray.getDimension(R.styleable.RoundedView_bottomLeftCornerRadius, 0f)
            bottomRightCornerRadius = typedArray.getDimension(R.styleable.RoundedView_bottomRightCornerRadius, 0f)
        }
        typedArray.recycle()
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    fun setCornerRadii(topLeft : Float, topRight : Float, bottomLeft : Float, bottomRight : Float, invalidate : Boolean) {
        topLeftCornerRadius = topLeft
        topRightCornerRadius = topRight
        bottomLeftCornerRadius = bottomLeft
        bottomRightCornerRadius = bottomRight
        if (invalidate)
            invalidate()
    }


    override fun dispatchDraw(canvas: Canvas) {
        val count = canvas.save()

        val path = Path()

        val cornerDimensions = floatArrayOf(
            topLeftCornerRadius,
            topLeftCornerRadius,
            topRightCornerRadius,
            topRightCornerRadius,
            bottomRightCornerRadius,
            bottomRightCornerRadius,
            bottomLeftCornerRadius,
            bottomLeftCornerRadius
        )

        path.addRoundRect(RectF(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat()), cornerDimensions, Path.Direction.CW)

        canvas.clipPath(path)

        super.dispatchDraw(canvas)
        canvas.restoreToCount(count)
    }
}