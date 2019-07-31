package com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview_k

import android.os.Parcel
import android.os.Parcelable
import java.util.ArrayList

internal class RowInfo : Parcelable {
    private val items: MutableList<RowItem>
    val rowHeight: Int
    val spaceLeft: Float

    constructor(rowHeight: Int, items: MutableList<RowItem>, spaceLeft: Float) {
        this.rowHeight = rowHeight
        this.items = items
        this.spaceLeft = spaceLeft
    }

    constructor(`in`: Parcel) {
        rowHeight = `in`.readInt()
        spaceLeft = `in`.readFloat()
        val totalItems = `in`.readInt()

        items = ArrayList()
        val classLoader = AsymmetricItem::class.java.classLoader

        for (i in 0 until totalItems) {
            items.add(
                RowItem(
                    `in`.readInt(),
                    `in`.readParcelable<Parcelable>(classLoader) as AsymmetricItem
                )
            )
        }
    }

    fun getItems(): List<RowItem> {
        return items
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(rowHeight)
        dest.writeFloat(spaceLeft)
        dest.writeInt(items.size)

        for (rowItem in items) {
            dest.writeInt(rowItem.index)
            dest.writeParcelable(rowItem.item, 0)
        }
    }

    companion object CREATOR : Parcelable.Creator<RowInfo> {
        override fun createFromParcel(parcel: Parcel): RowInfo {
            return RowInfo(parcel)
        }

        override fun newArray(size: Int): Array<RowInfo?> {
            return arrayOfNulls(size)
        }
    }
}