package com.ms8.smartirhub.android.remote_control.views.asymmetric_gridview

interface PoolObjectFactory<T> {
    fun createObject(): T
}