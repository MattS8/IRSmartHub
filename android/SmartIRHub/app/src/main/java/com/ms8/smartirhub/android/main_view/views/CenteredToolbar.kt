package com.ms8.smartirhub.android.main_view.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.main_view.MainViewActivity.Companion.LayoutState
import com.ms8.smartirhub.android.utils.extensions.hideKeyboard
import org.jetbrains.anko.singleLine
import org.jetbrains.anko.textColor


@Suppress("JoinDeclarationAndAssignment")
class CenteredToolbar(context: Context, attrs: AttributeSet) : androidx.appcompat.widget.Toolbar(context, attrs) {

/*
----------------------------------------------
  Toolbar State
----------------------------------------------
*/
    var layoutState : LayoutState? = null
    set(value) {
        field = value
        applyLayoutState()
    }

/*
----------------------------------------------
 Inner View Logic
----------------------------------------------
*/

    private lateinit var _titleTV : TextView
    private lateinit var _titleET : EditText

    private val location = IntArray(2)

    private val titleEditorAction = TextView.OnEditorActionListener { tv, actionId, keyEvent ->
        if (actionId == EditorInfo.IME_ACTION_SEARCH
            || actionId == EditorInfo.IME_ACTION_DONE
            || keyEvent.action == KeyEvent.ACTION_DOWN
            && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
            _titleET.clearFocus()
            hideKeyboard()
            true
        } else {
            false
        }
    }

    init {
        // setup titleET
        _titleET = EditText(context)
        _titleET.setSelectAllOnFocus(true)
        _titleET.textColor = ContextCompat.getColor(context, R.color.white)
        _titleET.maxLines = 1
        _titleET.singleLine = true
        TextViewCompat.setTextAppearance(_titleET, R.style.AppTheme_TextAppearance_ToolbarTitle)
        _titleET.setOnEditorActionListener(titleEditorAction)
        _titleET.hint = context.getString(R.string.name_new_remote)

        // setup titleTV
        _titleTV = TextView(context)
        _titleTV.textColor = ContextCompat.getColor(context, R.color.white)
        _titleTV.maxLines = 1
        _titleTV.singleLine = true
        TextViewCompat.setTextAppearance(_titleTV, R.style.AppTheme_TextAppearance_ToolbarTitle)

        // add both views
        addView(_titleET)
        addView(_titleTV)

        applyLayoutState()
    }

    @SuppressLint("LogNotTimber")
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        _titleTV.getLocationOnScreen(location)
        _titleTV.translationX = _titleTV.translationX + (-location[0] + getScreenSize().x / 2 - _titleTV.width / 2)
        _titleET.getLocationOnScreen(location)
        _titleET.translationX = _titleET.translationX + (-location[0] + getScreenSize().x / 2 - _titleET.width / 2)
    }

    private fun applyLayoutState() {
        val newLayoutParams = layoutParams as MarginLayoutParams?

        when (layoutState) {
            LayoutState.REMOTES_FAV ->
            {
                // get toolbar title
                _titleTV.text =  if (AppState.tempData.tempRemoteProfile.name == "")
                    context.getString(R.string.title_remotes)
                else
                    AppState.tempData.tempRemoteProfile.name

                // set margins
                val sideMargins = context.resources.getDimension(R.dimen.toolbar_horizontal_margin_remote).toInt()
                val topMargins = context.resources.getDimension(R.dimen.fsw_nav_height).toInt()
                newLayoutParams?.setMargins(sideMargins, topMargins, sideMargins, 0)

                _titleTV.visibility = View.VISIBLE
                _titleET.visibility = View.GONE
            }
            LayoutState.REMOTES_FAV_EDITING ->
            {
                // get toolbar title
                _titleET.setText(AppState.tempData.tempRemoteProfile.name)

                // set margins
                val sideMargins = context.resources.getDimension(R.dimen.toolbar_horizontal_margin_remote).toInt()
                val topMargins = context.resources.getDimension(R.dimen.fsw_nav_height).toInt()
                newLayoutParams?.setMargins(sideMargins, topMargins, sideMargins, 0)

                _titleTV.visibility = View.GONE
                _titleET.visibility = View.VISIBLE
            }
            LayoutState.REMOTES_ALL ->
            {
                // get toolbar title
                _titleTV.text = context.getString(R.string.all_remotes)

                // set margins
                val sideMargins = context.resources.getDimension(R.dimen.toolbar_horizontal_margin).toInt()
                val topMargins = context.resources.getDimension(R.dimen.fsw_nav_height).toInt()
                newLayoutParams?.setMargins(sideMargins, topMargins, sideMargins, 0)

                _titleTV.visibility = View.VISIBLE
                _titleET.visibility = View.GONE
            }
            LayoutState.DEVICES_ALL ->
            {
                // get toolbar title
                _titleTV.text = context.getString(R.string.title_my_devices)

                // set margins
                val sideMargins = context.resources.getDimension(R.dimen.toolbar_horizontal_margin).toInt()
                val topMargins = context.resources.getDimension(R.dimen.fsw_nav_height).toInt()
                newLayoutParams?.setMargins(sideMargins, topMargins, sideMargins, 0)

                _titleTV.visibility = View.VISIBLE
                _titleET.visibility = View.GONE
            }
            LayoutState.DEVICES_HUBS ->
            {
                // get toolbar title
                _titleTV.text = context.getString(R.string.title_my_ir_hubs)

                // set margins
                val sideMargins = context.resources.getDimension(R.dimen.toolbar_horizontal_margin).toInt()
                val topMargins = context.resources.getDimension(R.dimen.fsw_nav_height).toInt()
                newLayoutParams?.setMargins(sideMargins, topMargins, sideMargins, 0)

                _titleTV.visibility = View.VISIBLE
                _titleET.visibility = View.GONE
            }
        }

        newLayoutParams?.let { layoutParams = it }
        requestLayout ()
    }

    private fun getScreenSize(): Point {
        val windowManager =  context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val screenSize = Point()
        windowManager.defaultDisplay.getSize(screenSize)
        return screenSize
    }

