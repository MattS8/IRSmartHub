package com.ms8.irsmarthub.remote_control.remote.views.asymmetric_gridview

import android.os.Parcelable

interface AsymmetricItem : Parcelable {
    var columnSpan: Int
    var rowSpan: Int
}