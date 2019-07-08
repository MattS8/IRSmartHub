package com.ms8.smartirhub.android.learn_signal

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.custom_views.bottom_sheets.*
import com.ms8.smartirhub.android.database.LocalData
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.databinding.ACreateButtonWalkthroughBinding
import com.ms8.smartirhub.android.databinding.VChooseNameSheetBinding
import com.ms8.smartirhub.android.databinding.VSimpleListDescSheetBinding
import com.ms8.smartirhub.android.firebase.FirestoreActions
import com.ms8.smartirhub.android.setup_hub.HubSetupMainActivity
import com.ms8.smartirhub.android.utils.MyValidators.SignalNameValidator

class LSWalkThroughActivity : AppCompatActivity() {
    lateinit var binding : ACreateButtonWalkthroughBinding

    private val warningSheet: BackWarningSheet = BackWarningSheet()

    /* Pick Name Bottom Sheet */
    private val nameSheet: PickNameSheet = PickNameSheet()
    private val nameSheetCallback = object: PickNameSheet.Callback {
        override fun onSavePressed(sheetBinding: VChooseNameSheetBinding?) {
            sheetBinding?.txtInput?.error = ""
            val isValidName = sheetBinding?.txtInput?.editText!!.text.toString().SignalNameValidator()
                .addErrorCallback { sheetBinding.txtInput.error = getString(R.string.err_invalid_button_name) }
                .check()
            if (isValidName) {
                TempData.tempSignal?.name = sheetBinding.txtInput.editText!!.text.toString()
                sheetBinding.btnPickName.startAnimation()
                uploadIrSignal()
            }
        }

        override fun onDismiss() {
            determineWalkThroughState()
        }
    }

    /* Pick Listening Hub Bottom Sheet */
    private val pickHubSheet = HubSheet()

    /* Listen for Signal Bottom Sheet */
    private val listenSignalSheet = LearnSignalSheet()
    private val listenSignalSheetCallback = object : LearnSignalSheet.Callback {
        override fun onSaveSignal() { determineWalkThroughState() }
    }

    /* Sate Variables */
    private var listeningHub = ""
    private var clicked = false

/*
    ----------------------------------------------
        Overridden Functions
    ----------------------------------------------
*/

