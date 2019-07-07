package com.ms8.smartirhub.android.create_remote_profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.models.firestore.RemoteProfileTemplate
import com.ms8.smartirhub.android.database.LocalData

class RemoteTemplateAdapter(var templateCallback: RemoteTemplateAdapterCallback): RecyclerView.Adapter<RemoteTemplateAdapter.RemoteTemplateViewHolder>() {
    private val listnener = object : ObservableMap.OnMapChangedCallback<ObservableArrayMap<String, RemoteProfileTemplate>, String, RemoteProfileTemplate>() {
        override fun onMapChanged(sender: ObservableArrayMap<String, RemoteProfileTemplate>?, key: String?) {
            if (sender != null) {
                list = ArrayList(sender.values)
            }
            notifyDataSetChanged()
        }
    }
    var list = ArrayList<RemoteProfileTemplate>()

    init {
        LocalData.remoteProfileTemplates.addOnMapChangedCallback(listnener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RemoteTemplateViewHolder {
        return RemoteTemplateViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.v_remote_template_item, parent, false))
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: RemoteTemplateViewHolder, position: Int) {
        holder.bind(list[position])
        holder.itemView.setOnClickListener { templateCallback.templateSelected(list[holder.adapterPosition]) }
    }

    fun listen() {
        LocalData.remoteProfileTemplates.addOnMapChangedCallback(listnener)
        list = ArrayList(LocalData.remoteProfileTemplates.values)
        notifyDataSetChanged()
    }

    fun stopListening() {
        LocalData.remoteProfileTemplates.removeOnMapChangedCallback(listnener)
    }

    class RemoteTemplateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(template: RemoteProfileTemplate) {
            itemView.findViewById<TextView>(R.id.tvTitle).text = template.name
            val imageView = itemView.findViewById<ImageView>(R.id.imgTemplatePreview)
            Glide.with(itemView).load(template.previewURL).placeholder(R.drawable.remote_template_d1_552).into(imageView)
        }
    }

    interface RemoteTemplateAdapterCallback {
        fun templateSelected(template: RemoteProfileTemplate)
    }
}