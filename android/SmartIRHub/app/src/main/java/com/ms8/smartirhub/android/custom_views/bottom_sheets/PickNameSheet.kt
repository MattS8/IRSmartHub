package com.ms8.smartirhub.android.custom_views.bottom_sheets

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.andrefrsousa.superbottomsheet.SuperBottomSheetFragment
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.utils.MyValidators.ButtonNameValidator
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.databinding.VChooseNameSheetBinding
import org.jetbrains.anko.sdk27.coroutines.onFocusChange

class PickNameSheet : SuperBottomSheetFragment() {

    private var binding: VChooseNameSheetBinding? = null
    var callback: Callback? = null

    var btnName = ""
    set(value) {
        field = value
        binding?.btnPickName?.text = field
    }
    var nameTitle = ""
    set(value) {
        field = value
        binding?.tvTitle?.text = field
    }
    var nameDesc = ""
    set(value) {
        field = value
        binding?.tvHelpNameDesc?.text = field
    }
    var tipsTitle = ""
    set(value) {
        field = value
        binding?.tvTipsTitle?.text = field
    }
    var tipsDesc1 = ""
    set(value) {
        field = value
        binding?.tvTipsDesc1?.text = field
    }
    var tipsDesc2 = ""
    set(value) {
        field = value
        binding?.tvTipsDesc2?.text = field
    }
    var tipsExampleTitle = ""
    set(value) {
        field = value
        binding?.tvTipsDescExampleTitle?.text = value
    }
    var nameInputHint = ""
    set(value) {
        field = value
        binding?.txtInput?.hint = value
    }


    /*
    ----------------------------------------------
        Overridden Functions
    ----------------------------------------------
*/

    override fun getTheme() = R.style.AppTheme

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(KEY_STR_LIST, arrayListOf(btnName, nameTitle, nameDesc, tipsTitle, tipsDesc1, tipsDesc2, tipsExampleTitle, nameInputHint))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.v_choose_name_sheet, container, false)

        val strList = savedInstanceState?.getStringArrayList(KEY_STR_LIST)

        binding!!.let { b ->
            b.btnPickName.text = strList?.get(0) ?: if (btnName != "") btnName else getString(R.string.pick_name)
            b.tvTitle.text =  strList?.get(1) ?: if (nameTitle != "") nameTitle else getString(R.string.need_help_with_a_name)
            b.tvHelpNameDesc.text = strList?.get(2) ?: if (nameDesc != "") nameDesc else getString(R.string.need_help_name_desc)
            b.tvTipsTitle.text = strList?.get(3) ?: tipsTitle
            b.tvTipsTitle.visibility = if (b.tvTipsTitle.text == "") View.GONE else View.VISIBLE
            b.tvTipsDesc1.text = strList?.get(4) ?: tipsDesc1
            b.tvTipsDesc1.visibility = if (b.tvTipsDesc1.text == "") View.GONE else View.VISIBLE
            b.tvTipsDesc2.text = strList?.get(4) ?: tipsDesc2
            b.tvTipsDesc2.visibility = if (b.tvTipsDesc2.text == "") View.GONE else View.VISIBLE
            b.tvTipsDescExampleTitle.text = strList?.get(5) ?: tipsExampleTitle
            b.tvTipsDescExampleTitle.visibility = if (b.tvTipsDescExampleTitle.text == "") View.GONE else View.VISIBLE
            b.txtInput.hint = strList?.get(6) ?: nameInputHint

            b.btnPickName.setOnClickListener { checkName() }
        }

        return binding!!.root
    }

    override fun onStart() {
        super.onStart()
        val layoutParams = dialog!!.findViewById<FrameLayout>(R.id.super_bottom_sheet).layoutParams
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog!!.findViewById<FrameLayout>(R.id.super_bottom_sheet).layoutParams = layoutParams
        binding?.txtInput?.editText!!.setUnderlineColor()
        binding?.txtInput?.editText!!.onFocusChange {v, _ ->
            v.setUnderlineColor()
        }

        //binding.txtInput.editText!!.inputType = (InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_PASSWORD )
    }

    private fun View.setUnderlineColor() {
        val color = ContextCompat.getColor(context!!, R.color.colorControlNormalWhite)
        this.backgroundTintList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_focused),
                intArrayOf(android.R.attr.state_focused)
            ),
            intArrayOf(
                color,
                color
            )
        )
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        callback?.onDismiss()
    }

    override fun getBackgroundColor() = ContextCompat.getColor(context!!, R.color.colorCardDark)

    override fun animateCornerRadius() = true

    override fun animateStatusBar() = true

    override fun isSheetAlwaysExpanded() = true

//    override fun getPeekHeight(): Int {
//        val displayMetrics = DisplayMetrics()
//        displayMetrics.heightPixels = 0
//        context?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
//        var height = displayMetrics.heightPixels
//        height = if (height <= 0){
//            500
//        }
//        else {
//            height - (height * .05).toInt()
//        }
//
//        @Suppress("UNNECESSARY_SAFE_CALL")
//        return
//    }

/*
    ----------------------------------------------
        OnClick Functions
    ----------------------------------------------
 */
    private fun checkName() {
    binding?.txtInput?.error = ""
        val isValidName = binding?.txtInput?.editText!!.text.toString().ButtonNameValidator()
            .addErrorCallback { binding?.txtInput?.error = getString(R.string.err_invalid_button_name) }
            .check()
        if (isValidName) {
            TempData.tempButton?.name = binding?.txtInput?.editText!!.text.toString()
            dismiss()
        }
    }

/*
    ----------------------------------------------
        Layout Functions
    ----------------------------------------------
*/



    interface Callback {
        fun onDismiss()
    }

    companion object {
        const val KEY_STR_LIST = "KEY_MANE_SHEET_STR_LIST"
    }
}
