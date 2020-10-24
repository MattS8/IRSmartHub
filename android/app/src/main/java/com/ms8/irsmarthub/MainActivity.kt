package com.ms8.irsmarthub

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.Observable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.ms8.irsmarthub.database.AppState
import com.ms8.irsmarthub.databinding.DialogPairingBinding
import com.ms8.irsmarthub.databinding.DrawerOptionsBinding
import com.ms8.irsmarthub.firebase.HubException
import com.ms8.irsmarthub.firebase.RealtimeDatabaseFunctions
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    /** State Variables **/
    private val IN_EDIT_MODE_KEY = "InEditMode"
    private var inEditMode = false
    private val DRAWER_OPEN_KEY = "IsDrawerOpen"
    private var isDrawerOpen = false
    private val PAIR_DIALOG_KEY = "IsShowingPairDialog"
    private var isShowingPairDialog = false
    private val PAIR_COMMAND_KEY = "CommandBeingPaied"
    private var commandBeingPaired : RealtimeDatabaseFunctions.Command? = null

    /** Drawer Variables **/
    private lateinit var drawer : Drawer
    private lateinit var optionsBinding : DrawerOptionsBinding

    /** Pairing Dialog **/
    private var pairingDialog : AlertDialog? = null
    var pairingBinding: DialogPairingBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup state
        inEditMode = savedInstanceState?.getBoolean(IN_EDIT_MODE_KEY, false)
            ?: inEditMode
        isDrawerOpen = savedInstanceState?.getBoolean(DRAWER_OPEN_KEY, false)
            ?: isDrawerOpen
        isShowingPairDialog = savedInstanceState?.getBoolean(PAIR_DIALOG_KEY, false)
            ?: isShowingPairDialog
        savedInstanceState?.getString(PAIR_COMMAND_KEY)?.let {
            commandBeingPaired = RealtimeDatabaseFunctions.Command.valueOf(it)
        }

        // Setup sliding drawer
        optionsBinding = DrawerOptionsBinding.inflate(layoutInflater)
        optionsBinding.btnSetupButtons.setOnClickListener { v -> handleButtonClick(v) }
        drawer = DrawerBuilder()
            .withActivity(this)
            .withDrawerGravity(Gravity.END)
            .withFullscreen(false)
            .withCloseOnClick(false)
            .withTranslucentStatusBar(false)
            .withCustomView(optionsBinding.optionsRoot)
            .build()
        if (isDrawerOpen)
            drawer.openDrawer()
        else
            drawer.closeDrawer()

        // Setup done button
        if (inEditMode) btnSetupButtonsDone.show() else btnSetupButtonsDone.hide()

        // Setup edit mode
        tvEditMode.visibility = if (inEditMode) View.VISIBLE else View.GONE

        // Setup pairing dialog