/*
----------------------------------------------
 Public Accessors
----------------------------------------------
*/

    @SuppressLint("LogNotTimber")
    fun selectTitleText() {
        if (layoutState != LayoutState.REMOTES_FAV_EDITING) {
            Log.e("ToolbarCenteredTitle", "Attempted to select toolbar title while not in edit mode.")
            return
        }

        Log.d("Toolbar", "text size = ${_titleET.text.toString().length}")
        _titleET.requestFocus()
        _titleET.selectAll()
    }

//    private var _screenWidth: Int
//    private lateinit var _titleTextView: EditText
//    private val location = IntArray(2)
//    private var titleStr: String = ""
//    private var _titleTextViewBG: Drawable?
//
//    private var _isTitleEditable = false
//    var centerTitle = true
//    set(value) {
//        field = value
//        requestLayout()
//    }
//
//    private val titleTextWatcher = object : TextWatcher {
//        override fun afterTextChanged(p0: Editable?) {}
//
//        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//
//        override fun onTextChanged(newText: CharSequence?, p1: Int, p2: Int, p3: Int) {
//            newText?.let {
//                AppState.tempData.tempRemoteProfile.name = it.toString()
//            }
//        }
//    }
//    private val titleEditorAction = TextView.OnEditorActionListener { tv, actionId, keyEvent ->
//        if (actionId == EditorInfo.IME_ACTION_SEARCH
//            || actionId == EditorInfo.IME_ACTION_DONE
//            || keyEvent.action == KeyEvent.ACTION_DOWN
//            && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
//            _titleTextView.clearFocus()
//            hideKeyboard()
//            true
//        } else {
//            false
//        }
//    }
//
//    init {
//        _screenWidth = getScreenSize().x
//        _titleTextView = EditText(context)
//        _titleTextView.setSelectAllOnFocus(true)
//        _titleTextView.textColor = ContextCompat.getColor(context, R.color.white)
//        _titleTextView.maxLines = 1
//        _titleTextView.singleLine = true
//        _titleTextView.setText(titleStr)
//        _titleTextView.setOnEditorActionListener(titleEditorAction)
//        _titleTextView.gravity = Gravity.CENTER
//        _titleTextViewBG = _titleTextView.background
//        makeTitleEditable(false)
//        TypefaceCache.get("font/roboto_light.ttf", context)?.let { _titleTextView.typeface = it }
//        TextViewCompat.setTextAppearance(_titleTextView, R.style.AppTheme_TextAppearance_ToolbarTitle)
//
//        addView(_titleTextView)
//    }
//
//    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
//        super.onLayout(changed, l, t, r, b)
//        if (centerTitle) {
//            _titleTextView.getLocationOnScreen(location)
//            _titleTextView.translationX =
//                _titleTextView.translationX + (-location[0] + _screenWidth / 2 - _titleTextView.width / 2)
//        }
//    }
//
//    @Suppress("UNNECESSARY_SAFE_CALL")
//    override fun setTitle(resId: Int) {
//        titleStr = context.getString(resId)
//        _titleTextView?.let { tv ->
//            tv.setText(resId)
//            requestLayout()
//        }
//    }
//
//    @Suppress("UNNECESSARY_SAFE_CALL")
//    override fun setTitle(title : CharSequence) {
//        titleStr = title.toString()
//        _titleTextView?.let { tv ->
//            tv.setText(title)
//            requestLayout()
//        }
//    }
//
//    private fun getScreenSize(): Point {
//        val windowManager =  context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        val screenSize = Point()
//        windowManager.defaultDisplay.getSize(screenSize)
//        return screenSize
//    }
//
//    fun makeTitleEditable(isEditable : Boolean = true) {
//        _isTitleEditable = isEditable
//        when (isEditable) {
//            true -> {
//                _titleTextView.setTextIsSelectable(true)
//                _titleTextView.isEnabled = true
//                _titleTextView.isFocusable = true
//                _titleTextView.background = _titleTextViewBG
//                _titleTextView.addTextChangedListener(titleTextWatcher)
//            }
//            false -> {
//                _titleTextView.setTextIsSelectable(false)
//                _titleTextView.isEnabled = false
//                _titleTextView.isFocusable = false
//                _titleTextView.background = null
//                _titleTextView.removeTextChangedListener(titleTextWatcher)
//            }
//        }
//
//    }
//
//    @SuppressLint("LogNotTimber")
//    fun selectTitleText() {
//        if (!_isTitleEditable) {
//            Log.e("ToolbarCenteredTitle", "Attempted to select toolbar title while not in edit mode.")
//            return
//        }
//
//        Log.d("Toolbar", "text size = ${_titleTextView.text.toString().length}")
//        _titleTextView.requestFocus()
//        _titleTextView.selectAll()
//    }
//
//    fun setTitleHint(string: String) {
//        _titleTextView.setText("")
//        _titleTextView.hint = string
//    }
}