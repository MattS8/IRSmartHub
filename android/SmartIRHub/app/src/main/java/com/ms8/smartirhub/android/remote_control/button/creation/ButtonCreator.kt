package com.ms8.smartirhub.android.remote_control.button.creation

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.VButtonTypesBinding
import com.ms8.smartirhub.android.remote_control.button.models.Button
import org.jetbrains.anko.layoutInflater
import java.lang.Exception

class ButtonCreator {

/*
----------------------------------------------
    State Functions
----------------------------------------------
*/

    enum class ButtonDialogState(var value: Int) {CHOOSE_TYPE(1), SETUP_BUTTON(2), NAME(3)}
    var dialogState             : ButtonDialogState  = ButtonDialogState.CHOOSE_TYPE

    var createButtonDialog      : BottomSheetDialog? = null
    private var isTransitioning : Boolean            = false


    fun showBottomDialog(context: Context) {
        when (dialogState) {
            ButtonDialogState.CHOOSE_TYPE -> { showChooseButtonTypeDialog(context) }
            ButtonDialogState.SETUP_BUTTON -> { showSetupButtonDialog(context) }
            ButtonDialogState.NAME -> { showNameButtonDialog(context) }
        }
    }

    fun dismissBottomDialog() {
        // set state if not transitioning
        if (!isTransitioning)
            dialogState = ButtonDialogState.CHOOSE_TYPE

        createButtonDialog?.dismiss()
        createButtonDialog = null
    }

/*
----------------------------------------------
    Activity Functions
----------------------------------------------
*/

    // must he bound/unbound per activity lifecycle
    var activity : AppCompatActivity? = null


/*
----------------------------------------------
    Display Dialog Functions
----------------------------------------------
*/

    private fun showChooseButtonTypeDialog(context: Context) {
        // change state
        dialogState = ButtonDialogState.CHOOSE_TYPE

        // set up bottom sheet dialog
        val buttonTypesView = context.layoutInflater.inflate(R.layout.v_button_types, null)
        createButtonDialog = BottomSheetDialog(context)
        val buttonTypeBinding = DataBindingUtil.bind<VButtonTypesBinding>(buttonTypesView)
        createButtonDialog?.setContentView(buttonTypesView)
        createButtonDialog?.setOnDismissListener {
            if (!isTransitioning)
                dialogState = ButtonDialogState.CHOOSE_TYPE
        }

        // set up list of button types
        buttonTypeBinding?.list?.adapter = ButtonTypeAdapter()
        buttonTypeBinding?.list?.layoutManager = GridLayoutManager(context, 2)
    }


    private fun showNameButtonDialog(context: Context) {
        // change state
        dialogState = ButtonDialogState.NAME

        // set up bottom sheet dialog
        //val buttonNameView = context.layoutInflater.inflate(R.layout.)

        // set up listeners
    }

    private fun showSetupButtonDialog(context: Context) {
        // change state
        dialogState = ButtonDialogState.SETUP_BUTTON

        // set up bottom sheet dialog
        when (AppState.tempData.tempButton?.style) {
            Button.Companion.ButtonStyle.STYLE_BTN_SINGLE_ACTION_ROUND -> { setupRoundSingleActionButton(context) }
            Button.Companion.ButtonStyle.STYLE_BTN_INCREMENTER_VERTICAL -> { setupVerticalIncrementerButton(context) }
            Button.Companion.ButtonStyle.STYLE_BTN_RADIAL -> { setupRadialButton(context) }
            Button.Companion.ButtonStyle.STYLE_BTN_RADIAL_W_CENTER -> { setupRadialWithCenterButton(context) }
            null -> throw Exception("Attempted to show 'Setup Button' dialog but tempButton was null.")
            else -> { Log.e("ButtonCreator", "showSetupButtonDialog: Unknown button style: ${Button.nameFromStyle(context, AppState.tempData.tempButton!!.style)}") }
        }
    }

    private fun setupRadialWithCenterButton(context: Context) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun setupRadialButton(context: Context) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun setupVerticalIncrementerButton(context: Context) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun setupRoundSingleActionButton(context: Context) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

/*
----------------------------------------------
    Adapters
----------------------------------------------
*/

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
            val buttonType = Button.buttonStyleFromInt(position) ?: throw Exception("Unknown button style ($position)")

            buttonTitle.text = Button.nameFromStyle(holder.itemView.context, buttonType)
            Glide.with(buttonImage).load(Button.imageResourceFromStyle(buttonType)).into(buttonImage)
            holder.itemView.setOnClickListener {
                AppState.tempData.tempButton = Button()
                    .apply {
                        style = buttonType
                    }

                if (activity != null) {
                    // bound activity means we can show the setup activity for result
                    //activity.startActivityForResult(Intent(activity, ))

                } else {
                    // no bound activity means we show the activity-independent view
                    isTransitioning = true
                    dismissBottomDialog()
                    showSetupButtonDialog(it.context)
                    isTransitioning = false
                }
            }
        }

        inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.v_item_button_type, null))
    }

}