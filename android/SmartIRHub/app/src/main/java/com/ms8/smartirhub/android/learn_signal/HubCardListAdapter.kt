package com.ms8.smartirhub.android.learn_signal

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableMap
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.data.Hub
import com.ms8.smartirhub.android.database.LocalData
import com.ms8.smartirhub.android.databinding.VHubCardBinding
import com.ms8.smartirhub.android.setup_hub.HubSetupMainActivity
import com.ms8.smartirhub.android.setup_hub.HubSetupMainActivity.Companion.RC_HUB_SETUP_MAIN
import java.lang.ref.WeakReference

//TODO remove this weak reference
class HubCardListAdapter : RecyclerView.Adapter<HubCardListAdapter.HubCardViewHolder>() {
    val list = ArrayList<Hub>(LocalData.hubs.values)
    var selectedItem = 0
    var setupNewHub : ObservableBoolean = ObservableBoolean(false)
    var callback: HubCardListAdapter.Callback? = null

    private val listener = object : ObservableMap.OnMapChangedCallback<ObservableMap<String, Hub>, String, Hub>() {
        @SuppressLint("LogNotTimber")
        override fun onMapChanged(sender: ObservableMap<String, Hub>?, key: String?) {
            if (sender != null) {
                when {
                    // Item inserted or changed
                    sender.containsKey(key) -> {
                        val existingPos = list.indexOf(sender[key])
                        // Wasn't previously in list
                        if (existingPos == -1) {
                            list.add(sender[key]!!)
                            notifyItemInserted(list.size-1)
                        }
                        // Was previously in list
                        else {
                            list.removeAt(existingPos)
                            list.add(existingPos, sender[key]!!)
                            notifyItemChanged(existingPos)
                        }
                    }
                    // Item removed
                    else -> {
                        val pos = list.indexOf(sender[key])
                        if (pos != -1) {
                            list.removeAt(pos)
                            notifyItemRemoved(pos)
                        } else {
                            notifyDataSetChanged()
                            Log.w("HubCardListAdapter", "Tried removing hub not in list but in LocalData... ${sender[key]?.uid}")
                        }
                    }
                }
            } else {
                notifyDataSetChanged()
            }
        }
    }

    init {
        LocalData.hubs.addOnMapChangedCallback(listener)
    }

    fun listen(shouldListen: Boolean) {
        if (shouldListen) {
            LocalData.hubs.addOnMapChangedCallback(listener)
        } else {
            LocalData.hubs.removeOnMapChangedCallback(listener)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HubCardViewHolder {
        return HubCardViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.v_hub_card, parent, false))
    }

    override fun getItemCount() = list.size + 1

    override fun onBindViewHolder(holder: HubCardViewHolder, position: Int) {
        when (position) {
        // Last item is "Set Up New Hub" option
            list.size -> {
                val hub = Hub().apply {
                    name = holder.itemView.context.getString(R.string.setup_new_hub_drp_dwn)
                    owner = holder.itemView.context.getString(R.string.setup_new_hub_desc)
                }
                holder.bind(hub, selectedItem == position)
                holder.binding.hubCard.setOnClickListener {callback?.newHubClicked() }

                // override string concatenation done in databinding
                holder.binding.tvOwner.text = hub.owner
            }
        // Every other view is a hub
            else -> {
                holder.bind(list[position], selectedItem == position)

                holder.binding.hubCard.setOnClickListener {
                    selectViewHolder(holder)
                }
            }
        }
    }

    private fun selectViewHolder(holder: HubCardViewHolder) {
        val oldItem = selectedItem
        selectedItem = holder.adapterPosition
        holder.setSelected(true)
        notifyItemChanged(holder.adapterPosition)
        notifyItemChanged(oldItem)
    }


    class HubCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var binding: VHubCardBinding = DataBindingUtil.bind(itemView)!!

        fun bind(hub: Hub, selectedItem: Boolean) {
            binding.hub = hub
            val setupDesc = itemView.context.getString(R.string.setup_new_hub_desc)
            if (hub.owner == setupDesc)  {
                binding.tvOwner.text = setupDesc
            } else {
                val ownerText = itemView.context.getString(R.string.set_up_by) + " " +  hub.ownerUsername
                binding.tvOwner.text = ownerText
            }
            setSelected(selectedItem)
        }

        fun setSelected(isSelected: Boolean) {
            binding.hubCard.isSelected = isSelected
//            binding.tvOwner.isSelected = isSelected
//            binding.tvHubName.isSelected = isSelected
            when (isSelected) {
                true -> {
                    binding.hubCard.cardElevation = 64f
                }
                false -> {
                    binding.hubCard.cardElevation = 0f
                }
            }
        }
    }

    interface Callback {
        fun newHubClicked()
    }
}