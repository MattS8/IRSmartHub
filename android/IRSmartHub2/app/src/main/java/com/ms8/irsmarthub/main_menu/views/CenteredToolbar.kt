package com.ms8.irsmarthub.main_menu.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.ms8.irsmarthub.R
import com.ms8.irsmarthub.database.AppState
import com.ms8.irsmarthub.database.AppState.tempData
import com.ms8.irsmarthub.main_menu.MainMenuAdapter.Companion.LayoutState
import com.ms8.irsmarthub.utils.hideKeyboard
import org.jetbrains.anko.appcompat.v7.coroutines.onMenuItemClick
import org.jetbrains.anko.sdk27.coroutines.onClick
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

    private var _titleTV : TextView
    private lateinit var _titleET : EditText
    private var _utilButton : ImageButton

    private val location = IntArray(2)

    private val titleEditorAction = TextView.OnEditorActionListener { tv, actionId, keyEvent ->
        if (actionId == EditorInfo.IME_ACTION_SEARCH
            || actionId == EditorInfo.IME_ACTION_DONE
            || keyEvent.action == KeyEvent.ACTION_DOWN
            && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
            _titleET.clearFocus()
            tempData.tempRemote.name.set(_titleET.text.toString())
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
        TextViewCompat.setTextAppearance(_titleET, R.style.TextAppearance_ToolbarText)
        _titleET.gravity = Gravity.CENTER
        _titleET.setOnEditorActionListener(titleEditorAction)
        _titleET.hint = context.getString(R.string.hint_remote_name)

        // setup titleTV
        _titleTV = TextView(context)
        _titleTV.textColor = ContextCompat.getColor(context, R.color.white)
        _titleTV.maxLines = 1
        _titleTV.singleLine = true
        TextViewCompat.setTextAppearance(_titleTV, R.style.TextAppearance_ToolbarText)
        _titleTV.gravity = Gravity.CENTER

        // setup right button

        _utilButton = ImageButton(context, null, R.style.Widget_MaterialComponents_Button_OutlinedButton).apply {
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_more_vert_black_24dp))
            imageTintList = ContextCompat.getColorStateList(context, android.R.color.white)
            background = ContextCompat.getDrawable(context, getSelectableItemBackground())
            setOnClickListener { onUtilButtonClicked() }
        }

        // add both views
        addView(_titleET)
        addView(_titleTV)
        addView(_utilButton)

        applyLayoutState()
    }
    private fun onUtilButtonClicked() {
        when (layoutState) {
            LayoutState.REMOTES_FAV -> TODO()
            LayoutState.REMOTES_FAV_EDITING -> TODO()
            LayoutState.REMOTES_ALL -> TODO()
            LayoutState.DEVICES_HUBS -> TODO()
            LayoutState.DEVICES_ALL -> TODO()
            null -> TODO()
        }
    }

    private fun getSelectableItemBackground(): Int {
        val outValue = TypedValue()
        context.theme
            .resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        return outValue.resourceId
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
        _utilButton.translationX = _utilButton.translationX + (-location[0] + getScreenSize().x - _utilButton.width - (42 * scale + 0.5f).toInt())
    }

    fun applyLayoutState() {
        val newLayoutParams = layoutParams as MarginLayoutParams?
        val marginsTop = context.resources.getDimension(R.dimen.nav_fsw_height).toInt()
        var marginsSide = 0
        var titleText = ""
        var tvTitleVis = View.VISIBLE
        var etTitleVis = View.GONE
        var btnUtilVis = View.GONE
        var btnUtilDrawable: Drawable? = null
        var btnUtilOnClick: OnClickListener? = null

        when (layoutState) {
            LayoutState.REMOTES_FAV ->
            {
                marginsSide = context.resources.getDimension(R.dimen.toolbar_margin_remote).toInt()
                titleText = if (tempData.tempRemote.name.get() ?: "" == "")
                    context.getString(R.string.title_new_remote)
                else
                    tempData.tempRemote.name.get().toString()
                tvTitleVis = View.VISIBLE
                etTitleVis = View.GONE
                btnUtilVis = View.GONE
                menu.clear()
                inflateMenu(R.menu.menu_remotes_fav)
                btnUtilOnClick = OnClickListener {  }
            }
            LayoutState.REMOTES_FAV_EDITING ->
            {
                marginsSide = context.resources.getDimension(R.dimen.toolbar_margin_remote).toInt()
                titleText = if (tempData.tempRemote.name.get() ?: "" == "")
                    ""
                else
                    tempData.tempRemote.name.get().toString()
                tvTitleVis = View.GONE
                etTitleVis = View.VISIBLE
                btnUtilVis = View.VISIBLE
                menu.clear()
                hideOverflowMenu()
                btnUtilDrawable = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_close_clear_cancel)
                btnUtilOnClick = OnClickListener { tempData.tempRemote.inEditMode.set(false) }
            }
            LayoutState.REMOTES_ALL ->
            {
                marginsSide = context.resources.getDimension(R.dimen.toolbar_margin).toInt()
                titleText = context.getString(R.string.title_all_remotes)
                tvTitleVis = View.VISIBLE
                etTitleVis = View.GONE
                btnUtilVis = View.GONE
                menu.clear()
                inflateMenu(R.menu.menu_remotes_all)
                btnUtilOnClick = OnClickListener {  }
            }
            LayoutState.DEVICES_HUBS ->
            {
                marginsSide = context.resources.getDimension(R.dimen.toolbar_margin).toInt()
                titleText = context.getString(R.string.title_my_ir_hubs)
                tvTitleVis = View.VISIBLE
                etTitleVis = View.GONE
                btnUtilVis = View.GONE
                menu.clear()
                inflateMenu(R.menu.menu_hubs)
                btnUtilOnClick = OnClickListener {  }
            }
            LayoutState.DEVICES_ALL ->
            {
                marginsSide = context.resources.getDimension(R.dimen.toolbar_margin).toInt()
                titleText = context.getString(R.string.title_my_devices)
                tvTitleVis = View.VISIBLE
                etTitleVis = View.GONE
                btnUtilVis = View.GONE
                menu.clear()
                inflateMenu(R.menu.menu_hubs)
                btnUtilOnClick = OnClickListener {  }
            }
        }

        _utilButton.apply {
            setImageDrawable(btnUtilDrawable)
            visibility = btnUtilVis
            setOnClickListener(btnUtilOnClick)
        }
        _titleTV.apply {
            text = titleText
            visibility = tvTitleVis
        }
        _titleET.apply {
            setText(titleText)
            visibility = etTitleVis
        }
        newLayoutParams?.setMargins(marginsSide, marginsTop, marginsSide, 0)
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
    fun viewAllRemotes(item: MenuItem) {

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