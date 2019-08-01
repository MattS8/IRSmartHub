package com.ms8.smartirhub.android.create_command

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableArrayMap
import androidx.databinding.ObservableMap
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.models.firestore.IrSignal
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.databinding.ACcChooseIrSignalBinding
import com.ms8.smartirhub.android.learn_signal.LSWalkThroughActivity
import com.ms8.smartirhub.android.learn_signal.LSWalkThroughActivity.Companion.NEW_IR_SIGNAL_UID
import com.ms8.smartirhub.android.learn_signal.LSWalkThroughActivity.Companion.REQ_NEW_IR_SIGNAL

class CC_ChooseIrSignalActivity : AppCompatActivity() {
    lateinit var binding: ACcChooseIrSignalBinding

/*
    ----------------------------------------------
        Listeners
    ----------------------------------------------
*/

    private val irSignalListner = object : ObservableMap.OnMapChangedCallback<ObservableArrayMap<String, IrSignal>, String, IrSignal>() {
        override fun onMapChanged(sender: ObservableArrayMap<String, IrSignal>?, key: String?) {
            adapter.signalList = ArrayList(sender?.values ?: listOf())
            adapter.notifyDataSetChanged()
        }
    }

    private val signalCallback = object : IrSignalAdapter.Callback {
        override fun signalSelected(irSignal: IrSignal?) = onSignalSelected(irSignal)
    }
    private val adapter = IrSignalAdapter().apply { callback = signalCallback }

/*
    ----------------------------------------------
        Overridden Functions
    ----------------------------------------------
*/

    override fun onResume() {
        super.onResume()
        AppState.userData.irSignals.addOnMapChangedCallback(irSignalListner)
    }

    override fun onPause() {
        super.onPause()
        AppState.userData.irSignals.removeOnMapChangedCallback(irSignalListner)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.a_cc_choose_ir_signal)
        binding.irList.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.irList.adapter = adapter

        binding.btnNewIrSignal.setOnClickListener { onNewIrSignalSelected() }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_NEW_IR_SIGNAL) {
            if (resultCode == Activity.RESULT_OK) {
                val newIrUID = data?.getStringExtra(NEW_IR_SIGNAL_UID) ?: ""
                if (newIrUID != "") {
                    setResult(Activity.RESULT_OK, Intent().putExtra(NEW_IR_SIGNAL_UID, newIrUID))
                    finish()
                } else {
                    Log.d("TEST", "now new signal gotten")
                }
            } else {
                Log.d("ChooseIrSignal", "Result was not ok: $resultCode")
            }
        }
    }

/*
    ----------------------------------------------
        OnClick Functions
    ----------------------------------------------
*/

    private fun onNewIrSignalSelected() {
        startActivityForResult(Intent(this, LSWalkThroughActivity::class.java), REQ_NEW_IR_SIGNAL)
    }

    private fun onSignalSelected(irSignal : IrSignal?) {
        irSignal?.let {
            setResult(Activity.RESULT_OK, Intent().putExtra(NEW_IR_SIGNAL_UID, it.uid))
            finish()
        }
    }

    companion object {
        const val REQ_NEW_ACTION = 3
        const val REQ_EDIT_ACTION = 4
    }
}