    override fun onBackPressed() {
        when {
            nameSheet.isVisible -> {
                nameSheet.dismiss()
            }
            pickHubSheet.isVisible -> {
                pickHubSheet.dismiss()
            }
            listenSignalSheet.onBackPressed() -> {}
            binding.prog1.bOnThisStep -> {
                when {
                    warningSheet.bWantsToLeave -> super.onBackPressed()
                    !warningSheet.bIsShowing -> warningSheet.show(supportFragmentManager, "WarningBottomSheet")
                }
            }
            binding.prog2.bOnThisStep -> {
                listeningHub = ""
                determineWalkThroughState()
            }
            binding.prog3.bOnThisStep -> {
                TempData.tempSignal = null
                determineWalkThroughState()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d("TEST", "Saving listeningHub = $listeningHub")
        outState.putString(LISTENING_HUB, listeningHub)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.a_create_button_walkthrough)

        //Restore state
        savedInstanceState?.getString(LISTENING_HUB)?.let { listeningHub = it }

        //Set up pickHubSheet
        pickHubSheet.sheetTitle = this@LSWalkThroughActivity.getString(R.string.hubs)
        pickHubSheet.helpTitle = this@LSWalkThroughActivity.getString(R.string.dont_see_hub_title)
        pickHubSheet.helpDesc = this@LSWalkThroughActivity.getString(R.string.dont_see_hub_desc)
        pickHubSheet.callback = object : SimpleListDescSheet.SimpleListDescSheetCallback {
            override fun getLayoutManager(): RecyclerView.LayoutManager {
                return GridLayoutManager(this@LSWalkThroughActivity, 1, RecyclerView.VERTICAL, false)
            }

            override fun getAdapter(): RecyclerView.Adapter<*> {
                return HubCardListAdapter().apply { callback = object : HubCardListAdapter.Callback {
                    override fun newHubClicked() {
                        startActivityForResult(Intent(this@LSWalkThroughActivity, HubSetupMainActivity::class.java),
                            HubSetupMainActivity.RC_HUB_SETUP_MAIN
                        )
                    }
                } }
            }

            override fun onSavePressed(simpleListDescSheet: SimpleListDescSheet, binding: VSimpleListDescSheetBinding) {
                val hubAdapter = (binding.sheetList.adapter as HubCardListAdapter)
                listeningHub = hubAdapter.list[hubAdapter.selectedItem].uid
                listenSignalSheet.hubUID = listeningHub
                Log.d("TEST", "Listening hub = $listeningHub")
                simpleListDescSheet.dismiss()
                determineWalkThroughState()
            }

            override fun onCancelPress(simpleListDescSheet: SimpleListDescSheet, binding: VSimpleListDescSheetBinding) {
                simpleListDescSheet.dismiss()
                determineWalkThroughState()
            }

            override fun onCreateView(binding: VSimpleListDescSheetBinding) {
                binding.sheetList.addItemDecoration(object : DividerItemDecoration(this@LSWalkThroughActivity, RecyclerView.VERTICAL){
                    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                        if (parent.getChildAdapterPosition(view) == parent.adapter!!.itemCount - 2) {
                            super.getItemOffsets(outRect, view, parent, state)
                        } else {
                            outRect.setEmpty()
                        }
                    }
                })
            }
        }
        pickHubSheet.hasSecondaryAction = false

        //Set up nameSheet
        nameSheet.callback = nameSheetCallback
        nameSheet.nameDesc = this@LSWalkThroughActivity.getString(R.string.need_help_name_ls_desc)
        nameSheet.tipsTitle = this@LSWalkThroughActivity.getString(R.string.tips_title)
        nameSheet.tipsDesc1 = this@LSWalkThroughActivity.getString(R.string.tips_learn_desc_1)
        nameSheet.tipsDesc2 = this@LSWalkThroughActivity.getString(R.string.tips_learn_desc_2)
        nameSheet.tipsExampleTitle = this@LSWalkThroughActivity.getString(R.string.for_example)
        nameSheet.nameInputHint = this@LSWalkThroughActivity.getString(R.string.signal_name_hint)


        //Set up listenSignalSheet
        listenSignalSheet.callback = listenSignalSheetCallback

        //Set up toolbar
        binding.toolbar.setTitle(R.string.learn_ir_title)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //Set progress texts
        binding.prog1.description = getString(R.string.select_ir_hub_desc)
        binding.prog2.description = getString(R.string.send_ir_signal_desc)
        binding.prog3.description = getString(R.string.name_learned_signal_desc)
    }

    override fun onResume() {
        super.onResume()
        clicked = false
        determineWalkThroughState()
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        when (requestCode) {
//            REQ_HUB -> {
//                if (resultCode == Activity.RESULT_OK) {
//                    listeningHub = data?.getStringExtra(LISTENING_HUB) ?: ""
//                }
//            }
//            REQ_NAME -> {
//                if (resultCode == Activity.RESULT_OK) {
//                    setResult(Activity.RESULT_OK, Intent().putExtra(NEW_IR_SIGNAL_UID, data?.getStringExtra(NEW_IR_SIGNAL_UID)))
//                    finish()
//                }
//            }
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            Log.d("LSWalkthroughActivity", "Fishing up!")
            TempData.tempSignal = null
        }
    }

/*
    ----------------------------------------------
        Layout Functions
    ----------------------------------------------
*/

