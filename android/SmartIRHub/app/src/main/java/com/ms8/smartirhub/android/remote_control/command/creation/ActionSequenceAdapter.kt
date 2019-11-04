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
import com.ms8.smartirhub.android.databinding.VRmtBtnCreateNewBinding
import com.ms8.smartirhub.android.models.firestore.Hub.Companion.DEFAULT_HUB

class ActionSequenceAdapter(var callback: ActionSequenceAdapterCallbacks?) : RecyclerView.Adapter<ActionSequenceAdapter.ActionViewHolder>() {
    var actionList = ArrayList<Command.Action>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
       val v = when (viewType) {
            VIEW_TYPE_ADD_ACTION -> VRmtBtnCreateNewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                .apply { btnRmtCreateNew.setText(R.string.add_action) }
                .btnCreateNewRoot
           else -> LayoutInflater.from(parent.context).inflate(R.layout.v_action_sequence_item, parent, false)
        }

        return ActionViewHolder(v)
    }

    override fun getItemCount() = actionList.size + 1

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        when (position) {
        // Create New Action View
            itemCount - 1 -> holder.itemView.setOnClickListener { callback?.addNewAction() }
        // Action Sequence Item
            else -> {
                holder.itemView.setOnClickListener { callback?.startEditAction(actionList[holder.adapterPosition], holder.adapterPosition) }
                holder.bindAction(actionList[position])
                holder.itemView.findViewById<ImageButton>(R.id.btnDeleteAction).setOnClickListener {
                    AppState.tempData.tempButton.get()?.commands?.get(0)?.actions?.removeAt(holder.adapterPosition)
                }
                // Show delay input for actions with additional actions after
                if (position > 0 && position < itemCount - 2) {
                    holder.itemView.findViewById<View>(R.id.view).visibility = View.VISIBLE
                    holder.itemView.findViewById<View>(R.id.view2).visibility = View.VISIBLE
                    holder.itemView.findViewById<TextView>(R.id.tvDelay).visibility = View.VISIBLE
                    val seekBar = holder.itemView.findViewById<SeekBar>(R.id.seekBar)
                    seekBar.visibility = View.VISIBLE
                    seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                            actionList[holder.adapterPosition].delay = p1
                            notifyItemChanged(holder.adapterPosition)
                        }

                        override fun onStartTrackingTouch(p0: SeekBar?) {}

                        override fun onStopTrackingTouch(p0: SeekBar?) {}

                    })
                }
                // Otherwise hide those views
                else {
                    holder.itemView.findViewById<View>(R.id.view).visibility = View.GONE
                    holder.itemView.findViewById<View>(R.id.view2).visibility = View.GONE
                    holder.itemView.findViewById<TextView>(R.id.tvDelay).visibility = View.GONE
                    holder.itemView.findViewById<SeekBar>(R.id.seekBar).visibility = View.GONE
                }
            }
        }
    }

    override fun getItemViewType(position: Int) =
        when (position) {
            itemCount - 1 -> VIEW_TYPE_ADD_ACTION
            else -> VIEW_TYPE_ACTION
        }


    class ActionViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {

        fun bindAction(action : Command.Action) {
            itemView.findViewById<TextView>(R.id.tvActionTitle).text = AppState.userData.irSignals[action.irSignal]?.name
            val targetHub = if (action.hubUID == DEFAULT_HUB) AppState.userData.hubs[AppState.userData.user.defaultHub] ?: "Default Hub" else action.hubUID
            val desc = itemView.context.getString(R.string.send_signal_to) + " " + targetHub
            itemView.findViewById<TextView>(R.id.tvActionDesc).text = desc
        }
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