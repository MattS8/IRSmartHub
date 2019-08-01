package com.ms8.smartirhub.android.create_button

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.create_button._old.CBStyleActivity
import com.ms8.smartirhub.android.create_command.ActionSequenceAdapter
import com.ms8.smartirhub.android.create_command.CC_ChooseIrSignalActivity
import com.ms8.smartirhub.android.custom_views.bottom_sheets.BackWarningSheet
import com.ms8.smartirhub.android.custom_views.bottom_sheets.SimpleListDescSheet
import com.ms8.smartirhub.android.custom_views.bottom_sheets.SimpleListDescSheet.Companion.REQ_EDIT_ACTION
import com.ms8.smartirhub.android.custom_views.bottom_sheets.SimpleListDescSheet.Companion.REQ_NEW_ACTION
import com.ms8.smartirhub.android.custom_views.bottom_sheets.PickNameSheet
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Command
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Button.Companion.ADD_TO_END
import com.ms8.smartirhub.android.databinding.ACreateButtonWalkthroughBinding
import com.ms8.smartirhub.android.databinding.VChooseNameSheetBinding
import com.ms8.smartirhub.android.databinding.VSimpleListDescSheetBinding
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import com.ms8.smartirhub.android.learn_signal.LSWalkThroughActivity
import com.ms8.smartirhub.android.utils.MyValidators.ButtonNameValidator

class CBWalkThroughActivity : AppCompatActivity() {
    lateinit var binding: ACreateButtonWalkthroughBinding

    private val warningSheet: BackWarningSheet = BackWarningSheet()
    private val pickNameSheet = PickNameSheet()

/*
    ----------------------------------------------
        PickActionsSheet Logic
    ----------------------------------------------
*/

    private var editingPosition = -1
    val pickActionsSheetAdapter = ActionSequenceAdapter(object : ActionSequenceAdapter.ActionSequenceAdapterCallbacks {
        override fun addNewAction() {
            startActivityForResult(Intent(this@CBWalkThroughActivity, CC_ChooseIrSignalActivity::class.java),
                REQ_NEW_ACTION
            )
        }

        override fun startEditAction(action: Command.Action, position: Int) {
            editingPosition = position
            startActivityForResult(Intent(this@CBWalkThroughActivity, CC_ChooseIrSignalActivity::class.java),
                REQ_EDIT_ACTION
            )
        }
    })
    private val pickActionsSheet = SimpleListDescSheet()

