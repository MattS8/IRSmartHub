package com.ms8.smartirhub.android.custom_views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.andrefrsousa.superbottomsheet.SuperBottomSheetFragment
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.databinding.VBottomSheetBinding

class BottomErrorSheet : SuperBottomSheetFragment() {
    lateinit var binding: VBottomSheetBinding
    var bWantsToLeave = false
    var bIsShowing = false
    var description = ""
    var sheetTitle = ""

    var posText = ""
    var posListener : () -> Any? = {}


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.v_bottom_sheet, container, false)
        binding.tvTitle.text = sheetTitle
        binding.tvTitle.setTextColor(ContextCompat.getColor(context!!, android.R.color.holo_red_dark))
        binding.tvDescription.text = description
        binding.btnNeg.text = context!!.getString(R.string.dismiss)
        binding.btnNeg.setOnClickListener { dismissWarning() }

        if (posText == "") {
            binding.btnPos.visibility = View.GONE
        } else {
            binding.btnPos.visibility = View.VISIBLE
            binding.btnPos.text = posText
            binding.btnPos.setOnClickListener {
                dismiss()
                posListener()
            }
        }

        return binding.root
    }

    override fun getBackgroundColor(): Int {
        return ContextCompat.getColor(context!!, R.color.colorPrimaryDark)
    }

    private fun dismissWarning() {
        bIsShowing = false
        dismiss()
    }

    fun setPositiveButton(buttonTitle: String, action: () -> Any?) {
        posText = buttonTitle
        posListener = action

    }

}