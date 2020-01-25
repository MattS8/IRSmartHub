package com.ms8.irsmarthub.database.models

import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import com.ms8.irsmarthub.user.models.User

class TempUser(
    var defaultHub: ObservableField<String> = ObservableField(""),
    var favRemote: ObservableField<String> = ObservableField(""),
    var uid: ObservableField<String> = ObservableField(""),
    var groups: ObservableArrayList<String> = ObservableArrayList(),
    var username: ObservableField<String> = ObservableField(""),
    var hubs: ObservableArrayList<String> = ObservableArrayList(),
    var irSignals: ObservableArrayList<String> = ObservableArrayList(),
    var remotes: ObservableArrayList<String> = ObservableArrayList()
) {

    fun copyFrom(user: User) {
        defaultHub.set(user.defaultHub)
        favRemote.set(user.favRemote)
        uid.set(user.uid)
        groups.apply {
            clear()
            addAll(user.groups)
        }
        username.set(user.username)
        hubs.apply {
            clear()
            addAll(user.hubs)
        }
        irSignals.apply {
            clear()
            addAll(user.irSignals)
        }
        remotes.apply {
            clear()
            addAll(user.remotes)
        }
    }
}