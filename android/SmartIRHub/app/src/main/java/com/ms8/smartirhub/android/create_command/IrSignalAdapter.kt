package com.ms8.smartirhub.android.create_command

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.data.IrSignal
import com.ms8.smartirhub.android.databinding.VIrSignalItemBinding

class IrSignalAdapter: RecyclerView.Adapter<IrSignalAdapter.IrActionViewHolder>() {
    var signalList = ArrayList<IrSignal>()
    var callback: Callback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IrActionViewHolder {
        return IrActionViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.v_ir_signal_item, parent, false))
    }

    override fun getItemCount() = signalList.size

    override fun onBindViewHolder(holder: IrActionViewHolder, position: Int) {
        holder.bind(signalList[position])
        holder.itemView.setOnClickListener { callback?.signalSelected(holder.irSignal) }
    }

    class IrActionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var irSignal: IrSignal? = null
        var binding: VIrSignalItemBinding? = null

        fun bind(irSignal: IrSignal) {
            this.irSignal = irSignal
            binding = DataBindingUtil.bind(itemView)

            binding?.let {
                val tempStr = itemView.context.getString(R.string.code) + " " + irSignal.code
                it.tvIrSignalCode.text = tempStr
                it.tvSignalTitle.text = irSignal.name
            }
        }

    }

    interface Callback {
        fun signalSelected(irSignal : IrSignal?)
    }
}