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
import com.ms8.smartirhub.android.create_command.ActionSequenceAdapter
import com.ms8.smartirhub.android.create_command.CC_ChooseIrSignalActivity
import com.ms8.smartirhub.android.custom_views.bottom_sheets.BackWarningSheet
import com.ms8.smartirhub.android.custom_views.bottom_sheets.SimpleListDescSheet
import com.ms8.smartirhub.android.custom_views.bottom_sheets.SimpleListDescSheet.Companion.REQ_EDIT_ACTION
import com.ms8.smartirhub.android.custom_views.bottom_sheets.SimpleListDescSheet.Companion.REQ_NEW_ACTION
import com.ms8.smartirhub.android.custom_views.bottom_sheets.PickNameSheet
import com.ms8.smartirhub.android.data.Command
import com.ms8.smartirhub.android.data.RemoteProfile
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.databinding.ACreateButtonWalkthroughBinding
import com.ms8.smartirhub.android.learn_signal.LSWalkThroughActivity

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
    private val pickActionsSheet = SimpleListDescSheet().apply {
        adapter = pickActionsSheetAdapter
        layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    }

    private val commandListener = object : ObservableList.OnListChangedCallback<ObservableArrayList<Command.Action>>() {
        override fun onChanged(sender: ObservableArrayList<Command.Action>) {
            pickActionsSheetAdapter.actionList = ArrayList(sender)
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

    private val actionSequenceAdapterCallback = object : ActionSequenceAdapter.ActionSequenceAdapterCallbacks {
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
    }
    val pickActionsSheetAdapter = ActionSequenceAdapter(actionSequenceAdapterCallback)

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
                TempData.tempButton?.let { it.command = Command() }
                determineWalkThroughState()
            }
            binding.prog2.bOnThisStep -> {
                TempData.tempButton?.name = ""
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
        TempData.tempButton?.command?.actions?.addOnListChangedCallback(commandListener)
        pickNameSheet.callback = object : PickNameSheet.Callback {
            override fun onDismiss() {
                determineWalkThroughState()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        TempData.tempButton?.command?.actions?.removeOnListChangedCallback(commandListener)
        pickNameSheet.callback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.a_create_button_walkthrough)

        if (TempData.tempButton == null)
            TempData.tempButton = RemoteProfile.Button()

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
                    TempData.tempButton?.command?.actions?.add(Command.Action().apply { irSignal =  newIrSignalUID})
                }
            }
            REQ_EDIT_ACTION -> {
                if (resultCode == Activity.RESULT_OK) {
                    val newIrSignalUID = data?.getStringExtra(LSWalkThroughActivity.NEW_IR_SIGNAL_UID) ?: return
                    if (editingPosition != -1) {
                        TempData.tempButton?.command?.actions?.removeAt(editingPosition)
                        TempData.tempButton?.command?.actions?.add(editingPosition, Command.Action().apply { irSignal = newIrSignalUID })
                    } else { Log.e("ChooseActions", "Returned successfully from REQ_EDIT_ACTION, but editingPosition was -1") }
                }
            }
            REQ_SIG_ACTION -> {
                determineWalkThroughState()
            }
            REQ_STYLE -> {
                if (resultCode == Activity.RESULT_OK) {
                    TempData.tempButton?.command?.let {
                        TempData.tempRemoteProfile.addButton(TempData.tempButton!!, intent.getIntExtra(EXTRA_BUTTON_POS, -1))
                        TempData.tempButton = null
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
            TempData.tempButton = null
        }
    }

/*
    ----------------------------------------------
        Layout Functions
    ----------------------------------------------
*/

    private fun determineWalkThroughState() {
        when {
            TempData.tempButton == null || TempData.tempButton?.name == "" -> {
                TempData.tempButton = RemoteProfile.Button()
                binding.prog1.bOnThisStep = true
                binding.prog2.bOnThisStep = false
                binding.prog3.bOnThisStep = false
                binding.btnNextStep.text = getString(R.string.choose_name)
                binding.btnNextStep.setOnClickListener { showPickNameSheet() }
                binding.prog1.setOnClickListener { showPickNameSheet() }
            }
            TempData.tempButton?.command?.actions?.size ?: 0 == 0 -> {
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
