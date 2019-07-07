package com.ms8.smartirhub.android.learn_signal

import android.animation.AnimatorSet
import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.view.isInvisible
import androidx.databinding.*
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.LocalData
import com.ms8.smartirhub.android.databinding.ALearnSigGetHubBinding
import com.ms8.smartirhub.android.utils.exts.*
import com.ms8.smartirhub.android.learn_signal.LSWalkThroughActivity.Companion.LISTENING_HUB
import java.lang.ref.WeakReference
import kotlin.math.hypot

class LSSelectHubActivity : AppCompatActivity() {
    lateinit var binding: ALearnSigGetHubBinding
    private val hubCardListAdapter = HubCardListAdapter()


    override fun onResume() {
        super.onResume()
        hubCardListAdapter.listen(true)
    }

    override fun onPause() {
        super.onPause()
        hubCardListAdapter.listen(false)
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

        binding = DataBindingUtil.setContentView(this, R.layout.a_learn_sig_get_hub)
        performCircularReveal()

        //binding.toolbar.title = getString(R.string.select_listening_hub_title)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.buttonLayout.clipToOutline = false

        binding.rvHubList.layoutManager = GridLayoutManager(this, 1, RecyclerView.VERTICAL, false)
        binding.rvHubList.adapter = hubCardListAdapter
        binding.hubList = LocalData.hubs
        binding.rvHubList.addItemDecoration(object : DividerItemDecoration(this, RecyclerView.VERTICAL){
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                if (parent.getChildAdapterPosition(view) == parent.adapter!!.itemCount - 2) {
                    super.getItemOffsets(outRect, view, parent, state)
                } else {
                    outRect.setEmpty()
                }
            }
        })

        binding.btnSelectHub.setOnClickListener { selectHub() }
    }

    private fun selectHub() {
        val resultIntent = Intent().apply {
            val hubUID = hubCardListAdapter.list[hubCardListAdapter.selectedItem].uid
            putExtra(LISTENING_HUB, hubUID)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
