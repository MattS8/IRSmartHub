package com.ms8.smartirhub.android._tests.dev_playground.remote_layout

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import androidx.recyclerview.widget.RecyclerView
import com.ms8.smartirhub.android.R
import com.ms8.smartirhub.android.database.AppState
import com.ms8.smartirhub.android.remote_control.button.models.Button
import com.ms8.smartirhub.android.remote_control.views.asymmetric_gridview.AsymmetricGridView
import com.ms8.smartirhub.android.remote_control.views.asymmetric_gridview.AsymmetricGridViewAdapter
import com.ms8.smartirhub.android.remote_control.views.asymmetric_gridview.AsymmetricItem

class RemoteLayout(context: Context, attrs: AttributeSet) : AsymmetricGridView(context, attrs) {

    //TODO: Test Code
    fun setupAdapter() {
        val rla = RemoteListAdapter(context)
            .apply {
                AppState.tempData.tempRemoteProfile.buttons
                    .apply {
                        for (i in 0 until 35) {
                            add(
                                Button()
                                .apply {
                                    name = "Button $i"
                                })
                            when (i) {
                                0,1,2,3,5,6,7,8 -> {
                                    add(AsymmetricButtonItem(1, 1, i))
                                }
                                4,9,10,12 -> {
                                    add(AsymmetricButtonItem(1, 2, i))
                                }
                                11 -> {
                                    add(AsymmetricButtonItem(2, 2, i))
                                }
                                else -> {
                                    val colspan = if (i % 2 == 0) 1 else 2
                                    val rowSpan = if (i % 10 == 0) 1 else 2
                                    add(AsymmetricButtonItem(1, 1, i))
                                }
                            }
                        }
                    }
            }

        adapter = AsymmetricGridViewAdapter(
            context,
            this,
            rla
        )
    }

    class AsymmetricButtonItem(var _columnSpan: Int, var _rowSpan: Int, var _position: Int):
        AsymmetricItem {
        override var columnSpan: Int
            get() = _columnSpan
            set(value) { _columnSpan = columnSpan }
        override var rowSpan: Int
            get() = _rowSpan
            set(value) { _rowSpan = rowSpan }
        var button: Button? = null

        constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt()
        )

        override fun describeContents() = 0

        override fun writeToParcel(dest: Parcel?, flags: Int) {
            dest?.apply {
                    writeInt(_columnSpan)
                    writeInt(_rowSpan)
                    writeInt(_position)
                }
        }

        override fun toString(): String {
            return String.format("%s: %sx%s", _position, _rowSpan, _columnSpan)
        }

        companion object CREATOR : Parcelable.Creator<AsymmetricButtonItem> {
            override fun createFromParcel(parcel: Parcel): AsymmetricButtonItem {
                return AsymmetricButtonItem(parcel)
            }

            override fun newArray(size: Int): Array<AsymmetricButtonItem?> {
                return arrayOfNulls(size)
            }
        }
    }

    class RemoteListAdapter(context: Context): ArrayAdapter<AsymmetricButtonItem>(context, 0) {
        private val layoutInflater = LayoutInflater.from(context)

        var remoteProperties: RemoteLayoutProperties? = null

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val button = AppState.tempData.tempRemoteProfile.buttons[position]

            val v = when (convertView) {
                null -> {
                    layoutInflater.inflate(R.layout.v_rmt_btn_base, parent, false)
                }
                else -> {
                    convertView
                }
            }
            //v.findViewById<TextView>(R.id.btnText).text = button.name

            when (position) {
                10,12 -> {
                    v.visibility = View.INVISIBLE
                }
                else -> {
                    v.visibility = View.VISIBLE
                }
            }

            //TODO: Test code
//            when (position) {
//                1,3,5 -> {
//                    remoteProperties?.getRemoteWidth()?.let {
//                        val layoutParams = v.layoutParams
//                        layoutParams.width = it / 2
//                        layoutParams.height = it / 4
//                        v.layoutParams = layoutParams
//                        v.invalidate()
//                    }
//                }
//            }

            return v
        }

        interface RemoteLayoutProperties {
            fun getRemoteWidth() : Int
        }
    }

    class RemoteRecyclerAdapter: RecyclerView.Adapter<RemoteRecyclerAdapter.RemoteViewHolder>() {
        private val listListener = object :
            ObservableList.OnListChangedCallback<ObservableArrayList<Button>>() {
            override fun onChanged(sender: ObservableArrayList<Button>?) = notifyDataSetChanged()
            override fun onItemRangeRemoved(s: ObservableArrayList<Button>?, positionStart: Int, itemCount: Int) = notifyItemRangeRemoved(positionStart, itemCount)
            override fun onItemRangeMoved(sender: ObservableArrayList<Button>?, fromPosition: Int, toPosition: Int, itemCount: Int) = notifyDataSetChanged()
            override fun onItemRangeInserted(sender: ObservableArrayList<Button>?, positionStart: Int, itemCount: Int) = notifyItemRangeInserted(positionStart, itemCount)
            override fun onItemRangeChanged(sender: ObservableArrayList<Button>?, positionStart: Int, itemCount: Int) = notifyItemRangeChanged(positionStart, itemCount)
        }

        var remoteProperties: RemoteLayoutProperties? = null

        init {
            startListening()
        }

        fun stopListening() {
            AppState.tempData.tempRemoteProfile.buttons.removeOnListChangedCallback(listListener)
        }

        fun startListening() {
            AppState.tempData.tempRemoteProfile.buttons.addOnListChangedCallback(listListener)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RemoteViewHolder {
            return RemoteViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.v_rmt_btn_base, parent, false))
        }

        override fun getItemCount() = AppState.tempData.tempRemoteProfile.buttons.size


        override fun onBindViewHolder(holder: RemoteViewHolder, position: Int) {
            holder.bind(AppState.tempData.tempRemoteProfile.buttons[position])

            //TODO: TEST CODE
            when (position) {
                1,3,5 -> {
                    remoteProperties?.getRemoteWidth()?.let {
                        val layoutParams = holder.itemView.layoutParams
                        layoutParams.width = it / 3
                        layoutParams.height = it / 2
                    }
                }
            }
        }

        class RemoteViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            private var button: Button? = null

            fun bind(button: Button) {
                this.button = button
                //itemView.findViewById<TextView>(R.id.btnText).text = this.button?.name
            }
        }

        interface RemoteLayoutProperties {
            fun getRemoteWidth() : Int
        }
    }
}