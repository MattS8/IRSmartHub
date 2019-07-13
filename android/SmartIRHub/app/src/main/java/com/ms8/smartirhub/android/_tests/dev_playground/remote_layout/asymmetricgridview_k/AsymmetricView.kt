package com.ms8.smartirhub.android._tests.dev_playground.remote_layout.asymmetricgridview_k

import android.view.View

interface AsymmetricView {
    val isDebugging: Boolean
    val numColumns: Int
    val isAllowReordering: Boolean
    val columnWidth: Int
    val divHeight: Int
    val requestedHorizontalSpacing: Int
    fun fireOnItemClick(index: Int, v: View)
    fun fireOnItemLongClick(index: Int, v: View): Boolean
}