package com.ms8.smartirhub.android.remote_control.command.creation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile.Command
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.VActionSequenceItemBinding
import com.ms8.smartirhub.android.databinding.VRmtBtnCreateNewBinding
import com.ms8.smartirhub.android.models.firestore.Hub.Companion.DEFAULT_HUB
import org.jetbrains.anko.sdk27.coroutines.onSeekBarChangeListener

class ActionSequenceAdapter(var callback: ActionSequenceAdapterCallbacks?,
                            var actionList: ArrayList<Command.Action> = ArrayList())
    : RecyclerView.Adapter<ActionSequenceAdapter.ActionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
       val v = when (viewType) {
            VIEW_TYPE_ADD_ACTION -> VRmtBtnCreateNewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                .apply {
                    btnRmtCreateNew.setText(R.string.add_action)
                    btnRmtCreateNew.setOnClickListener { callback?.addNewAction() }
                }
                .btnCreateNewRoot
           else -> LayoutInflater.from(parent.context).inflate(R.layout.v_action_sequence_item, parent, false)
        }

        return ActionViewHolder(v)
    }

    override fun getItemCount() = actionList.size + 1

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        when (position) {
        // Create New Action View
            (itemCount - 1) -> holder.itemView.setOnClickListener { callback?.addNewAction() }
        // Action Sequence Item
            else -> bindAction(holder, actionList[position], position)
        }
    }

    override fun getItemViewType(position: Int) =
        when (position) {
            itemCount - 1 -> VIEW_TYPE_ADD_ACTION
            else -> VIEW_TYPE_ACTION
        }

    private fun bindAction(holder: ActionViewHolder, action: Command.Action, position: Int) {
        holder.itemView.setOnClickListener { callback?.startEditAction(action, holder.adapterPosition) }
        val binding = VActionSequenceItemBinding.bind(holder.itemView)
        val irSignal = AppState.userData.irSignals[action.irSignal]
        val hub = AppState.getHub(action.hubUID)

        binding?.apply {
            irSignal?.let {
                tvActionTitle.text = it.code
                val descText = holder.itemView.context.getString(R.string.target_smart_hub) + ": ${hub.name}"
                tvActionDesc.text = descText
            }

            btnDeleteAction.setOnClickListener { AppState.tempData.tempCommand?.actions?.removeAt(holder.adapterPosition) }

            if (shouldShowDelayViews(position))
                showDelayViews(binding, action, holder)
            else
                hideDelayViews(binding)
        }
    }

    private fun hideDelayViews(binding: VActionSequenceItemBinding) {
        binding.apply {
            view.visibility = View.GONE
            view2.visibility = View.GONE
            tvDelay.visibility = View.GONE
            seekBar.visibility = View.GONE
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
                onSeekBarChangeListener { object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        action.delay = p1
                        notifyItemChanged(holder.adapterPosition)
                    }
                    override fun onStartTrackingTouch(p0: SeekBar?) {}
                    override fun onStopTrackingTouch(p0: SeekBar?) {}
                } }
            }
        }
    }

    private fun shouldShowDelayViews(position: Int) = itemCount > 1 && position < itemCount - 2

    class ActionViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {

//        fun bindAction(action : Command.Action) {
//            itemView.findViewById<TextView>(R.id.tvActionTitle).text = AppState.userData.irSignals[action.irSignal]?.name
//            val targetHub = if (action.hubUID == DEFAULT_HUB) AppState.userData.hubs[AppState.userData.user.defaultHub] ?: "Default Hub" else action.hubUID
//            val desc = itemView.context.getString(R.string.send_signal_to) + " " + targetHub
//            itemView.findViewById<TextView>(R.id.tvActionDesc).text = desc
//        }
    }

    interface ActionSequenceAdapterCallbacks {
        fun addNewAction()
        fun startEditAction(action: Command.Action, position: Int)
    }

    companion object {
        const val VIEW_TYPE_ADD_ACTION = 2
        const val VIEW_TYPE_ACTION = 1
    }
}