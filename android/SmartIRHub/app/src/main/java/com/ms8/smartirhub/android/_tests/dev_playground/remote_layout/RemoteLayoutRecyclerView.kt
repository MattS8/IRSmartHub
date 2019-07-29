//package com.ms8.smartirhub.android._tests.dev_playground.remote_layout
//
//import android.content.Context
//import android.os.Parcel
//import android.os.Parcelable
//import android.util.AttributeSet
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.ms8.smartirhub.android.R
//import com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview_k.*
//import com.ms8.smartirhub.android.database.TempData
//import com.ms8.smartirhub.android.models.firestore.RemoteProfile
//
//class RemoteLayoutRecyclerView(context: Context, attrs: AttributeSet) : AsymmetricRecyclerView(context, attrs) {
//
//    //TODO: Test Code
//    fun setupAdapter() {
//        val remoteAdapter = RemoteLayoutAdapter()
//            .apply {
//                TempData.tempRemoteProfile.buttons
//                    .apply {
//                        for (i in 0 until 50) {
//                            val b = RemoteProfile.Button()
//                                .apply {
//                                    name = "Button $i"
//                                }
//                            add(b)
//                            when (i) {
//                                0,1,2,3 -> {
//                                    items.add(AsymmetricButtonItem(1, 1, i)
//                                        .apply {
//                                            button = b
//                                        })
//                                }
//                                4,8 -> {
//                                    items.add(AsymmetricButtonItem(1, 2, i)
//                                        .apply {
//                                            button = b
//                                        })
//                                }
//                                11 -> {
//                                    items.add(AsymmetricButtonItem(2, 2, i)
//                                        .apply {
//                                            button = b
//                                        })
//                                }
//                                else -> {
//                                    val colspan = if (i % 2 == 0) 1 else 2
//                                    val rowSpan = if (i % 10 == 0) 1 else 2
//                                    items.add(AsymmetricButtonItem(1, 1, i)
//                                        .apply {
//                                            button = b
//                                        })
//                                }
//                            }
//                        }
//                    }
//            }
//
//        setRequestedColumnCount(3)
//        isDebugging = true
//        addItemDecoration(SpacesItemDecoration(Utils.dpToPx(context, 3f)))
//        requestedHorizontalSpacing = Utils.dpToPx(context, 3f)
//        adapter = AsymmetricRecyclerViewAdapter(context, this, remoteAdapter)
//        adapter?.notifyDataSetChanged()
//    }
//
//
//    class AsymmetricButtonItem(var _columnSpan: Int, var _rowSpan: Int, var _position: Int): AsymmetricItem {
//        override var columnSpan: Int
//            get() = _columnSpan
//            set(value) { _columnSpan = columnSpan }
//        override var rowSpan: Int
//            get() = _rowSpan
//            set(value) { _rowSpan = rowSpan }
//        var button: RemoteProfile.Button? = null
//
//        constructor(parcel: Parcel) : this(
//            parcel.readInt(),
//            parcel.readInt(),
//            parcel.readInt()
//        )
//
//        override fun describeContents() = 0
//
//        override fun writeToParcel(dest: Parcel?, flags: Int) {
//            dest?.apply {
//                writeInt(_columnSpan)
//                writeInt(_rowSpan)
//                writeInt(_position)
//            }
//        }
//
//        override fun toString(): String {
//            return String.format("%s: %sx%s", _position, _rowSpan, _columnSpan)
//        }
//
//        companion object CREATOR : Parcelable.Creator<AsymmetricButtonItem> {
//            override fun createFromParcel(parcel: Parcel): AsymmetricButtonItem {
//                return AsymmetricButtonItem(parcel)
//            }
//
//            override fun newArray(size: Int): Array<AsymmetricButtonItem?> {
//                return arrayOfNulls(size)
//            }
//        }
//    }
//
//    class RemoteLayoutAdapter: AGVRecyclerViewAdapter<RemoteViewHolder>() {
//        val items = ArrayList<AsymmetricButtonItem>()
//
//        override fun getItem(position: Int) = items[position]
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = RemoteViewHolder(parent, viewType)
//
//        override fun getItemCount() = items.size
//
//        override fun onBindViewHolder(holder: RemoteViewHolder, position: Int) {
//            Log.d("TEST", "binding $position")
//            holder.bind(items[position].button)
//            if (position == 4)
//                holder.itemView.visibility = View.INVISIBLE
//            else
//                holder.itemView.visibility = View.VISIBLE
//        }
//
//    }
//
//    class RemoteViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(
//        R.layout.v_rmt_btn_base,
//        parent,
//        false)) {
//        var btnTitle: TextView = itemView.findViewById(R.id.btnText)
//
//        fun bind(button: RemoteProfile.Button?) {
//            btnTitle.text = button?.name
//        }
//    }
//}