package com.ms8.smartirhub.android.main_view.fragments


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableMap
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.AllRmtItemBinding
import com.ms8.smartirhub.android.databinding.FRemoteAllBinding
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.main_view.MainViewActivity
import com.ms8.smartirhub.android.remote_control.models.RemoteProfile
import com.ms8.smartirhub.android.utils.MyValidators
import com.ms8.smartirhub.android.utils.NoSpecialCharacterAllowSpaceAndUnderscoreRule
import com.wajahatkarim3.easyvalidation.core.view_ktx.validator
import java.lang.ref.WeakReference

class MyRemotesFragment : MainFragment() {
    override fun newInstance(): MainFragment {
        return MyRemotesFragment()
    }

    private val remotesListener = object
        : ObservableMap.OnMapChangedCallback<ObservableMap<String, RemoteProfile>, String, RemoteProfile>() {
        override fun onMapChanged(sender: ObservableMap<String, RemoteProfile>?, key: String?) {
            addUserRemotesToAdapter()
        }

        fun addUserRemotesToAdapter() {
            adapter?.remoteList = ArrayList<RemoteProfile>().apply {
                addAll(AppState.userData.remotes.values)
            }
            checkPromptVisibility()
        }

        fun checkPromptVisibility() {
            binding?.apply {
                val visibility = if (adapter?.remoteList?.size ?: 0 > 0) View.GONE else View.VISIBLE
                txtCreateFirstRemoteP1.visibility = visibility
                txtCreateFirstRemoteP2.visibility = visibility
            }
        }
    }
    var binding: FRemoteAllBinding? = null
    var adapter : AllRemotesAdapter? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.f_remote_all, container, false)
        binding?.apply {
            rvAllRemotes.layoutManager = LinearLayoutManager(inflater.context, LinearLayoutManager.VERTICAL, false)
            adapter = AllRemotesAdapter()
            rvAllRemotes.adapter = adapter
        }
        remotesListener.addUserRemotesToAdapter()

        return binding?.root
    }

    override fun toString(): String {
        return "My Remotes Fragment"
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AppState.userData.remotes.addOnMapChangedCallback(remotesListener)
        remotesListener.addUserRemotesToAdapter()
    }

    override fun onDetach() {
        super.onDetach()
        AppState.userData.remotes.removeOnMapChangedCallback(remotesListener)
        adapter?.remoteList = ArrayList()
    }


    inner class AllRemotesAdapter : RecyclerView.Adapter<AllRemotesAdapter.RemoteViewHolder>() {
        var remoteList : ArrayList<RemoteProfile> = ArrayList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(RemoteDiffCallback(field, value))
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RemoteViewHolder {
            return RemoteViewHolder(AllRmtItemBinding.inflate(LayoutInflater.from(context),parent, false))
        }

        override fun getItemCount() = remoteList.size

        override fun onBindViewHolder(holder: RemoteViewHolder, position: Int) {
            holder.bind(remoteList[position])
        }

        inner class RemoteDiffCallback(private var oldRemotes : List<RemoteProfile>, private var newRemotes : List<RemoteProfile>) : DiffUtil.Callback() {

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldRemotes[oldItemPosition].uid == newRemotes[newItemPosition].uid

            override fun getOldListSize() = oldRemotes.size

            override fun getNewListSize() = newRemotes.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldRemotes[oldItemPosition] == newRemotes[newItemPosition]

        }

        inner class RemoteViewHolder(private val binding: AllRmtItemBinding) : RecyclerView.ViewHolder(binding.allRemotesLayout) {
            var remote : RemoteProfile? = null

            fun bind(newRemote: RemoteProfile) {
                remote = newRemote
                remote?.let { rmt ->
                    binding.apply {
                        tvRemoteName.text = rmt.name
                        tvOwner.text = rmt.ownerUsername
                        Log.d("TEST", "rmt.uid = ${rmt.uid}")
                        if (rmt.userHasPermission(RemoteProfile.PermissionType.READ_WRITE)) {
                            Log.d("TEST", "User has permission for ${rmt.name}")
                            ibEditRemoteName.isEnabled = true
                            ibEditRemoteName.visibility = View.VISIBLE
                            ibEditRemoteName.setOnClickListener { v ->
                                createEditNameDialog(WeakReference(v.context), remote?.name, remote?.uid)
                            }
                        } else {
                            Log.d("TEST", "User DOES NOT have permission for ${rmt.name} (${rmt.uid})")
                            ibEditRemoteName.isEnabled = false
                            ibEditRemoteName.visibility = View.GONE
                            ibEditRemoteName.setOnClickListener {  }
                        }
                        allRemotesLayout.setOnClickListener {
                            AppState.tempData.tempRemoteProfile.copyFrom(rmt)
                            if (activity is MainViewActivity)
                                (activity as MainViewActivity).switchInnerPage(0)
                        }
                    }
                }
            }

            private fun createEditNameDialog(context : WeakReference<Context>, remoteName : String?, remoteUid : String?) {
                val listener = DialogInterface.OnClickListener { dialogInterface, which ->
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        dialogInterface.dismiss()
                        if (AppState.tempData.tempRemoteName.isValidRemoteName()) {
                            remoteUid?.let { FirestoreActions.updateRemoteName(it) }
                        } else {
                            context.get()?.let {
                                AlertDialog.Builder(it)
                                    .setTitle(R.string.err_title)
                                    .setMessage(R.string.err_invalid_remote_name)
                                    .setPositiveButton(android.R.string.ok) { d, _ -> d.dismiss() }
                                    .setIcon(android.R.drawable.stat_notify_error)
                                    .show()
                            }
                        }
                    } else {
                        dialogInterface.dismiss()
                    }
                }

                val titleString = "${context.get()?.getString(R.string.edit)} $remoteName's ${context.get()?.getString(R.string.name)}"
                context.get()?.let { c ->
                    AlertDialog.Builder(c)
                        .setTitle(titleString)
                        .setView(EditText(c).apply {
                            addTextChangedListener(object : TextWatcher {
                                override fun afterTextChanged(p0: Editable?) {}

                                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int ) {
                                    AppState.tempData.tempRemoteName = p0.toString()
                                }
                            })
                        })
                        .setIcon(R.drawable.ic_mode_edit_white_24dp)
                        .setPositiveButton(R.string.change_name, listener)
                        .setNegativeButton(R.string.cancel, listener)
                        .show()
                }
            }
        }
    }
}

@SuppressLint("LogNotTimber")
fun RemoteProfile.userHasPermission(permission: RemoteProfile.PermissionType) : Boolean {
    val remotePermission = AppState.userData.remotePermissions[this.uid]

    if (remotePermission == null) {
        Log.e("Remote", "userHasPermission - couldn't find permission data for remote ${this.name} (${this.uid})")
        return false
    }

    return when (permission) {
        RemoteProfile.PermissionType.READ -> true
        RemoteProfile.PermissionType.READ_WRITE ->
        {
            remotePermission.permission == RemoteProfile.PermissionType.READ_WRITE
                    || remotePermission.permission == RemoteProfile.PermissionType.FULL_ACCESS
        }
        RemoteProfile.PermissionType.FULL_ACCESS ->
        {
            remotePermission.permission == RemoteProfile.PermissionType.FULL_ACCESS
        }
    }
}

fun String.isValidRemoteName() : Boolean {
    return this.validator()
            .nonEmpty()
            .addRule(NoSpecialCharacterAllowSpaceAndUnderscoreRule())
            .minLength(1)
            .maxLength(MyValidators.MAX_REMOTE_NAME_LENGTH)
            .check()

}
