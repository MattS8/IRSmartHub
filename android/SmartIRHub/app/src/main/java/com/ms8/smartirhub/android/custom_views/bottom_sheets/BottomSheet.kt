package com.ms8.smartirhub.android.custom_views.bottom_sheets

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.databinding.VBottomSheetBinding

@Suppress("JoinDeclarationAndAssignment")
open class BottomSheet(context: Context) : BottomSheetDialog(context) {
    private val sheetView : View
    protected var binding : VBottomSheetBinding?

    var bWantsToLeave = false
    var bIsShowing = false
    var dismissOnButtonClick = true

    var description = ""
    set(value) {
        field = value
        binding?.tvDescription?.text = field
    }
    var sheetTitle = ""
    set(value) {
        field = value
        binding?.tvTitle?.text = field
    }

    var posText = ""
    set(value) {
        field = value
        binding?.let { b ->
            b.btnPos.text = field
            if (posText == "") {
                b.btnPos.visibility = View.GONE
            } else {
                b.btnPos.visibility = View.VISIBLE
                b.btnPos.text = posText
            }
        }

    }

    var negText = context.getString(R.string.dismiss)
    set(value) {
        field = value
        binding?.btnNeg?.text = field
    }

    var posListener : () -> Any? = {}
    var negListener : () -> Any? = {}

    constructor(context: Context,
                sheetTitle : String = "",
                description : String = "",
                posText : String = "",
                negText : String = "",
                posListener : () -> Any? = {},
                negListener : () -> Any? = {}) : this(context) {
        this.sheetTitle = sheetTitle
        this.description = description
        this.posText = posText
        this.negText = negText
        this.posListener = posListener
        this.negListener = negListener
    }

    init {
        sheetView = layoutInflater.inflate(R.layout.v_bottom_sheet, null)
        binding = DataBindingUtil.bind(sheetView)

        binding?.let { b ->
            b.tvTitle.text = sheetTitle
            b.tvDescription.text = description
            b.btnPos.text = posText
            b.btnNeg.text = negText

            b.btnPos.setOnClickListener {
                if (dismissOnButtonClick)
                    dismiss()
                posListener()
            }
            b.btnNeg.setOnClickListener {
                if (dismissOnButtonClick)
                    dismiss()
                negListener
            }
        }
    }

    fun setup() {
        setContentView(sheetView)
    }
}

class BottomErrorSheet(context: Context) : BottomSheet(context) {

    constructor(context: Context,
                sheetTitle : String = "",
                description : String = "",
                posText : String = "",
                negText : String = "",
                posListener : () -> Any? = {},
                negListener : () -> Any? = {}) : this(context) {
        this.sheetTitle = sheetTitle
        this.description = description
        this.posText = posText
        this.negText = negText
        this.posListener = posListener
        this.negListener = negListener
    }

    init {
        binding?.tvTitle?.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
    }


}