//        pairingBinding.btnPairEnd.setOnClickListener { v -> handleButtonClick(v) }
//        pairingDialog = PairDialog(
//                {d -> onDialogDismiss(d)},
//                {onShowDialog()},
//                supportFragmentManager,
//                pairingBinding)

    }

    override fun onResume() {
        super.onResume()

        if (isShowingPairDialog) {
            showPairDialog(true)
            when {
                AppState.errorData.pairSignalError.get() != null ->
                    AppState.errorData.pairSignalError.get()?.let { showPairingError(it) }

                AppState.pairedSignal.get()?.containsAllRawData() == true ->
                    showPairingSuccess()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // Clear Pairing Dialog values to avoid mem leak
        pairingDialog?.setOnDismissListener {  }
        pairingDialog?.dismiss()
        pairingBinding = null
        pairingDialog = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(IN_EDIT_MODE_KEY, inEditMode)
        outState.putBoolean(DRAWER_OPEN_KEY, isDrawerOpen)
        outState.putBoolean(PAIR_DIALOG_KEY, isShowingPairDialog)
        commandBeingPaired?.let { outState.putString(PAIR_COMMAND_KEY, it.name) }
        super.onSaveInstanceState(outState)
    }

    private fun setEditMode(newInEditMode: Boolean) {
        inEditMode = newInEditMode

        if (inEditMode) {
            drawer.closeDrawer()
            btnSetupButtonsDone.show()
            tvEditMode.visibility = View.VISIBLE
        } else {
            btnSetupButtonsDone.hide()
            tvEditMode.visibility = View.GONE
        }
    }

    fun handleButtonClick(v: View) {
        Log.d("MainActivity", "Handle button click for id ${v.id}")
        when (v.id) {
            R.id.btnSetupButtonsDone -> setEditMode(false)
            R.id.btnSetupButtons -> setEditMode(true)
            R.id.btnHiddenOptions -> drawer.openDrawer()
            R.id.btnPairEnd -> {
                AppState.pairedSignal.get()?.let { newSignal ->
                    if (newSignal.containsAllRawData()) {
                        commandBeingPaired?.let { command ->
                            RealtimeDatabaseFunctions.saveCommand(command, newSignal)
                        }
                    }
                }
                pairingDialog?.dismiss()
            }
            else -> if (inEditMode) {
                commandBeingPaired = getCommandForButton(v.id)
                showPairDialog(true)
                RealtimeDatabaseFunctions.listenForCommand()
            } else
                RealtimeDatabaseFunctions.sendCommand(getCommandForButton(v.id))
        }
    }

    /** Pair Dialog **/

    private fun showPairDialog(initialSetup: Boolean) {
        pairingBinding = DialogPairingBinding.inflate(layoutInflater)
        pairingBinding?.btnPairEnd?.setOnClickListener { v -> handleButtonClick(v) }
        pairingDialog = MaterialAlertDialogBuilder(this)
                .setView(pairingBinding?.pairingRoot)
                .setOnDismissListener { onDialogDismiss() }
                .create()

        isShowingPairDialog = true
        AppState.pairedSignal.addOnPropertyChangedCallback(signalPairedListener)
        AppState.errorData.pairSignalError.addOnPropertyChangedCallback(pairSignalErrorListener)

        pairingDialog?.show()

        if (initialSetup) {
            pairingBinding?.let { binding ->
                binding.tvPairTitle.text = getText(R.string.capture_command)
                binding.tvPairTitle.setTextColor(
                        ContextCompat.getColor(binding.tvPairTitle.context, R.color.material_drawer_dark_secondary_text)
                )
                binding.tvPairDescription.text = getText(R.string.pair_desc)
                binding.tvPairDescription.setTextColor(
                        ContextCompat.getColor(binding.tvPairDescription.context, R.color.material_drawer_dark_secondary_text)
                )

                binding.progPair.visibility = View.VISIBLE

                binding.btnPairEnd.text = getText(android.R.string.cancel)
                binding.btnPairEnd.setTextColor(
                        ContextCompat.getColor(binding.btnPairEnd.context, android.R.color.holo_red_dark)
                )
            }
        }
    }

    private fun showPairingError(hubException: HubException) {
        if (pairingDialog == null)
            showPairDialog(false)

        pairingBinding?.let { binding ->
            binding.tvPairDescription.text = getText(hubException.messageID)
            binding.tvPairDescription.setTextColor(
                    ContextCompat.getColor(binding.tvPairDescription.context, android.R.color.holo_red_dark)
            )

            binding.tvPairTitle.text = getText(hubException.titleID)
            binding.tvPairTitle.setTextColor(
                    ContextCompat.getColor(binding.tvPairTitle.context, android.R.color.holo_red_dark)
            )

            binding.progPair.visibility = View.GONE

            binding.btnPairEnd.text = getText(R.string.dismiss)
            binding.btnPairEnd.setTextColor(
                    ContextCompat.getColor(binding.btnPairEnd.context, android.R.color.holo_red_dark)
            )
        }
    }

    fun showPairingSuccess() {
        if (pairingDialog == null)
            showPairDialog(false)

        pairingBinding?.let { binding ->
            AppState.pairedSignal.get()?.let { signal ->
                val successText = "Received IR signal: ${signal.code}"
                binding.tvPairDescription.text = successText
                binding.tvPairDescription.setTextColor(
                        ContextCompat.getColor(binding.tvPairDescription.context, android.R.color.holo_green_light)
                )
            }

            binding.tvPairTitle.text = getText(R.string.signal_received_title)
            binding.tvPairTitle.setTextColor(
                    ContextCompat.getColor(binding.tvPairTitle.context, android.R.color.holo_green_light)
            )

            binding.progPair.visibility = View.GONE

            binding.btnPairEnd.text = getText(R.string.save)
            binding.btnPairEnd.setTextColor(
                    ContextCompat.getColor(binding.btnPairEnd.context, android.R.color.holo_green_light)
            )
        }
    }

    private fun onDialogDismiss() {
        isShowingPairDialog = false
        AppState.pairedSignal.removeOnPropertyChangedCallback(signalPairedListener)
        AppState.errorData.pairSignalError.removeOnPropertyChangedCallback(pairSignalErrorListener)
        pairingDialog = null
        pairingBinding = null
    }

    /** Listeners **/

    private val signalPairedListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            if (AppState.pairedSignal.get()?.containsAllRawData() == true) {
                showPairingSuccess()
            }
        }
    }

    private val pairSignalErrorListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            AppState.errorData.pairSignalError.get()?.let {
                showPairingError(it)
            }
        }
    }

    private fun getCommandForButton(buttonId: Int) : RealtimeDatabaseFunctions.Command {
        return when (buttonId) {
            R.id.btnTVOnly -> RealtimeDatabaseFunctions.Command.TOGGLE_TV
            R.id.btnMute -> RealtimeDatabaseFunctions.Command.TOGGLE_MUTE
            R.id.btnTurnOn -> RealtimeDatabaseFunctions.Command.TURN_ON
            R.id.btnTurnOff -> RealtimeDatabaseFunctions.Command.TURN_OFF
            R.id.btnVolUp -> RealtimeDatabaseFunctions.Command.VOL_UP
            R.id.btnVolDown -> RealtimeDatabaseFunctions.Command.VOL_DOWN
            else -> throw CommandNotFoundException(buttonId)
        }
    }

    class CommandNotFoundException(buttonId: Int) : Throwable("Unknown button id passed! ($buttonId)")
}