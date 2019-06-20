package com.ms8.smartirhub.android.learn_signal

import android.animation.AnimatorSet
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.isInvisible
import androidx.databinding.*
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.custom_views.BottomErrorSheet
import com.ms8.smartirhub.android.data.Hub
import com.ms8.smartirhub.android.database.LocalData
import com.ms8.smartirhub.android.databinding.ALearnSigGetHubBinding
import com.ms8.smartirhub.android.exts.*
import com.ms8.smartirhub.android.learn_signal.LSWalkthroughActivity.Companion.LISTENING_HUB
import kotlin.math.hypot

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

    private fun performCircularReveal() {
        if (!hasSourceBounds) {
            binding.root.isInvisible = false
        } else {
            sourceBounds { sourceBounds ->
                binding.root.run {
                    screenBounds { rootLayoutBounds ->
                        // Verify if sourceBounds is valid
                        if (rootLayoutBounds.contains(sourceBounds)) {
                            val circle = createCircularReveal(
                                centerX = sourceBounds.centerX() - rootLayoutBounds.left,
                                centerY = sourceBounds.centerY() - rootLayoutBounds.top,
                                startRadius = (minOf(sourceBounds.width(), sourceBounds.height()) * 0.2).toFloat(),
                                endRadius = hypot(binding.root.width.toFloat(), binding.root.height.toFloat())
                            ).apply {
                                isInvisible = false
                                duration = 500L
                            }
                            AnimatorSet()
                                .apply { playTogether(circle) }
                                .start()
                        } else {
                            isInvisible = false
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        preAnimationSetup()
        super.onCreate(savedInstanceState)
        errorSheet.sheetTitle = getString(R.string.err_no_hub_selected_title)
        errorSheet.description = getString(R.string.err_no_hub_selected_desc)
        binding = DataBindingUtil.setContentView(this, R.layout.a_learn_sig_get_hub)
        performCircularReveal()


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
