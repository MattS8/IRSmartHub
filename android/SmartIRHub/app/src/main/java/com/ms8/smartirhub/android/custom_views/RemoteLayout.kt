package com.ms8.smartirhub.android.custom_views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.Observable
import androidx.databinding.ObservableList
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.models.firestore.RemoteProfile.Button
import com.ms8.smartirhub.android.models.firestore.RemoteProfile.Button.Companion.STYLE_CREATE_BUTTON
import com.ms8.smartirhub.android.models.firestore.RemoteProfile.Button.Companion.STYLE_BTN_SINGLE_ACTION
import com.ms8.smartirhub.android.models.firestore.RemoteProfile.Button.Companion.STYLE_SPACE
import com.ms8.smartirhub.android.models.firestore.RemoteProfile.Button.Companion.STYLE_BTN_NO_MARGIN
import com.ms8.smartirhub.android.database.TempData
import com.ms8.smartirhub.android.firebase.RealtimeDatabaseFunctions
import com.ms8.smartirhub.android.models.firestore.RemoteProfile.Button.Companion.STYLE_BTN_INCREMENTER_VERTICAL

class RemoteLayout(context: Context, attrs : AttributeSet) : RecyclerView(context, attrs) {
    private var isListening = true
    var buttonCallback: RemoteLayoutButtonCallback? = null
    set(value) {
        field = value
        (adapter as RemoteAdapter).buttonCallback = field
    }

    private val buttonListener = object : ObservableList.OnListChangedCallback<ObservableList<Button>>() {
        override fun onChanged(sender: ObservableList<Button>?) {
            adapter?.notifyDataSetChanged()
        }

        override fun onItemRangeRemoved(sender: ObservableList<Button>?, positionStart: Int, itemCount: Int) {
            adapter?.notifyItemRangeRemoved(positionStart, itemCount)
        }

        override fun onItemRangeMoved(sender: ObservableList<Button>?, fromPosition: Int, toPosition: Int, itemCount: Int) {
            adapter?.notifyDataSetChanged()
        }

        override fun onItemRangeInserted(sender: ObservableList<Button>?, positionStart: Int, itemCount: Int) {
            adapter?.notifyItemRangeInserted(positionStart, itemCount)
        }

        override fun onItemRangeChanged(sender: ObservableList<Button>?, positionStart: Int, itemCount: Int) {
            adapter?.notifyItemRangeChanged(positionStart, itemCount)
        }
    }

    private val remoteListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            adapter?.notifyDataSetChanged()
        }
    }

    init {
        layoutManager = StaggeredGridLayoutManager(BUTTON_SPAN, StaggeredGridLayoutManager.VERTICAL)
        adapter = RemoteAdapter(buttonCallback)
        TempData.tempRemoteProfile.buttons.addOnListChangedCallback(buttonListener)
        clipToPadding = false
        clipChildren = false
    }

    fun isListening() = isListening

    fun listen() {
        if (!isListening) {
            TempData.tempRemoteProfile.inEditMode.addOnPropertyChangedCallback(remoteListener)
            TempData.tempRemoteProfile.buttons.addOnListChangedCallback(buttonListener)
        }
    }

    fun stopListening() {
        TempData.tempRemoteProfile.inEditMode.removeOnPropertyChangedCallback(remoteListener)
        TempData.tempRemoteProfile.buttons.removeOnListChangedCallback(buttonListener)
    }

    fun updateAdapter() {
        adapter?.notifyDataSetChanged()
    }

    class RemoteAdapter(var buttonCallback: RemoteLayoutButtonCallback?) : RecyclerView.Adapter<ButtonViewHolder>() {
        @SuppressLint("LogNotTimber")
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
           val v = when (viewType) {
                STYLE_CREATE_BUTTON -> {
                    LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_create_new, parent, false)
                }
                STYLE_SPACE -> {
                    Log.d("RemoteAdapter", "Inflating square button")
                    (LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_base, parent, false))
                }
                STYLE_BTN_SINGLE_ACTION -> {
                    (LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_base, parent, false))
                }
                STYLE_BTN_NO_MARGIN -> {
                    (LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_base, parent, false))
                }
                STYLE_BTN_INCREMENTER_VERTICAL -> {
                    (LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_base, parent, false))
                }
                else -> {
                    Log.w("RemoteLayout", "unknown viewType ($viewType)")
                    LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_base, parent, false)
                }
            }

            return ButtonViewHolder(v)
        }

        override fun getItemCount() = TempData.tempRemoteProfile.buttons.size + (if (isInEditMode()) 1 else 0)

        override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
            when {
                position == itemCount - 1 && isInEditMode() -> {
                    holder.button = null
                    holder.itemView.setOnClickListener { buttonCallback?.createNewButton() }
                }
                else -> {
                    holder.bind(TempData.tempRemoteProfile.buttons[position])
                }
            }
        }

        override fun getItemViewType(position: Int) =
            when {
                position == itemCount - 1 && isInEditMode() -> STYLE_CREATE_BUTTON
                else -> TempData.tempRemoteProfile.buttons[position].style
            }

        private fun isInEditMode() : Boolean {
            return TempData.tempRemoteProfile.inEditMode.get()
        }
    }

    class ButtonViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        var button : Button? = null

        fun bind(button: Button) {
            this.button = button
            //itemView.findViewById<TextView>(R.id.btnText).text = button.name
            itemView.setOnClickListener { RealtimeDatabaseFunctions.sendCommandToHub(button.command) }
        }
    }

    interface RemoteLayoutButtonCallback {
        fun createNewButton()
    }

    companion object {
        const val BUTTON_SPAN = 3

    }
}