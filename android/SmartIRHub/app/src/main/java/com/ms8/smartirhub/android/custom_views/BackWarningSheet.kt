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

class BackWarningSheet : SuperBottomSheetFragment() {
    lateinit var binding: VBottomSheetBinding
    var bWantsToLeave = false
    var bIsShowing = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.v_bottom_sheet, container, false)
        binding.tvTitle.text = context!!.getString(R.string.are_you_sure)
        binding.tvDescription.text = context!!.getString(R.string.wrn_create_button_desc)
        binding.btnNeg.text = context!!.getString(R.string.stay)
        binding.btnNeg.setOnClickListener { dismissWarning() }
        binding.btnPos.setOnClickListener { leave() }
        binding.btnPos.text = context!!.getString(R.string.go_back)

        return binding.root
    }

    override fun getBackgroundColor(): Int {
        return ContextCompat.getColor(context!!, R.color.colorPrimaryDark)
    }

    private fun dismissWarning() {
        bIsShowing = false
        dismiss()
    }

    private fun leave() {
        bWantsToLeave = true
        activity?.onBackPressed()
    }
}