package com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview_k

import android.os.Parcelable

interface AsymmetricItem : Parcelable {
    var columnSpan: Int
    var rowSpan: Int
}