    private fun determineWalkThroughState() {
        // Figure out which step we're on
        when {
            listeningHub == "" -> {
                Log.d("###TEST", "Starting on prog 1")
                binding.prog1.bOnThisStep = true
                binding.prog2.bOnThisStep = false
                binding.prog3.bOnThisStep = false
                binding.btnNextStep.text = getString(R.string.select_ir_hub)
                binding.btnNextStep.setOnClickListener { showPickHubSheet() }
                binding.prog1.setOnClickListener { showPickHubSheet() }
            }
            TempData.tempSignal == null || TempData.tempSignal?.rawData?.size == 0 -> {
                Log.d("###TEST", "Starting on prog 2")
                binding.prog1.bOnThisStep = false
                binding.prog2.bOnThisStep = true
                binding.prog3.bOnThisStep = false
                binding.btnNextStep.text = getString(R.string.send_ir_signal)
                binding.btnNextStep.setOnClickListener { showLearnSignalSheet() }
                binding.prog2.setOnClickListener { showLearnSignalSheet() }
            }
            else -> {
                Log.d("###TEST", "Starting on prog 3")
                binding.prog1.bOnThisStep = false
                binding.prog2.bOnThisStep = false
                binding.prog3.bOnThisStep = true
                binding.btnNextStep.setOnClickListener { showNameSheet() }
                binding.prog3.setOnClickListener { showNameSheet() }
            }
        }
    }

/*
    ----------------------------------------------
        OnClick Functions
    ----------------------------------------------
*/

    private fun showLearnSignalSheet() {
        if (!listenSignalSheet.isVisible)
            listenSignalSheet.show(supportFragmentManager, "ListeningSignalSheet")
    }

    private fun showPickHubSheet() {
        if (!pickHubSheet.isVisible)
            pickHubSheet.show(supportFragmentManager, "ListeningHubSheet")
    }

    private fun showNameSheet() {
        if (!nameSheet.isVisible)
            nameSheet.show(supportFragmentManager, "SignalNameSheet")
    }

    @SuppressLint("LogNotTimber")
    private fun uploadIrSignal() {
        FirestoreActions.addIrSignal()
            .addOnCompleteListener {
                nameSheet.binding?.btnPickName?.revertAnimation()
            }
            .addOnFailureListener {
                Log.e("LSNameSignalActivity", "AddIrSignal listener error: $it")
                nameSheet.binding?.txtInput?.error = getString(R.string.err_unknown_desc)
            }
            .addOnSuccessListener {
                Log.d("TEST", "New uid: ${it.id}")
                setResult(Activity.RESULT_OK, Intent().putExtra(NEW_IR_SIGNAL_UID, it.id))
                //LocalData.signals[it.id] = TempData.tempSignal
                TempData.tempSignal = null
                finish()
            }
    }

    private fun getSignalNameActivity() {
        if (!clicked) {
            startActivityForResult(Intent(this, LSNameSignalActivity::class.java), REQ_NAME)
            clicked = true
        }
    }

    private fun getSignalActivity() {
        if (!clicked) {
            startActivityForResult(Intent(this, LSListenActivity::class.java).putExtra(LISTENING_HUB, listeningHub), REQ_SIG)
            clicked = true
        }
    }

    private fun getHubActivity() {
        if (!clicked){
//            val center = binding.prog1.progressBar.getCenter()
//            val options = ActivityOptionsCompat.makeClipRevealAnimation(binding.prog1, center.first.toInt(), center.second.toInt(), 0, 0)
            val intent = Intent(this, LSSelectHubActivity::class.java)
//            ViewAnimationUtils.createCircularReveal(binding.root, center.first.toInt(), center.second.toInt(), 0f, 100f).apply {
//                duration = 1000
//                interpolator = DecelerateInterpolator()
//            }.start()

            ActivityCompat.startActivityForResult(this, intent, REQ_HUB, null)
            clicked = true
        }
    }

/*
    ----------------------------------------------
        Static Stuff
    ----------------------------------------------
*/

    companion object {
        const val REQ_NEW_IR_SIGNAL = 9
        const val REQ_HUB = 2
        const val REQ_SIG = 3
        const val REQ_NAME = 4
        const val LISTENING_HUB = "ACT_LISTENEING_HUB"
        const val NEW_IR_SIGNAL_UID = "NEW_SIGNAL_UID"
        const val EXTRA_REVEAL = "EXTRA_REVEAL"
    }
}


