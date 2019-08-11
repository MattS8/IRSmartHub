package com.ms8.smartirhub.android.custom_views.bottom_sheets

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ms8.smartirhub.android.databinding.VSimpleListDescSheetBinding

open class SimpleListDescSheet(context: Context) : BottomSheetDialog(context) {
    var binding: VSimpleListDescSheetBinding? = null

    var hasSecondaryAction: Boolean = true
    set(value) {
        field = value
        binding?.btnClearActions?.isEnabled = field
        binding?.btnClearActions?.visibility = if (field) View.VISIBLE else View.GONE
    }

    var isSaveEnabled: Boolean = true
    set(value) {
        field = value
        binding?.btnSaveCommand?.isEnabled = field
    }

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

    fun setup() {
        binding?.let { b ->
            b.sheetList.adapter = callback?.getAdapter()
            b.sheetList.layoutManager = callback?.getLayoutManager()
            b.tvTitle.text = sheetTitle
            b.tvListHelpTitle.text = helpTitle
            b.tvListHelpTitle.visibility = if (b.tvListHelpTitle.text == "") View.GONE else View.VISIBLE
            b.tvListHelpDesc.text = helpDesc
            b.tvListHelpDesc.visibility = if (b.tvListHelpDesc.text == "") View.GONE else View.VISIBLE

            b.btnSaveCommand.setOnClickListener { if (callback != null) callback?.onSavePressed(this, b) else dismiss() }
            b.btnClearActions.setOnClickListener { if (callback != null) callback?.onCancelPress(this, b) else dismiss() }

            b.btnSaveCommand.isEnabled = isSaveEnabled
            b.btnClearActions.isEnabled = hasSecondaryAction
            b.btnClearActions.visibility = if (hasSecondaryAction) View.VISIBLE else View.GONE

            callback?.onCreateView(b)
        }
    }

    interface SimpleListDescSheetCallback {
        fun onSavePressed(simpleListDescSheet: SimpleListDescSheet, binding: VSimpleListDescSheetBinding)
        fun onCancelPress(simpleListDescSheet: SimpleListDescSheet, binding: VSimpleListDescSheetBinding)
        fun onCreateView(binding: VSimpleListDescSheetBinding)
        fun getLayoutManager() : RecyclerView.LayoutManager
        fun getAdapter() : RecyclerView.Adapter<*>
    }

    companion object {
        const val REQ_NEW_ACTION = 50
        const val REQ_EDIT_ACTION = 51

        const val KEY_SHEET_STRS = "KEY_SHEET_STRS"
        const val KEY_SHEET_BOOLS = "KEY_SHEET_BOOLS"
    }
}