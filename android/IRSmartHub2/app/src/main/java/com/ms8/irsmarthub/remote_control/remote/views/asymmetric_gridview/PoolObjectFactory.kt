package com.ms8.irsmarthub.remote_control.remote.views.asymmetric_gridview

interface PoolObjectFactory<T> {
    fun createObject(): T
}