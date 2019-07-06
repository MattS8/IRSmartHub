package com.ms8.smartirhub.android.custom_views.bottom_sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.andrefrsousa.superbottomsheet.SuperBottomSheetFragment
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.databinding.VSimpleListDescSheetBinding

class SimpleListDescSheet : SuperBottomSheetFragment() {
    private var binding: VSimpleListDescSheetBinding? = null

    var isSaveEnabled: Boolean = false
    set(value) {
        field = value
        binding?.btnSaveCommand?.isEnabled = field
    }

    var adapter: RecyclerView.Adapter<*>? = null
    var layoutManager: RecyclerView.LayoutManager? = null

    var sheetTitle = ""
        set(value) {
            field = value
            binding?.tvTitle?.text = field
            binding?.tvTitle?.visibility = if (binding?.tvTitle?.text == "") View.GONE else View.VISIBLE
        }
    var helpTitle = ""
    set(value) {
        field = value
        binding?.tvListHelpTitle?.text = field
        binding?.tvListHelpTitle?.visibility = if (binding?.tvListHelpTitle?.text == "") View.GONE else View.VISIBLE
    }
    var helpDesc = ""
        set(value) {
            field = value
            binding?.tvListHelpDesc?.text = field
            binding?.tvListHelpDesc?.visibility = if (binding?.tvListHelpDesc?.text == "") View.GONE else View.VISIBLE
        }

    var callback: SimpleListDescSheetCallback? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(KEY_SHEET_STRS, arrayListOf(sheetTitle, helpTitle, helpDesc))
        outState.putBoolean(KEY_SHEET_BOOLS, isSaveEnabled)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.v_simple_list_desc_sheet, container, false)

        val strList = savedInstanceState?.getStringArrayList(KEY_SHEET_STRS)
        savedInstanceState?.getBoolean(KEY_SHEET_BOOLS)?.let { isSaveEnabled = it }

        binding!!.let { b ->
            b.sheetList.layoutManager = layoutManager
            b.sheetList.adapter = adapter

            b.tvTitle.text = strList?.get(0) ?: if (sheetTitle != "") sheetTitle else context?.getString(R.string.command_title)
            b.tvListHelpTitle.text = strList?.get(1) ?: helpTitle
            b.tvListHelpTitle.visibility = if (b.tvListHelpTitle.text == "") View.GONE else View.VISIBLE
            b.tvListHelpDesc.text = strList?.get(2) ?: helpDesc
            b.tvListHelpDesc.visibility = if (b.tvListHelpDesc.text == "") View.GONE else View.VISIBLE

            b.btnSaveCommand.setOnClickListener { if (callback != null) callback?.onSavePressed() else dismiss() }
            b.btnClearActions.setOnClickListener { if (callback != null) callback?.onCancelPress() else dismiss() }

            b.btnSaveCommand.isEnabled = isSaveEnabled
        }

        callback?.onCreateView(binding!!)
        return binding!!.root
    }

    interface SimpleListDescSheetCallback {
        fun onSavePressed()
        fun onCancelPress()
        fun onCreateView(binding: VSimpleListDescSheetBinding)
    }



    companion object {
        const val REQ_NEW_ACTION = 50
        const val REQ_EDIT_ACTION = 51

        const val KEY_SHEET_STRS = "KEY_SHEET_STRS"
        const val KEY_SHEET_BOOLS = "KEY_SHEET_BOOLS"
    }
}