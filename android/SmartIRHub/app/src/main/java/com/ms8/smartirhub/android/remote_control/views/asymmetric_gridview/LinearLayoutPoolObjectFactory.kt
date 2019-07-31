package com.ms8.smartirhub.android.remote_control.views.asymmetric_gridview

import android.content.Context
import android.widget.LinearLayout

class LinearLayoutPoolObjectFactory(private val context: Context) :
    PoolObjectFactory<LinearLayout> {

    override fun createObject() = LinearLayout(context, null)
}