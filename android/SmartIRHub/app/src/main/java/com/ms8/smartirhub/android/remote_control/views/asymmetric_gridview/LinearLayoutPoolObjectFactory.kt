package com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview_k

import android.content.Context
import android.widget.LinearLayout

class LinearLayoutPoolObjectFactory(private val context: Context) : PoolObjectFactory<LinearLayout> {

    override fun createObject() = LinearLayout(context, null)
}