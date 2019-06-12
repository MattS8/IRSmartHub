package com.ms8.smartirhub.android.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ms8.smartirhub.android.data.Group
import com.ms8.smartirhub.android.database.LocalData

class GroupViewModel(application: Application) : AndroidViewModel(application) {
    var groups : LiveData<HashMap<String, Group>> = MutableLiveData<HashMap<String, Group>>()

}