package com.ms8.smartirhub.android.custom_views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.andrefrsousa.superbottomsheet.SuperBottomSheetFragment
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.databinding.VBottomSheetBinding
import kotlinx.android.synthetic.main.v_bottom_sheet.*

class BackWarningSheet : SuperBottomSheetFragment() {
    private var binding: VBottomSheetBinding? = null
    var bWantsToLeave = false
    var bIsShowing = false

    var titleStr = ""
    set(value) {
        field = value
        binding?.tvTitle?.text = titleStr
    }
    var descStr = ""
    set(value) {
        field = value
        binding?.tvDescription?.text = descStr
    }
    var btnNegStr = ""
    set(value) {
        field = value
        binding?.btnNeg?.text = value
    }
    var btnPosStr = ""
    set(value) {
        field = value
        binding?.btnPos?.text = value
    }

    var callback : BackWaringSheetCallback? = null

    var performDefaultPosAction = true
    var performDefaultNegAction = true

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(KEY_STR_STATE, arrayListOf(titleStr, descStr, btnNegStr, btnPosStr))
        outState.putBooleanArray(KEY_DEF_ACTION_STATE, BooleanArray(2).apply {
            set(0, performDefaultPosAction)
            set(1, performDefaultNegAction)
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.v_bottom_sheet, container, false)

        val strArray = savedInstanceState?.getStringArray(KEY_STR_STATE)
        binding?.tvTitle?.text =  strArray?.get(0) ?: if (titleStr == "") context!!.getString(R.string.are_you_sure) else titleStr
        binding?.tvDescription?.text = strArray?.get(1) ?: if (descStr == "") context!!.getString(R.string.wrn_create_button_desc) else descStr
        binding?.btnNeg?.text = strArray?.get(2) ?: if (btnNegStr == "") context!!.getString(R.string.stay) else btnNegStr
        binding?.btnPos?.text = strArray?.get(3) ?: if (btnPosStr == "") context!!.getString(R.string.go_back) else btnPosStr

        val boolArray = savedInstanceState?.getBooleanArray(KEY_DEF_ACTION_STATE)
        performDefaultNegAction = boolArray?.get(1) ?: performDefaultNegAction
        performDefaultPosAction = boolArray?.get(0) ?: performDefaultPosAction

        binding?.btnNeg?.setOnClickListener { dismissWarning() }
        binding?.btnPos?.setOnClickListener { leave() }

        return binding?.root
    }


//    override fun getPeekHeight() = 250

    override fun animateCornerRadius() = true

    override fun getBackgroundColor(): Int {
        return ContextCompat.getColor(context!!, R.color.colorCardDark)
    }

    private fun dismissWarning() {
        callback?.btnNegAction()

        if (performDefaultNegAction) {
            bIsShowing = false
            dismiss()
        }
    }

    private fun leave() {
        callback?.btnPosAction()

        if (performDefaultPosAction) {
            bWantsToLeave = true
            activity?.onBackPressed()
        }
    }

    interface BackWaringSheetCallback {
        fun btnNegAction()
        fun btnPosAction()
    }

    companion object {
        const val KEY_STR_STATE = "KEY_STR_STATE"
        const val KEY_DEF_ACTION_STATE = "KEY_DEF_ACT_STATE"
    }
}