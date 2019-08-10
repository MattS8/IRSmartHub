package com.ms8.smartirhub.android.custom_views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.text.method.KeyListener
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.utils.TypefaceCache
import com.ms8.smartirhub.android.utils.extensions.hideKeyboard
import org.jetbrains.anko.singleLine
import org.jetbrains.anko.textColor


class ToolbarCenteredTitle(context: Context, attrs: AttributeSet) : androidx.appcompat.widget.Toolbar(context, attrs) {

    private var _screenWidth: Int
    private lateinit var _titleTextView: EditText
    private val location = IntArray(2)
    private var titleStr: String = ""
    private var _titleTextViewBG: Drawable
    private var _isTitleEditable = false
    var centerTitle = true
    set(value) {
        field = value
        requestLayout()
    }

    private val titleTextWatcher = object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {}

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(newText: CharSequence?, p1: Int, p2: Int, p3: Int) {
            newText?.let {
                AppState.tempData.tempRemoteProfile.name = it.toString()
            }
        }
    }
    private val titleEditorAction = TextView.OnEditorActionListener { tv, actionId, keyEvent ->
        if (actionId == EditorInfo.IME_ACTION_SEARCH
            || actionId == EditorInfo.IME_ACTION_DONE
            || keyEvent.action == KeyEvent.ACTION_DOWN
            && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
            _titleTextView.clearFocus()
            hideKeyboard()
            true
        } else {
            false
        }
    }

    init {
        _screenWidth = getScreenSize().x

        _titleTextView = EditText(context)
        _titleTextView.setSelectAllOnFocus(true)
        _titleTextView.textColor = ContextCompat.getColor(context, R.color.white)
        _titleTextView.maxLines = 1
        _titleTextView.singleLine = true
        _titleTextView.setText(titleStr)
        _titleTextView.setOnEditorActionListener(titleEditorAction)
        _titleTextViewBG = _titleTextView.background
        makeTitleEditable(false)
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
            tv.setText(title)
            requestLayout()
        }
    }

    private fun getScreenSize(): Point {
        val windowManager =  context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val screenSize = Point()
        windowManager.defaultDisplay.getSize(screenSize)
        return screenSize
    }

    fun makeTitleEditable(isEditable : Boolean = true) {
        _isTitleEditable = isEditable
        when (isEditable) {
            true -> {
                _titleTextView.setTextIsSelectable(true)
                _titleTextView.isEnabled = true
                _titleTextView.isFocusable = true
                _titleTextView.background = _titleTextViewBG
                _titleTextView.addTextChangedListener(titleTextWatcher)
            }
            false -> {
                _titleTextView.setTextIsSelectable(false)
                _titleTextView.isEnabled = false
                _titleTextView.isFocusable = false
                _titleTextView.background = null
                _titleTextView.removeTextChangedListener(titleTextWatcher)
                _titleTextView.textColor = ContextCompat.getColor(context, R.color.white)
            }
        }

    }

    @SuppressLint("LogNotTimber")
    fun selectTitleText() {
        if (!_isTitleEditable) {
            Log.e("ToolbarCenteredTitle", "Attempted to select toolbar title while not in edit mode.")
            return
        }

        Log.d("Toolbar", "text size = ${_titleTextView.text.toString().length}")
        _titleTextView.requestFocus()
        _titleTextView.selectAll()
    }
}