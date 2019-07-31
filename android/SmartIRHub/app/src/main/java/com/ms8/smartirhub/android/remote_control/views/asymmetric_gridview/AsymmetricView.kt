package com.ms8.smartirhub.android.remote_control.views.asymmetric_gridview

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