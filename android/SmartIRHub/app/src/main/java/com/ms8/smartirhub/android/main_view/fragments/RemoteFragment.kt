package com.ms8.smartirhub.android.main_view.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.databinding.FRemoteCurrentBinding

class RemoteFragment : MainViewFragment() {
    override fun newInstance(): MainViewFragment {
        return RemoteFragment()
    }

    lateinit var binding: FRemoteCurrentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            binding = DataBindingUtil.inflate(inflater, R.layout.f_remote_current, container, false)
            return binding.root
        }

    override fun toString(): String {
        return "Remote Fragment"
    }
}

