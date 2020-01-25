package com.ms8.smartirhub.android.main_view.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Point
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
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
    private lateinit var _utilButton : ImageButton

    private val location = IntArray(2)

    private val titleEditorAction = TextView.OnEditorActionListener { tv, actionId, keyEvent ->
        if (actionId == EditorInfo.IME_ACTION_SEARCH
            || actionId == EditorInfo.IME_ACTION_DONE
            || keyEvent.action == KeyEvent.ACTION_DOWN
            && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
            _titleET.clearFocus()
            AppState.tempData.tempRemoteProfile.name = _titleET.text.toString()
            hideKeyboard()
            true
        } else {
            false
        }
    }

    init {
        // setup titleET
        _titleET = EditText(context).apply {
            setSelectAllOnFocus(true)
            textColor = ContextCompat.getColor(context, R.color.white)
            maxLines = 1
            singleLine = true
            TextViewCompat.setTextAppearance(this, R.style.AppTheme_TextAppearance_ToolbarTitle)
            gravity = Gravity.CENTER
            setOnEditorActionListener(titleEditorAction)
            hint = context.getString(R.string.name_new_remote)
        }

        // setup titleTV
        _titleTV = TextView(context).apply {
            textColor = ContextCompat.getColor(context, R.color.white)
            maxLines = 1
            singleLine = true
            TextViewCompat.setTextAppearance(this, R.style.AppTheme_TextAppearance_ToolbarTitle)
            gravity = Gravity.CENTER
        }

        // setup right button
        _utilButton = ImageButton(context, null, R.style.Widget_MaterialComponents_Button_OutlinedButton).apply {
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_more_vert_black_24dp))
            imageTintList = ContextCompat.getColorStateList(context, R.color.colorToolbarButtonTint)
            isClickable = true
            isFocusable = true
        }

        // add both views
        addView(_titleET)
        addView(_titleTV)
        addView(_utilButton)

        applyLayoutState()
    }

    @SuppressLint("LogNotTimber")
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        val scale: Float = resources.displayMetrics.density

        _titleTV.getLocationOnScreen(location)
        _titleTV.translationX = _titleTV.translationX + (-location[0] + getScreenSize().x / 2 - _titleTV.width / 2)
        _titleET.getLocationOnScreen(location)
        _titleET.translationX = _titleET.translationX + (-location[0] + getScreenSize().x / 2 - _titleET.width / 2)
        _utilButton.getLocationOnScreen(location)
        _utilButton.translationX = _utilButton.translationX + (-location[0] + getScreenSize().x - _utilButton.width - (32 * scale + 0.5f).toInt())
    }

    fun applyLayoutState() {
        val newLayoutParams = layoutParams as MarginLayoutParams?
        val topMargins = context.resources.getDimension(R.dimen.fsw_nav_height).toInt()
        val sideMargins = when (layoutState) {
            LayoutState.REMOTES_FAV,
            LayoutState.REMOTES_FAV_EDITING -> context.resources.getDimension(R.dimen.toolbar_horizontal_margin_remote).toInt()
            LayoutState.DEVICES_HUBS,
            LayoutState.REMOTES_ALL,
            LayoutState.DEVICES_ALL -> context.resources.getDimension(R.dimen.toolbar_horizontal_margin).toInt()

            null -> 0
        }
        val titleText = when (layoutState) {
            LayoutState.REMOTES_FAV_EDITING -> AppState.tempData.tempRemoteProfile.name
            LayoutState.REMOTES_ALL -> context.getString(R.string.all_remotes)
            LayoutState.DEVICES_HUBS -> context.getString(R.string.title_my_ir_hubs)
            LayoutState.DEVICES_ALL -> context.getString(R.string.title_my_devices)
            LayoutState.REMOTES_FAV -> if (AppState.tempData.tempRemoteProfile.name == "")
                context.getString(R.string.title_remotes)
            else
                AppState.tempData.tempRemoteProfile.name

            null -> ""
        }
        val utilButtonDrawable = if (layoutState == LayoutState.REMOTES_FAV_EDITING)
            ContextCompat.getDrawable(context, android.R.drawable.ic_menu_close_clear_cancel)
        else
            ContextCompat.getDrawable(context, R.drawable.ic_more_vert_black_24dp)

        _utilButton.apply {
            setImageDrawable(utilButtonDrawable)
            //todo - set on click listener
        }
        _titleTV.apply {
            text = titleText
            visibility = if (layoutState == LayoutState.REMOTES_FAV_EDITING) GONE else View.VISIBLE
        }
        _titleET.apply {
            setText(titleText)
            visibility = if (layoutState == LayoutState.REMOTES_FAV_EDITING) VISIBLE else GONE
        }
        newLayoutParams?.setMargins(sideMargins, topMargins, sideMargins, 0)
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

    fun setUtilButtonOnClick(onClick: OnClickListener?) {
        _utilButton.setOnClickListener(onClick)
    }

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
}