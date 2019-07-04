package com.ms8.smartirhub.android.custom_views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.andrefrsousa.superbottomsheet.SuperBottomSheetFragment
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.create_command.ActionSequenceAdapter
import com.ms8.smartirhub.android.create_command.CC_ChooseIrSignalActivity
import com.ms8.smartirhub.android.data.Command
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.databinding.VChooseActionsSheetBinding

class PickActionsSheet : SuperBottomSheetFragment() {
    lateinit var binding: VChooseActionsSheetBinding
    var editingPosition = -1


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

    private val callback = object : ActionSequenceAdapter.ActionSequenceAdapterCallbacks {
        override fun addNewAction() {
            startActivityForResult(Intent(activity, CC_ChooseIrSignalActivity::class.java), REQ_NEW_ACTION)
        }

        override fun startEditAction(action: Command.Action, position: Int) {
            editingPosition = position
            startActivityForResult(Intent(activity, CC_ChooseIrSignalActivity::class.java), REQ_EDIT_ACTION)
        }
    }
    val adapter = ActionSequenceAdapter(callback)


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_EDIT_POS, editingPosition)
    }

    override fun onPause() {
        super.onPause()
        TempData.tempButton?.command?.actions?.removeOnListChangedCallback(commandListener)
    }

    override fun onResume() {
        super.onResume()
        TempData.tempButton?.command?.actions?.addOnListChangedCallback(commandListener)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        editingPosition = savedInstanceState?.getInt(KEY_EDIT_POS) ?: editingPosition

        binding = DataBindingUtil.inflate(inflater, R.layout.v_choose_actions_sheet, container, false)

        binding.actionsList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.actionsList.adapter = adapter

        binding.btnSaveCommand.setOnClickListener { saveCommand() }


        return binding.root
    }

    private fun saveCommand() {
        Log.d("TEST", "SAVE COMMAND")
    }

    fun replaceActionWithIrSignal(newIrSignalUID: String) {
        if (editingPosition != -1) {
            TempData.tempButton?.command?.actions?.removeAt(editingPosition)
            TempData.tempButton?.command?.actions?.add(editingPosition, Command.Action().apply { irSignal = newIrSignalUID })
        } else { Log.e("ChooseActions", "Returned successfully from REQ_EDIT_ACTION, but editingPosition was -1") }
    }

    companion object {
        const val REQ_NEW_ACTION = 50
        const val REQ_EDIT_ACTION = 51

        const val KEY_EDIT_POS = "KEY_EDIT_POS"
    }
}