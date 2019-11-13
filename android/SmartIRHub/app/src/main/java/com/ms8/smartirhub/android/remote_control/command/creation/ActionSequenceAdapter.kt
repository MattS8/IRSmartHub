package com.ms8.smartirhub.android.remote_control.command.creation

import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.IndicatorViewBinding
import com.ms8.smartirhub.android.databinding.VActionSequenceItemBinding
import com.ms8.smartirhub.android.databinding.VBtnAddActionBinding
import com.ms8.smartirhub.android.firebase.FirebaseConstants.ACTION_DELAY_MAX
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Command
import com.ms8.smartirhub.android.utils.extensions.hideKeyboard
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams


class ActionSequenceAdapter(var callback: ActionSequenceAdapterCallbacks?,
                            var actionList: ArrayList<Command.Action> = ArrayList())
    : RecyclerView.Adapter<ActionSequenceAdapter.ActionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
        return when (viewType) {
            VIEW_TYPE_ACTION ->
            {
                val binding = VActionSequenceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

                ActionViewHolder(binding.actionRoot, binding)
            }
            VIEW_TYPE_ADD_ACTION ->
            {
                val binding = VBtnAddActionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    .apply {
                        tvAddAction.setOnClickListener { callback?.addNewAction() }
                    }

                ActionViewHolder(binding.btnAddActionRoot)
            }
            else -> throw Exception("Unknown action sequence view type found! ($viewType)")
        }
    }

    override fun getItemCount() = actionList.size + 1

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_ACTION -> bindAction(holder, actionList[position], position)
            VIEW_TYPE_ADD_ACTION -> holder.itemView.setOnClickListener { callback?.addNewAction() }
            else -> throw Exception("Unknown action sequence view type found! (${holder.itemViewType})")
        }
    }

    override fun getItemViewType(position: Int) =
        when (position) {
            itemCount - 1 -> VIEW_TYPE_ADD_ACTION
            else -> VIEW_TYPE_ACTION
        }

    private fun bindAction(holder: ActionViewHolder, action: Command.Action, position: Int) {
        holder.itemView.setOnClickListener { callback?.startEditAction(action, holder.adapterPosition) }
        val irSignal = AppState.userData.irSignals[action.irSignal]
        val hub = AppState.getHub(action.hubUID)

        holder.binding?.apply {
            irSignal?.let {
                tvActionTitle.text = it.code
                val descText = holder.itemView.context.getString(R.string.target_smart_hub) + " ${hub.name}"
                tvActionDesc.text = descText
            }

            btnDeleteAction.setOnClickListener { removeAction(holder.adapterPosition) }

            if (shouldShowDelayViews(position))
                showDelayViews(this, action, holder)
            else
                hideDelayViews(this)
        }
    }

    private fun hideDelayViews(binding: VActionSequenceItemBinding) {
        binding.apply {
            view.visibility = View.GONE
            view2.visibility = View.GONE
//            etDelay.visibility = View.GONE
            tvDelay.visibility = View.GONE
            seekBar.visibility = View.GONE
            seekBarRoot.visibility = View.GONE
        }
    }

    private fun showDelayViews(
        binding: VActionSequenceItemBinding,
        action: Command.Action,
        holder: ActionViewHolder
    ) {
        binding.apply {
            view.visibility = View.VISIBLE
            view2.visibility = View.VISIBLE
            tvDelay.visibility = View.VISIBLE
            seekBar.apply {
                visibility = View.VISIBLE
                val etIndicator = IndicatorViewBinding.inflate(LayoutInflater.from(seekBar.context))
                indicator.setTopContentView(etIndicator.seekBarIndicatorLayout, etIndicator.etDelay)
                Log.d("TEST_", "setting progress to ${action.delay.toFloat()}")
                setProgress(action.delay.toFloat())
                etIndicator.etDelay.setText(action.delay.toString())
                onSeekChangeListener = object : OnSeekChangeListener {
                    override fun onSeeking(seekParams: SeekParams?) {}

                    override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {}

                    override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {
                        Log.d("TEST", "onStopTrackingTouch... ${seekBar?.progress}")
                        seekBar?.progress?.let {
                            action.delay = it
                            notifyItemChanged(holder.adapterPosition)
                        }
                    }
                }
                etIndicator.etDelay.setOnEditorActionListener { tv, actionId, keyEvent -> Boolean
                    if (actionId != EditorInfo.IME_ACTION_DONE && keyEvent?.action != KeyEvent.KEYCODE_ENTER) {
                        Log.d("TEST", "actionID ($actionId) != EditorInfo.IME_ACTION_DONE (${EditorInfo.IME_ACTION_DONE}) && KeyEvent (${keyEvent?.action}) != KeyEvent.KEYCODE_ENTER")
                        false
                    }
                    else {
                        val newDelay = etIndicator.etDelay.text.toString().toIntOrNull() ?: -1
                        var logStr = "Text is changing to $newDelay... "

                        if (!newDelay.isValidDelay()) {
                            setProgress(0f)
                            logStr += "is NOT VALID!"
                        } else if (newDelay != progress) {
                            AppState.tempData.tempCommand?.actions?.get(holder.adapterPosition)?.delay = newDelay
                            notifyItemChanged(holder.adapterPosition)
                            logStr += "progress changed from $progress to ${newDelay.toFloat()}"
                            tv.hideKeyboard()
                        }
                        Log.d("TEST", logStr)
                        true
                    }
                }
            }
        }
    }

    private fun removeAction(adapterPosition: Int) {
        AppState.tempData.tempCommand?.actions?.removeAt(adapterPosition)
        notifyItemRemoved(adapterPosition)

        when (itemCount) {
            1 -> callback?.onNoActionsLeft()
            2 -> notifyItemChanged(0)
        }
    }

    private fun shouldShowDelayViews(position: Int) = itemCount > 1 && position < itemCount - 2

    class ActionViewHolder(itemView : View, var binding: VActionSequenceItemBinding? = null) : RecyclerView.ViewHolder(itemView)

    interface ActionSequenceAdapterCallbacks {
        fun addNewAction()
        fun startEditAction(action: Command.Action, position: Int)
        fun onNoActionsLeft()
    }

    companion object {
        const val VIEW_TYPE_ADD_ACTION = 2
        const val VIEW_TYPE_ACTION = 1
    }
}

private fun Int.isValidDelay(): Boolean {
    return (this in 0..ACTION_DELAY_MAX)
}
