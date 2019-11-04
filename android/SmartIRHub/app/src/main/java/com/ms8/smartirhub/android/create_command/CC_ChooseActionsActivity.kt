package com.ms8.smartirhub.android.create_command

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
import com.ms8.smartirhub.android.create_command.CC_ChooseIrSignalActivity.Companion.REQ_EDIT_ACTION
import com.ms8.smartirhub.android.create_command.CC_ChooseIrSignalActivity.Companion.REQ_NEW_ACTION
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.ACcChooseActionBinding
import com.ms8.smartirhub.android.learn_signal.LSWalkThroughActivity
import com.ms8.smartirhub.android.remote_control.button.models.Button
import com.ms8.smartirhub.android.remote_control.command.creation.ActionSequenceAdapter
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Command

class CC_ChooseActionsActivity : AppCompatActivity() {
    lateinit var binding: ACcChooseActionBinding
    var editingPosition = -1
/*
    ----------------------------------------------
        Listeners
    ----------------------------------------------
*/

    private val callback = object : ActionSequenceAdapter.ActionSequenceAdapterCallbacks {
        override fun addNewAction() {
            startActivityForResult(Intent(this@CC_ChooseActionsActivity, CC_ChooseIrSignalActivity::class.java), REQ_NEW_ACTION)
        }

        override fun startEditAction(action: Command.Action, position: Int) {
            editingPosition = position
            startActivityForResult(Intent(this@CC_ChooseActionsActivity, CC_ChooseIrSignalActivity::class.java), REQ_EDIT_ACTION)
        }
    }

    private val commandListener = object : ObservableList.OnListChangedCallback<ObservableArrayList<Command.Action>>() {
        override fun onChanged(sender: ObservableArrayList<Command.Action>) {
            adapter.actionList = ArrayList(sender)
            binding.btnSaveCommand.isEnabled = sender.size > 0
        }

        override fun onItemRangeRemoved(sender: ObservableArrayList<Command.Action>, positionStart: Int, itemCount: Int) {
            adapter.actionList = ArrayList(sender)
            adapter.notifyItemRangeRemoved(positionStart, itemCount)
            binding.btnSaveCommand.isEnabled = sender.size > 0
        }

        override fun onItemRangeMoved(sender: ObservableArrayList<Command.Action>, fromPosition: Int, toPosition: Int, itemCount: Int) {
            adapter.actionList = ArrayList(sender)
            adapter.notifyDataSetChanged()
            binding.btnSaveCommand.isEnabled = sender.size > 0
        }

        override fun onItemRangeInserted(sender: ObservableArrayList<Command.Action>, positionStart: Int, itemCount: Int) {
            adapter.actionList = ArrayList(sender)
            adapter.notifyItemRangeInserted(positionStart, itemCount)
            binding.btnSaveCommand.isEnabled = sender.size > 0
        }

        override fun onItemRangeChanged(sender: ObservableArrayList<Command.Action>, positionStart: Int, itemCount: Int) {
            adapter.actionList = ArrayList(sender)
            adapter.notifyItemRangeChanged(positionStart, itemCount)
            binding.btnSaveCommand.isEnabled = sender.size > 0
        }
    }

    val adapter =
        ActionSequenceAdapter(callback)

/*
    ----------------------------------------------
        Overridden Functions
    ----------------------------------------------
*/

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_EDITING_POS, editingPosition)
    }

    @SuppressLint("LogNotTimber")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_NEW_ACTION -> {
                if (resultCode == Activity.RESULT_OK) {
                    val newIrSignalUID = data?.getStringExtra(LSWalkThroughActivity.NEW_IR_SIGNAL_UID) ?: return
                    AppState.tempData.tempButton.get()?.commands?.get(0)?.actions?.add(Command.Action().apply { irSignal =  newIrSignalUID})
                }
            }
            REQ_EDIT_ACTION -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (editingPosition != -1) {
                        val newIrSignalUID = data?.getStringExtra(LSWalkThroughActivity.NEW_IR_SIGNAL_UID) ?: return
                        AppState.tempData.tempButton.get()?.commands?.get(0)?.actions?.removeAt(editingPosition)
                        AppState.tempData.tempButton.get()?.commands?.get(0)?.actions?.add(editingPosition, Command.Action().apply { irSignal = newIrSignalUID })
                    } else {
                        Log.e("ChooseActions", "Returned successfully from REQ_EDIT_ACTION, but editingPosition was -1")
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        AppState.tempData.tempButton.get()?.commands?.get(0)?.actions?.removeOnListChangedCallback(commandListener)
    }

    override fun onResume() {
        super.onResume()
        AppState.tempData.tempButton.get()?.commands?.get(0)?.actions?.addOnListChangedCallback(commandListener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppState.tempData.tempButton.get()?.commands == null)
            AppState.tempData.tempButton.get()?.commands = Button.newCommandList()

        binding = DataBindingUtil.setContentView(this, R.layout.a_cc_choose_action)
        binding.actionsList.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.actionsList.adapter = adapter
        binding.btnSaveCommand.isEnabled = AppState.tempData.tempButton.get()?.commands?.get(0)?.actions?.size ?: 0 > 0
        binding.btnSaveCommand.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Restore state
        editingPosition = savedInstanceState?.getInt(KEY_EDITING_POS) ?: -1
    }

    companion object {
        const val KEY_EDITING_POS = "KEY_EDITING_POS"
    }
}