    private val commandListener = object : ObservableList.OnListChangedCallback<ObservableArrayList<Command.Action>>() {
        override fun onChanged(sender: ObservableArrayList<Command.Action>) {
            pickActionsSheetAdapter.actionList = ArrayList(sender)
            pickActionsSheetAdapter.notifyDataSetChanged()
            pickActionsSheet.isSaveEnabled = sender.size > 0
        }

        override fun onItemRangeRemoved(sender: ObservableArrayList<Command.Action>, positionStart: Int, itemCount: Int) {
            pickActionsSheetAdapter.actionList = ArrayList(sender)
            pickActionsSheetAdapter.notifyItemRangeRemoved(positionStart, itemCount)
            pickActionsSheet.isSaveEnabled = sender.size > 0
        }

        override fun onItemRangeMoved(sender: ObservableArrayList<Command.Action>, fromPosition: Int, toPosition: Int, itemCount: Int) {
            pickActionsSheetAdapter.actionList = ArrayList(sender)
            pickActionsSheetAdapter.notifyDataSetChanged()
            pickActionsSheet.isSaveEnabled = sender.size > 0
        }

        override fun onItemRangeInserted(sender: ObservableArrayList<Command.Action>, positionStart: Int, itemCount: Int) {
            pickActionsSheetAdapter.actionList = ArrayList(sender)
            pickActionsSheetAdapter.notifyItemRangeInserted(positionStart, itemCount)
            pickActionsSheet.isSaveEnabled = sender.size > 0
        }

        override fun onItemRangeChanged(sender: ObservableArrayList<Command.Action>, positionStart: Int, itemCount: Int) {
            pickActionsSheetAdapter.actionList = ArrayList(sender)
            pickActionsSheetAdapter.notifyItemRangeChanged(positionStart, itemCount)
            pickActionsSheet.isSaveEnabled = sender.size > 0
        }
    }

/*
    ----------------------------------------------
        Overridden Functions
    ----------------------------------------------
*/

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_ACTIONS_EDITING_POS, editingPosition)
    }

    override fun onBackPressed() {
        when {
            pickNameSheet.isVisible -> {
                pickNameSheet.dismiss()
            }
            binding.prog3.bOnThisStep -> {
                AppState.tempData.tempButton?.let { it.commands = RemoteProfile.Button.newCommandList()}
                determineWalkThroughState()
            }
            binding.prog2.bOnThisStep -> {
                AppState.tempData.tempButton?.name = ""
                determineWalkThroughState()
            }
            warningSheet.bWantsToLeave -> {
                //Note: TempData is not cleared on exit. It is up to the activity that started the Create Button process to clear TempData
                super.onBackPressed()
            }
            !warningSheet.bIsShowing -> {
                warningSheet.show(supportFragmentManager, "WarningBottomSheet")
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        AppState.tempData.tempButton?.commands?.get(0)?.actions?.addOnListChangedCallback(commandListener)
        pickNameSheet.callback = object : PickNameSheet.Callback {
            override fun onSavePressed(sheetBinding: VChooseNameSheetBinding?) {
                sheetBinding?.txtInput?.error = ""
                val isValidName = sheetBinding?.txtInput?.editText!!.text.toString().ButtonNameValidator()
                    .addErrorCallback { sheetBinding.txtInput.error = getString(R.string.err_invalid_button_name) }
                    .check()
                if (isValidName) {
                    AppState.tempData.tempButton?.name = sheetBinding.txtInput.editText!!.text.toString()
                    pickNameSheet.dismiss()
                }
            }

            override fun onDismiss() {
                determineWalkThroughState()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        AppState.tempData.tempButton?.commands?.get(0)?.actions?.removeOnListChangedCallback(commandListener)
        pickNameSheet.callback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.a_create_button_walkthrough)

        if (AppState.tempData.tempButton == null)
            AppState.tempData.tempButton = RemoteProfile.Button()

        //Set up PickNameSheet
        pickNameSheet.nameDesc = getString(R.string.remember_button_name_desc)

        //Set up pickActionsSheet
        pickActionsSheet.sheetTitle = this@CBWalkThroughActivity.getString(R.string.command_title)
        pickActionsSheet.callback = object : SimpleListDescSheet.SimpleListDescSheetCallback {
            override fun onSavePressed(simpleListDescSheet: SimpleListDescSheet, binding: VSimpleListDescSheetBinding) {
                simpleListDescSheet.dismiss()
                determineWalkThroughState()
            }

            override fun onCancelPress(simpleListDescSheet: SimpleListDescSheet, binding: VSimpleListDescSheetBinding) {
                simpleListDescSheet.dismiss()
                determineWalkThroughState()
            }

            override fun onCreateView(binding: VSimpleListDescSheetBinding) {}

            override fun getLayoutManager(): RecyclerView.LayoutManager {
                return LinearLayoutManager(this@CBWalkThroughActivity, RecyclerView.VERTICAL, false)
            }

            override fun getAdapter(): RecyclerView.Adapter<*> {
                return pickActionsSheetAdapter
            }
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = getString(R.string.add_button_title)

        savedInstanceState?.getInt(KEY_ACTIONS_EDITING_POS)?.let { editingPosition = it }

        // Set progress texts
        binding.prog1.description = getString(R.string.prog_button_name)
        binding.prog2.description = getString(R.string.prog_get_actions)
        binding.prog3.description = getString(R.string.prog_button_style)

        // Figure out which step we're on
        determineWalkThroughState()
    }

    @SuppressLint("LogNotTimber")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_NEW_ACTION -> {
                if (resultCode == Activity.RESULT_OK) {
                    val newIrSignalUID = data?.getStringExtra(LSWalkThroughActivity.NEW_IR_SIGNAL_UID) ?: return
                    Log.d("TEST", "got new signal with uid: $newIrSignalUID")
                    AppState.tempData.tempButton!!.commands[0].actions.add(Command.Action().apply {
                        irSignal =  newIrSignalUID })
                    pickActionsSheetAdapter.actionList = ArrayList(AppState.tempData.tempButton?.commands?.get(0)?.actions ?: ArrayList())
                    pickActionsSheetAdapter.notifyDataSetChanged()
                }
            }
            REQ_EDIT_ACTION -> {
                if (resultCode == Activity.RESULT_OK) {
                    val newIrSignalUID = data?.getStringExtra(LSWalkThroughActivity.NEW_IR_SIGNAL_UID) ?: return
                    if (editingPosition != -1) {
                        AppState.tempData.tempButton?.commands?.get(0)?.actions?.removeAt(editingPosition)
                        AppState.tempData.tempButton?.commands?.get(0)?.actions?.add(editingPosition, Command.Action().apply { irSignal = newIrSignalUID })
                    } else { Log.e("ChooseActions", "Returned successfully from REQ_EDIT_ACTION, but editingPosition was -1") }
                }
            }
            REQ_SIG_ACTION -> {
                determineWalkThroughState()
            }
            REQ_STYLE -> {
                if (resultCode == Activity.RESULT_OK) {
                    AppState.tempData.tempButton?.commands?.let {
                        AppState.tempData.tempRemoteProfile.addButton(AppState.tempData.tempButton!!, intent.getIntExtra(EXTRA_BUTTON_POS, ADD_TO_END))
                        AppState.tempData.tempButton = null
                        // Only finish if TempData has a valid button
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
            }
            REQ_NAME -> {
                determineWalkThroughState()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            AppState.tempData.tempButton = null
        }
    }

/*
    ----------------------------------------------
        Layout Functions
    ----------------------------------------------
*/

    private fun determineWalkThroughState() {
        when {
            AppState.tempData.tempButton == null || AppState.tempData.tempButton?.name == "" -> {
                AppState.tempData.tempButton = RemoteProfile.Button()
                binding.prog1.bOnThisStep = true
                binding.prog2.bOnThisStep = false
                binding.prog3.bOnThisStep = false
                binding.btnNextStep.text = getString(R.string.choose_name)
                binding.btnNextStep.setOnClickListener { showPickNameSheet() }
                binding.prog1.setOnClickListener { showPickNameSheet() }
            }
            AppState.tempData.tempButton?.commands?.get(0)?.actions?.size ?: 0 == 0 -> {
                binding.prog1.bOnThisStep = true
                binding.prog2.bOnThisStep = true
                binding.prog3.bOnThisStep = false
                binding.btnNextStep.text = getString(R.string.choose_action)
                binding.btnNextStep.setOnClickListener { getSignalOrAction() }
                binding.prog1.setOnClickListener { getSignalOrAction() }
            }
            else -> {
                binding.prog1.bOnThisStep = true
                binding.prog2.bOnThisStep = true
                binding.prog3.bOnThisStep = true
                binding.btnNextStep.text = getString(R.string.select_style)
                binding.btnNextStep.setOnClickListener { getButtonStyle() }
                binding.prog1.setOnClickListener { getButtonStyle() }
            }
        }
    }

/*
    ----------------------------------------------
        OnClick Functions
    ----------------------------------------------
*/

    private fun showPickNameSheet() {
        if (!pickNameSheet.isVisible) {
            pickNameSheet.show(supportFragmentManager, "PickButtonNameSheet")
        }
    }

    private fun getSignalOrAction() {
        if (!pickActionsSheet.isVisible) {
            pickActionsSheet.show(supportFragmentManager, "PickActionsSheet")
        }
    }

    private fun getButtonStyle() {
        startActivityForResult(Intent(this, CBStyleActivity::class.java), REQ_STYLE)
    }

//    private fun getNameActivity() {
//        startActivityForResult(Intent(this, CBNameActivity::class.java), REQ_NAME)
//    }

    companion object {
        const val REQ_NEW_BUTTON = 9
        const val REQ_NAME = 2
        const val REQ_SIG_ACTION = 3
        const val REQ_STYLE = 4
        const val EXTRA_BUTTON_POS = "EXTRA_BUTTON_POS"


        const val KEY_ACTIONS_EDITING_POS = "KEY_ACT_EDIT_POS"
    }
}
