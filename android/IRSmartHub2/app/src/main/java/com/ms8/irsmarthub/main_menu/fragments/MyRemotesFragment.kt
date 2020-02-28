package com.ms8.irsmarthub.main_menu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.ms8.irsmarthub.R
import com.ms8.irsmarthub.database.AppState
import com.ms8.irsmarthub.database.FirestoreFunctions
import com.ms8.irsmarthub.databinding.FRemotesAllBinding
import com.ms8.irsmarthub.databinding.IvRemotesAllBinding
import com.ms8.irsmarthub.main_menu.MainActivity
import com.ms8.irsmarthub.remote_control.remote.models.Remote
import com.ms8.irsmarthub.remote_control.remote.models.RemotePermissions

class MyRemotesFragment: MainFragment() {
    override fun newInstance(): MainFragment { return MyRemotesFragment() }

    lateinit var binding: FRemotesAllBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FRemotesAllBinding.inflate(inflater, null, false)

        return binding.root
    }

    companion object {
        const val recyclerViewTag = "AllRemotesRV"
    }

    private val adapter = MyRemotesAdapter()
    inner class MyRemotesAdapter: RecyclerView.Adapter<MyRemotesAdapter.MyRemotesViewHolder>() {
        var remotes = ArrayList<Remote>()
        set(value) {
            // Puts the favorite remote at the top of the list
            AppState.tempData.tempUser.favRemote.get()?.let {favRemoteUID ->
                for (i in 0..value.size) {
                    if (favRemoteUID == value[i].uid) {
                        val temp = value.removeAt(i)
                        value.add(0, temp)
                        break
                    }
                }
            }

            val diffCallback = RemotesDiffCallback(field, value)
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            field.clear()
            field.addAll(value)
            diffResult.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyRemotesViewHolder {
            return MyRemotesViewHolder(parent)
        }

        override fun getItemCount() = remotes.size

        override fun onBindViewHolder(holder: MyRemotesViewHolder, position: Int) {
            val remote = remotes[position]
            val isFavorite = AppState.tempData.tempUser.favRemote.get() == remote.uid
            val binding = IvRemotesAllBinding.bind(holder.itemView)

            binding.btnFavorite.apply {
                setImageDrawable(context.getDrawable(
                    if (isFavorite)
                        R.drawable.ic_star_black_24dp
                    else
                        R.drawable.ic_star_border_black_24dp
                ))
                imageTintList =
                    if (isFavorite)
                        ContextCompat.getColorStateList(context, R.color.selectable_fav_icon)
                    else
                        ContextCompat.getColorStateList(context, R.color.md_grey_800)
                setOnClickListener {
                    AppState.tempData.tempUser.favRemote.set(remote.uid)
                    FirestoreFunctions.User.setUser()
                    remotes = ArrayList<Remote>().apply {
                        addAll(AppState.userData.remotes.values)
                    }
                }
            }
            binding.tvRemoteName.apply {
                text = remote.name
                setOnClickListener {
                    AppState.tempData.tempRemote.copyFrom(remote)
                    (activity as MainActivity).switchInnerPage(0)
                }
            }
            binding.ivRemotesAllRoot.apply {
                setOnClickListener {
                    AppState.tempData.tempRemote.copyFrom(remote)
                    (activity as MainActivity).switchInnerPage(0)
                }
            }
            binding.tvEditRemoteButton.apply {
                FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                    val hasEditingPermission = remote.userPermissions[uid]?.permission?.ordinal
                            ?: 0 >= RemotePermissions.Companion.PermissionLevel.READ_WRITE.ordinal
                    visibility = if (hasEditingPermission) View.VISIBLE else View.GONE
                    isEnabled = hasEditingPermission
                }
            }
        }

        inner class RemotesDiffCallback
            (private val oldList: List<Remote>,
             private val newList: List<Remote>
        ): DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition].uid == newList[newItemPosition].uid
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = oldList[oldItemPosition]
                val newItem = newList[newItemPosition]

                return oldItem.uid == newItem.uid && oldItem.name == newItem.name
            }

            override fun getOldListSize() = oldList.size

            override fun getNewListSize() = newList.size

        }

        inner class MyRemotesViewHolder(parent: ViewGroup): RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_inc_vert, parent, false)
        )
    }
}