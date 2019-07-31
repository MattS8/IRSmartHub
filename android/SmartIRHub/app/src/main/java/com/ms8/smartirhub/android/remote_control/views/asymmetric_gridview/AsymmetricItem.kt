package com.ms8.smartirhub.android.remote_control.views.asymmetric_gridview

import android.os.Parcelable

interface AsymmetricItem : Parcelable {
    var columnSpan: Int
    var rowSpan: Int
}