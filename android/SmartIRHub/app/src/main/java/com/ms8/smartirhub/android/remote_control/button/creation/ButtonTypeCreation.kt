package com.ms8.smartirhub.android.remote_control.button.creation

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.VBottomSheetBinding
import com.ms8.smartirhub.android.databinding.VButtonTypesBinding
import com.ms8.smartirhub.android.remote_control.button.models.Button
import java.lang.Exception

class ButtonTypeCreation {
    var onComplete: () -> Unit = {}
    var onCanceled: () -> Unit = {}

    fun showDialog(context: Context) {
        val bottomSheetBinding = VBottomSheetBinding.inflate(LayoutInflater.from(context))
        val buttonTypesBinding = VButtonTypesBinding.inflate(LayoutInflater.from(context), bottomSheetBinding.bottomSheetContainer, true)

        buttonTypesBinding.list?.adapter = ButtonTypeAdapter()
        buttonTypesBinding.list?.layoutManager = GridLayoutManager(context, 2)

        val createButtonDialog = BottomSheetDialog(context)

        createButtonDialog.setContentView(bottomSheetBinding.bottomSheetContainer)
        createButtonDialog.setOnDismissListener { onCanceled() }
        createButtonDialog.show()
    }

    inner class ButtonTypeAdapter : RecyclerView.Adapter<ButtonTypeAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(parent)
        }

        override fun getItemCount(): Int {
            return Button.Companion.ButtonStyle.values().size - 1
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val buttonImage =  holder.itemView.findViewById<ImageView>(R.id.btnImage)
            val buttonTitle = holder.itemView.findViewById<TextView>(R.id.tvButtonType)
            val buttonType = Button.buttonStyleFromInt(position) ?: throw Exception("Unknown button type ($position)")

            buttonTitle.text = Button.nameFromStyle(holder.itemView.context, buttonType)
            val buttonResource = Button.imageResourceFromStyle(buttonType)
            if (buttonResource != 0) Glide.with(buttonImage).load(buttonResource).into(buttonImage)
            holder.itemView.setOnClickListener {
                Log.d("TEST", "setting buttonType to $buttonType")

                // Set button type or create new button if none currently exists
                val tempButton = AppState.tempData.tempButton.get() ?: Button(buttonType)
                tempButton.type = buttonType
                AppState.tempData.tempButton.set(tempButton)

                // notify that button type was selected
                onComplete()
            }
        }

        inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.v_item_button_type, null))
    }
}