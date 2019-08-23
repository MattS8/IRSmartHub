package com.ms8.smartirhub.android.custom_views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.utils.extensions.hideKeyboard
import org.jetbrains.anko.singleLine
import org.jetbrains.anko.textColor


@Suppress("JoinDeclarationAndAssignment")
class ToolbarCenteredTitle(context: Context, attrs: AttributeSet) : androidx.appcompat.widget.Toolbar(context, attrs) {

/*
----------------------------------------------
  Toolbar State
----------------------------------------------
*/
    var layoutState = REMOTE_TITLE
    var titleStr = ""

    override fun onSaveInstanceState(): Parcelable? {
        return State(super.onSaveInstanceState())
            .apply {
                layoutState = this@ToolbarCenteredTitle.layoutState
                titleStr = this@ToolbarCenteredTitle.titleStr
            }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is State) {
            super.onRestoreInstanceState(state.superState)
            titleStr = state.titleStr
            layoutState = state.layoutState
            _titleET.setText(titleStr)
            _titleTV.text = titleStr
            state
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    internal class State : BaseSavedState {
        var layoutState : Int       = REMOTE_TITLE
        var titleStr    : String    = ""

        constructor(source: Parcel) : super(source) {
            layoutState = source.readInt()
            titleStr = source.readString() ?: ""
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(layoutState)
            parcel.writeString(titleStr)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<State> {
            override fun createFromParcel(parcel: Parcel): State {
                return State(parcel)
            }

            override fun newArray(size: Int): Array<State?> {
                return arrayOfNulls(size)
            }
        }
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
        when (layoutState) {
        // Start-aligned title
            NORMAL_TITLE ->
            {
                //todo set title to start-aligned instead of centered
            }
        // Centered title
            REMOTE_TITLE, NORMAL_TITLE_CENTERED ->
            {
                _titleTV.getLocationOnScreen(location)
                _titleTV.translationX = _titleTV.translationX + (-location[0] + getScreenSize().x / 2 - _titleTV.width / 2)
            }
        // Centered, editable title
            REMOTE_TITLE_EDITABLE ->
            {
                _titleET.getLocationOnScreen(location)
                _titleET.translationX = _titleET.translationX + (-location[0] + getScreenSize().x / 2 - _titleET.width / 2)
            }
            else -> Log.w("CenteredTitleToolbar", "Unknown state found in onLayout call: $layoutState")
        }
    }

    private fun applyLayoutState() {
        when (layoutState) {
            NORMAL_TITLE ->
            {
                _titleTV.visibility = View.VISIBLE
                _titleET.visibility = View.GONE
            }
            REMOTE_TITLE ->
            {
                //todo set large margins

                _titleTV.visibility = View.VISIBLE
                _titleET.visibility = View.GONE
            }
            NORMAL_TITLE_CENTERED ->
            {
                //todo set small margins

                _titleTV.visibility = View.VISIBLE
                _titleET.visibility = View.GONE
            }
            REMOTE_TITLE_EDITABLE ->
            {
                //todo set large margins

                _titleTV.visibility = View.GONE
                _titleET.visibility = View.VISIBLE
            }
        }
        requestLayout()
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

    @Suppress("UNNECESSARY_SAFE_CALL")
    override fun setTitle(resId: Int) {
        titleStr = context.getString(resId)

        _titleTV?.setText(resId)
        _titleET?.setText(resId)

        requestLayout()
    }

    @Suppress("UNNECESSARY_SAFE_CALL")
    override fun setTitle(title : CharSequence) {
        titleStr = title.toString()

        _titleTV?.text = titleStr
        _titleET?.setText(titleStr)

        requestLayout()
    }

    fun setTitleMode(newMode : Int) {
        layoutState = newMode
        applyLayoutState()
    }

    @SuppressLint("LogNotTimber")
    fun selectTitleText() {
        if (layoutState != REMOTE_TITLE_EDITABLE) {
            Log.e("ToolbarCenteredTitle", "Attempted to select toolbar title while not in edit mode.")
            return
        }

        Log.d("Toolbar", "text size = ${_titleET.text.toString().length}")
        _titleET.requestFocus()
        _titleET.selectAll()
    }

    fun setTitleHint(string: String) {
        _titleET.setText("")
        _titleET.hint = string
    }




    companion object {
        const val REMOTE_TITLE          = 0
        const val REMOTE_TITLE_EDITABLE = 1
        const val NORMAL_TITLE          = 2
        const val NORMAL_TITLE_CENTERED = 3
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