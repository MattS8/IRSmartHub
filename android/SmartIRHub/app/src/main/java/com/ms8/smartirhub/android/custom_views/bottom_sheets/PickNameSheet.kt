package com.ms8.smartirhub.android.custom_views.bottom_sheets

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.VChooseNameSheetBinding
import com.ms8.smartirhub.android.utils.MyValidators.ButtonNameValidator
import org.jetbrains.anko.sdk27.coroutines.onFocusChange

class PickNameSheet(context: Context) : BottomSheetDialog(context) {

    var binding: VChooseNameSheetBinding? = null
    private var nameView: View = layoutInflater.inflate(R.layout.v_bottom_sheet, null)

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

    var strList : ArrayList<String>? = null

    init {
        binding = DataBindingUtil.bind(nameView)
    }

    fun setup() {
        setContentView(nameView)

        binding?.let { b ->
            b.btnPickName.text = strList?.get(0) ?: if (btnName != "") btnName else context.getString(R.string.pick_name)
            b.tvTitle.text =  strList?.get(1) ?: if (nameTitle != "") nameTitle else context.getString(R.string.need_help_with_a_name)
            b.tvHelpNameDesc.text = strList?.get(2) ?: if (nameDesc != "") nameDesc else context.getString(R.string.need_help_name_desc)
            b.tvTipsTitle.text = strList?.get(3) ?: tipsTitle
            b.tvTipsTitle.visibility = if (b.tvTipsTitle.text == "") View.GONE else View.VISIBLE
            b.tvTipsDesc1.text = strList?.get(4) ?: tipsDesc1
            b.tvTipsDesc1.visibility = if (b.tvTipsDesc1.text == "") View.GONE else View.VISIBLE
            b.tvTipsDesc2.text = strList?.get(4) ?: tipsDesc2
            b.tvTipsDesc2.visibility = if (b.tvTipsDesc2.text == "") View.GONE else View.VISIBLE
            b.tvTipsDescExampleTitle.text = strList?.get(5) ?: tipsExampleTitle
            b.tvTipsDescExampleTitle.visibility = if (b.tvTipsDescExampleTitle.text == "") View.GONE else View.VISIBLE
            b.txtInput.hint = strList?.get(6) ?: nameInputHint

            b.btnPickName.setOnClickListener { if (callback != null) callback?.onSavePressed(binding) else dismiss() }

            b.txtInput.editText!!.setUnderlineColor()
            b.txtInput.editText!!.onFocusChange {v, _ -> v.setUnderlineColor() }
        }

        setOnDismissListener { callback?.onDismiss() }
    }

    fun revertAnimation() {
        binding?.btnPickName?.revertAnimation()
    }

/*
    ----------------------------------------------
        Overridden Functions
    ----------------------------------------------
*/

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

/*
    ----------------------------------------------
        OnClick Functions
    ----------------------------------------------
 */
    private fun checkName() {
    binding?.txtInput?.error = ""
        val isValidName = binding?.txtInput?.editText!!.text.toString().ButtonNameValidator()
            .addErrorCallback { binding?.txtInput?.error = context.getString(R.string.err_invalid_button_name) }
            .check()
        if (isValidName) {
            AppState.tempData.tempButton?.name = binding?.txtInput?.editText!!.text.toString()
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
        fun onSavePressed(sheetBinding: VChooseNameSheetBinding?)
    }

    companion object {
        const val KEY_STR_LIST = "KEY_MANE_SHEET_STR_LIST"
    }
}
