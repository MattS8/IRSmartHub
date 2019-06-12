package com.ms8.smartirhub.android.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ms8.smartirhub.android.data.RemoteProfile

class RemoteProfileViewModel(application: Application) : AndroidViewModel(application) {
    private var remoteProfiles: MutableLiveData<HashMap<String, RemoteProfile>> = MutableLiveData()


    fun getProfiles() : LiveData<HashMap<String, RemoteProfile>> {
        if (remoteProfiles.value == null) {
            remoteProfiles.value = HashMap()
            FirebaseFirestore.getInstance().collection("remoteProfiles")
                .whereArrayContains("users", FirebaseAuth.getInstance().currentUser!!.uid)
                .addSnapshotListener{snapshot, e ->
                    when {
                        e != null -> { Log.e("listenToRemoteProfiles", "$e") }
                        else -> {
                            Log.d("listenToRemoteProfiles", "Received <${snapshot?.size()}> remoteProfiles from db")
                            for (doc in snapshot!!) {
                                val remoteProfile = doc.toObject(RemoteProfile::class.java)
                                remoteProfiles.value!!.remove(doc.id)
                                remoteProfiles.value!![doc.id] = remoteProfile
                            }
                            remoteProfiles.postValue(remoteProfiles.value)
                        }
                    }
                }
        }

        return remoteProfiles
    }

}