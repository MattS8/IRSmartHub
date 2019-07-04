package com.ms8.smartirhub.android.custom_views

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

    lateinit var binding: VChooseNameSheetBinding
    var callback: Callback? = null
    /*
    ----------------------------------------------
        Overridden Functions
    ----------------------------------------------
*/

    override fun getTheme() = R.style.AppTheme

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.v_choose_name_sheet, container, false)

        binding.btnPickName.text = getString(R.string.pick_name)
        binding.tvTitle.text = getString(R.string.need_help_with_a_name)
        binding.tvHelpNameDesc.text = getString(R.string.need_help_name_desc)
        binding.tvRememberDesc.text = getString(R.string.remember_button_name_desc)

        binding.tvTipsTitle.visibility = View.GONE
        binding.tvTipsDesc1.visibility = View.GONE
        binding.tvTipsDesc2.visibility = View.GONE
        binding.tvTipsDescExampleTitle.visibility = View.GONE

        binding.txtName.hint = getString(R.string.button_name_hint)

        binding.btnPickName.text = getString(R.string.pick_name)
        binding.btnPickName.setOnClickListener { checkName() }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val layoutParams = dialog!!.findViewById<FrameLayout>(R.id.super_bottom_sheet).layoutParams
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog!!.findViewById<FrameLayout>(R.id.super_bottom_sheet).layoutParams = layoutParams
        binding.txtName.editText!!.setUnderlineColor()
        binding.txtName.editText!!.onFocusChange {v, _ ->
            v.setUnderlineColor()

        }
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
    binding.txtName.error = ""
        val isValidName = binding.txtName.editText!!.text.toString().ButtonNameValidator()
            .addErrorCallback { binding.txtName.error = getString(R.string.err_invalid_button_name) }
            .check()
        if (isValidName) {
            TempData.tempButton?.name = binding.txtName.editText!!.text.toString()
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
}
