package com.ms8.smartirhub.android.custom_views

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MyExtendedFAB(context: Context, attrs: AttributeSet): ExtendedFloatingActionButton(context, attrs) {
    private var oldWidth = measuredWidth
    private var animateWidth = false
    private val myInterp = DecelerateInterpolator()
    private var originalLayoutParams: ViewGroup.LayoutParams? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
//        Log.d("MyExtendedFAB", "Width = $measuredWidth, suggestedWidth = $suggestedMinimumWidth")
//        if (animateWidth) {
//            animateWidth = false
//            if (measuredWidth != 0 && oldWidth != measuredWidth) {
//                Log.d("MyExtendedFAB", "oldWidth = $oldWidth, newWidth = $measuredWidth")
//                ObjectAnimator.ofInt(this, "width", oldWidth, measuredWidth).apply {
//                    duration = 2000
//                    interpolator = myInterp
//                    addListener(object : Animator.AnimatorListener {
//                        override fun onAnimationRepeat(p0: Animator?) {}
//                        override fun onAnimationStart(p0: Animator?) {}
//                        override fun onAnimationEnd(p0: Animator?) {
//                            Log.d("MyExtendedFAB", "Animation done!")
//                            layoutParams = originalLayoutParams
//                        }
//                        override fun onAnimationCancel(p0: Animator?) { layoutParams = originalLayoutParams.also { it!!.width = ViewGroup.LayoutParams.WRAP_CONTENT} }
//
//                    })
//                }.start()
//            }
//        }

    }

    fun animateNewText(newText: CharSequence) {
        if (originalLayoutParams == null)
            originalLayoutParams = layoutParams
        animateWidth = true
        oldWidth = measuredWidth
        text = newText
    }
}