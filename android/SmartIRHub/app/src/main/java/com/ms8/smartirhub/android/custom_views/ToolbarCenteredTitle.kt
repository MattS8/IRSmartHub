package com.ms8.smartirhub.android.custom_views

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.WindowManager
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.utils.TypefaceCache
import java.lang.Exception


class ToolbarCenteredTitle(context: Context, attrs: AttributeSet) : androidx.appcompat.widget.Toolbar(context, attrs) {

    private var _screenWidth: Int
    private var _titleTextView: TextView
    private val location = IntArray(2)
    private var titleStr: String = ""
    var centerTitle = true
    set(value) {
        field = value
        requestLayout()
    }

    init {
        _screenWidth = getScreenSize().x

        _titleTextView = TextView(context)
        _titleTextView.text = titleStr
        TypefaceCache.get("font/roboto_light.ttf", context)?.let { _titleTextView.typeface = it }
        TextViewCompat.setTextAppearance(_titleTextView, R.style.AppTheme_TextAppearance_ToolbarTitle)
        addView(_titleTextView)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (centerTitle) {
            _titleTextView.getLocationOnScreen(location)
            _titleTextView.translationX =
                _titleTextView.translationX + (-location[0] + _screenWidth / 2 - _titleTextView.width / 2)
        }
    }

    @Suppress("UNNECESSARY_SAFE_CALL")
    override fun setTitle(resId: Int) {
        titleStr = context.getString(resId)
        _titleTextView?.let { tv ->
            tv.setText(resId)
            requestLayout()
        }
    }

    @Suppress("UNNECESSARY_SAFE_CALL")
    override fun setTitle(title : CharSequence) {
        titleStr = title.toString()
        _titleTextView?.let { tv ->
            tv.text = title
            requestLayout()
        }
    }

    private fun getScreenSize(): Point {
        val windowManager =  context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val screenSize = Point()
        windowManager.defaultDisplay.getSize(screenSize)
        return screenSize
    }

}