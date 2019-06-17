package com.ms8.smartirhub.android.learn_signal

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.databinding.*
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.custom_views.BottomErrorSheet
import com.ms8.smartirhub.android.data.Hub
import com.ms8.smartirhub.android.database.LocalData
import com.ms8.smartirhub.android.databinding.ALearnSigGetHubBinding
import com.ms8.smartirhub.android.learn_signal.LSWalkthroughActivity.Companion.LISTENING_HUB

class LSSelectHubActivity : AppCompatActivity() {
    lateinit var binding: ALearnSigGetHubBinding
    private val hubListeners = HubNamesListener()
    private val hubList = ArrayList<Hub>()
    private val errorSheet = BottomErrorSheet()
    val nameList = ArrayList<String>()


    override fun onResume() {
        super.onResume()
        LocalData.hubs.addOnMapChangedCallback(hubListeners)
    }

    override fun onPause() {
        super.onPause()
        LocalData.hubs.removeOnMapChangedCallback(hubListeners)
    }

    override fun onNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        errorSheet.sheetTitle = getString(R.string.err_no_hub_selected_title)
        errorSheet.description = getString(R.string.err_no_hub_selected_desc)

        binding = DataBindingUtil.setContentView(this, R.layout.a_learn_sig_get_hub)

//        binding.toolbar.title = getString(R.string.select_listening_hub_title)
//        setSupportActionBar(binding.toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setDisplayShowHomeEnabled(true)

        getNameListItems()
        binding.drpdwnSelectedHub.adapter = ArrayAdapter<String>(this,
            R.layout.support_simple_spinner_dropdown_item, nameList)

        binding.btnSelectHub.setOnClickListener { selectHub() }
    }

    private fun getNameListItems() {
        nameList.clear()
        hubList.clear()
        hubList.addAll(LocalData.hubs.values)
        hubList.sortBy { it.name }
        hubList.forEach { nameList.add(it.name) }
        nameList.add(getString(R.string.setup_new_hub_drp_dwn))
        binding.drpdwnSelectedHub.adapter = ArrayAdapter<String>(this@LSSelectHubActivity,
            R.layout.support_simple_spinner_dropdown_item, nameList)

        Log.d("###TEST", "nameList: ${arrayListOf(nameList)}")
    }

    private fun selectHub() {
        when (binding.drpdwnSelectedHub.selectedItemPosition) {
        // No hub selected
            -1 -> {
                if (!errorSheet.bIsShowing)
                    errorSheet.show(supportFragmentManager, "Bottom_error_sheet")
            }
        // Set up new hub selected
            nameList.size-1 -> {
                binding.drpdwnSelectedHub.setSelection(0)
                //TODO Start "Set Up Hub" process
            }
        // Existing hub selected
            else -> {
                val resultIntent = Intent().apply {
                    val hubUID = hubList[binding.drpdwnSelectedHub.selectedItemPosition].uid
                    putExtra(LISTENING_HUB, hubUID)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }


    private inner class HubNamesListener : ObservableMap.OnMapChangedCallback<ObservableArrayMap<String, Hub>, String, Hub>() {
        override fun onMapChanged(sender: ObservableArrayMap<String, Hub>?, key: String?) {
            getNameListItems()
        }
    }
}
