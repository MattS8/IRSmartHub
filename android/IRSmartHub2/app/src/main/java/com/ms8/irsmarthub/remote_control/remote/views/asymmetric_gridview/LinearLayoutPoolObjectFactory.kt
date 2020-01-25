package com.ms8.irsmarthub.remote_control.remote.views.asymmetric_gridview

import android.content.Context
import android.widget.LinearLayout

class LinearLayoutPoolObjectFactory(private val context: Context) :
    PoolObjectFactory<LinearLayout> {

    override fun createObject() = LinearLayout(context, null)
}