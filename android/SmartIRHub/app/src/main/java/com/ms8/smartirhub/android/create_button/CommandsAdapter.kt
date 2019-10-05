//package com.ms8.smartirhub.android.create_button
//
//import android.view.View
//import android.view.ViewGroup
//import androidx.recyclerview.widget.RecyclerView
//import com.ms8.smartirhub.android.database.AppState
//import com.ms8.smartirhub.android.firebase.FirestoreActions
//
//class CommandsAdapter: RecyclerView.Adapter<CommandsAdapter.CommandsListViewHolder>() {
//
//    init {
//        if (AppState.userData.remotes.size == 0) {
//            FirestoreActions.getRemoteTemplates()
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommandsListViewHolder {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun getItemCount(): Int {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun onBindViewHolder(holder: CommandsListViewHolder, position: Int) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    class CommandsListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//
//    }
//}